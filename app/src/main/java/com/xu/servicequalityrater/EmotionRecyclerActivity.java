package com.xu.servicequalityrater;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xu.servicequalityrater.data.EmotionListContract.EmotionListEntry;
import com.xu.servicequalityrater.data.EmotionListDbHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;


//TODO 14 07 Added NHS number handling
//TODO 01.05.2017 Code finally works
//TODO add facebook Login, add save target email, add remote trigger, add front face camera for Veritas
//TODO 0705 remote triggers underway, face camera works. Need to add timestamp into SQlite
//TODO 0905 DATETIME timestamp added, for retrieval methods consult bottom
public class EmotionRecyclerActivity extends AppCompatActivity {
    public ArrayList<Double> anger;
    private TextView averageAnger;
    private TextView averageContempt;
    private TextView averageDisgust;
    private TextView averageHappiness;
    private TextView averageSadness;
    public ArrayList<Double> averages;
    public Double avganger;
    public Double avgcontempt;
    public Double avgdisgust;
    public Double avgfear;
    public Double avghappiness;
    public Double avgneutral;
    public Double avgsadness;
    public Double avgsurprise;
    public ArrayList<Double> contempt;
    public SQLiteDatabase db;
    public ArrayList<Double> disgust;
    public ArrayList<String> emotionNames;
    public RecyclerView emotionRecyclerView;
    public ArrayList<Double> fear;
    public ArrayList<Double> happiness;
    public EmotionListDbHelper helper;
    public EmotionAdapter mAdapter;
    public ArrayList<Double> neutral;
    public ArrayList<Double> sadness;
    public ArrayList<Double> surprise;

    public Button toCSV;
    public Button toUpload;
    public Button toSend;

    private static final String TAG = "debug";
    SharedPreferences profileSave;
    String profilename;
    String profilenumber;

    //28082017 Code for calculating averages
    HashMap<String,Double>dailyaverages;
    public ArrayList<String>dateStamps;
    public ArrayList<Date>dateReals;
    boolean allEntriesInSameDay;



    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        profileSave=getSharedPreferences("profileSave",Context.MODE_PRIVATE);
        profilename = profileSave.getString("profilename","");
        profilenumber=profileSave.getString("profilenumber","000");



        //TODO buttons work

        this.toCSV = (Button) findViewById(R.id.publishDisk);
        toCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //28082017 Both datestamps and dateReals work fine
                    //exportDB();
                if (allEntriesInSameDay==false){
                    dailyaverages = returnDailyAverages(dateReals,happiness);
                    Toast.makeText(EmotionRecyclerActivity.this, String.valueOf(dailyaverages.size()),Toast.LENGTH_SHORT).show();
                    /*
                    for (String key:dailyaverages.keySet()){
                        Toast.makeText(EmotionRecyclerActivity.this, key,Toast.LENGTH_SHORT).show();
                    }
                    */
                }else if(allEntriesInSameDay==true){
                    dailyaverages= returnSingleDayAverage(dateReals,happiness);
                    /*
                    for (Double value:dailyaverages.values()){
                        Toast.makeText(EmotionRecyclerActivity.this, String.valueOf(value),Toast.LENGTH_SHORT).show();
                    }
                    */
                }



            }
        });



        toUpload = (Button) findViewById(R.id.publishDb);
        toUpload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent launchGraph = new Intent(EmotionRecyclerActivity.this, GraphActivity.class);
                startActivity(launchGraph);


                /*TODO removed temporarily and replaced with graphactivity
                //TODO delete from DB? http://docs.aws.amazon.com/mobile/sdkforandroid/developerguide/dynamodb_om.html

                final Cursor cursor = getAllEmotions();
                cursor.moveToFirst();
                Runnable runnable = new Runnable() {
                    public void run() {

                        while (cursor.moveToNext()) {
                            try {
                                int id = cursor.getInt(cursor.getColumnIndex("_id"));
                                double anger = cursor.getDouble(cursor.getColumnIndex(EmotionListEntry.COLUMN_ANGER));
                                double contempt = cursor.getDouble(cursor.getColumnIndex(EmotionListEntry.COLUMN_CONTEMPT));
                                double disgust = cursor.getDouble(cursor.getColumnIndex(EmotionListEntry.COLUMN_DISGUST));
                                double happiness = cursor.getDouble(cursor.getColumnIndex(EmotionListEntry.COLUMN_HAPPINESS));
                                double sadness = cursor.getDouble(cursor.getColumnIndex(EmotionListEntry.COLUMN_SADNESS));




                            } finally {
                                cursor.close();

                            }

                        }
                    }
                };
                Thread mythread = new Thread(runnable);
                mythread.start();
                */





            }
        });

        toSend = (Button)findViewById(R.id.publishMD);
        toSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                exportAndSend();
            }
        });


        this.emotionRecyclerView = (RecyclerView) findViewById(R.id.emotion_recycler_view);
        this.emotionRecyclerView.addItemDecoration(new DividerItemDecoration(this, 0));
        this.emotionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.emotionRecyclerView.setItemAnimator(new DefaultItemAnimator());
        this.helper = new EmotionListDbHelper(this);
        this.db = this.helper.getWritableDatabase();
        Cursor cursor = getAllEmotions();
        //TODO get the cursor for the sqlite
        this.averageAnger = (TextView) findViewById(R.id.averageanger);
        this.averageContempt = (TextView) findViewById(R.id.averagecontempt);
        this.averageDisgust = (TextView) findViewById(R.id.averagedisgust);
        this.averageHappiness = (TextView) findViewById(R.id.averagehappiness);
        this.averageSadness = (TextView) findViewById(R.id.averagesadness);
        this.anger = new ArrayList();
        this.contempt = new ArrayList();
        this.disgust = new ArrayList();
        this.fear = new ArrayList();
        this.happiness = new ArrayList();
        this.neutral = new ArrayList();
        this.sadness = new ArrayList();
        this.surprise = new ArrayList();
        this.averages = new ArrayList();
        this.emotionNames = new ArrayList();
        this.anger = getEmotionColumn(EmotionListEntry.COLUMN_ANGER);
        this.contempt = getEmotionColumn(EmotionListEntry.COLUMN_CONTEMPT);
        this.disgust = getEmotionColumn(EmotionListEntry.COLUMN_DISGUST);
        this.fear = getEmotionColumn(EmotionListEntry.COLUMN_FEAR);
        this.happiness = getEmotionColumn(EmotionListEntry.COLUMN_HAPPINESS);
        this.neutral = getEmotionColumn(EmotionListEntry.COLUMN_NEUTRAL);
        this.sadness = getEmotionColumn(EmotionListEntry.COLUMN_SADNESS);
        this.surprise = getEmotionColumn(EmotionListEntry.COLUMN_SURPRISE);
        this.avganger = calculateAverage(this.anger);
        this.avgcontempt = calculateAverage(this.contempt);
        this.avgdisgust = calculateAverage(this.disgust);
        this.avgfear = calculateAverage(this.fear);
        this.avghappiness = calculateAverage(this.happiness);
        this.avgneutral = calculateAverage(this.neutral);
        this.avgsadness = calculateAverage(this.sadness);
        this.avgsurprise = calculateAverage(this.surprise);
        this.averageAnger.setText(roundAndFormat(this.avganger));
        this.averageContempt.setText(roundAndFormat(this.avgcontempt));
        this.averageDisgust.setText(roundAndFormat(this.avgdisgust));
        this.averageHappiness.setText(roundAndFormat(this.avghappiness));
        this.averageSadness.setText(roundAndFormat(this.avgsadness));
        this.dateStamps =getUnixColumn();
        this.dateReals = convertStringToDates(dateStamps);
        this.allEntriesInSameDay=checkAllDaysInArray(dateReals);
        if (this.mAdapter == null) {
            this.mAdapter = new EmotionAdapter(this, cursor);
            this.emotionRecyclerView.setAdapter(this.mAdapter);
            return;
        }
        this.mAdapter.notifyDataSetChanged();




    }


    private void exportAndSend() {
        //http://stackoverflow.com/questions/5401104/android-exporting-to-csv-and-sending-as-email-attachment/5403357#5403357

        File dbFile = getDatabasePath("emotionSQLTable.db");
        EmotionListDbHelper dbhelper = new EmotionListDbHelper(getApplicationContext());
        File exportDir = new File(Environment.getExternalStorageDirectory(), "/AletheiaCSV");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String fileName = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File file = new File(exportDir, "csvname-" + fileName + ".csv");
        //Fetch path http://stackoverflow.com/questions/9974987/how-to-send-an-email-with-a-file-attachment-in-android
        Uri path = Uri.fromFile(file);
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = dbhelper.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM "+EmotionListDbHelper.DATABASE_TABLE_NAME, null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to exprort
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3),curCSV.getString(4),curCSV.getString(5),curCSV.getString(6),curCSV.getString(7),curCSV.getString(8),curCSV.getString(9)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
        } catch (Exception sqlEx) {
            Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
        } finally {
            //Send an email and attach the file you just made
            Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Health Update from Patient: "+profilename+" "+"NHS No: " +profilenumber+ " "+ date);
            intent.putExtra(Intent.EXTRA_TEXT, "Attached to this message in CSV form");
            intent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(intent,"Sending email"));
        }
    }







    public void fillArrayLists() {
        this.averages.add(this.avganger);
        this.averages.add(this.avgcontempt);
        this.averages.add(this.avgdisgust);
        this.averages.add(this.avgfear);
        this.averages.add(this.avghappiness);
        this.averages.add(this.avgneutral);
        this.averages.add(this.avgsadness);
        this.averages.add(this.avgsurprise);
        this.emotionNames.add("Anger");
        this.emotionNames.add("Contempt");
        this.emotionNames.add("Disgust");
        this.emotionNames.add("Fear");
        this.emotionNames.add("Happiness");
        this.emotionNames.add("Neutral");
        this.emotionNames.add("Sadness");
        this.emotionNames.add("Surprise");
    }

    public Double calculateAverage(ArrayList<Double> vals) {
        if (vals.size() == 0) {
            return Double.valueOf(0.0d);
        }
        Double sum = Double.valueOf(0.0d);
        Double average = Double.valueOf(0.0d);
        for (int i = 0; i < vals.size(); i++) {
            sum = Double.valueOf(sum.doubleValue() + ((Double) vals.get(i)).doubleValue());
        }
        return Double.valueOf(sum.doubleValue() / ((double) vals.size()));
    }

    private String roundAndFormat(Double d) {
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(d);
    }

    private Cursor getAllEntries() {
        return this.db.query(EmotionListEntry.TABLE_NAME, null, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
    }

    private ArrayList<Double> getEmotionColumn(String name) {
        Cursor cursortemp;
        ArrayList<Double> results = new ArrayList();
        if (name.equals(EmotionListEntry.COLUMN_ANGER)) {
            cursortemp = this.db.query(EmotionListEntry.TABLE_NAME, new String[]{EmotionListEntry.COLUMN_ANGER, EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListEntry.COLUMN_ANGER))));
            }
        }
        if (name.equals(EmotionListEntry.COLUMN_CONTEMPT)) {
            cursortemp = this.db.query(EmotionListEntry.TABLE_NAME, new String[]{EmotionListEntry.COLUMN_CONTEMPT, EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListEntry.COLUMN_CONTEMPT))));
            }
        }
        if (name.equals(EmotionListEntry.COLUMN_DISGUST)) {
            cursortemp = this.db.query(EmotionListEntry.TABLE_NAME, new String[]{EmotionListEntry.COLUMN_DISGUST, EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListEntry.COLUMN_DISGUST))));
            }
        }
        if (name.equals(EmotionListEntry.COLUMN_FEAR)) {
            cursortemp = this.db.query(EmotionListEntry.TABLE_NAME, new String[]{EmotionListEntry.COLUMN_FEAR, EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListEntry.COLUMN_FEAR))));
            }
        }
        if (name.equals(EmotionListEntry.COLUMN_HAPPINESS)) {
            cursortemp = this.db.query(EmotionListEntry.TABLE_NAME, new String[]{EmotionListEntry.COLUMN_HAPPINESS, EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListEntry.COLUMN_HAPPINESS))));
            }
        }
        if (name.equals(EmotionListEntry.COLUMN_NEUTRAL)) {
            cursortemp = this.db.query(EmotionListEntry.TABLE_NAME, new String[]{EmotionListEntry.COLUMN_NEUTRAL, EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListEntry.COLUMN_NEUTRAL))));
            }
        }
        if (name.equals(EmotionListEntry.COLUMN_SADNESS)) {
            cursortemp = this.db.query(EmotionListEntry.TABLE_NAME, new String[]{EmotionListEntry.COLUMN_SADNESS, EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListEntry.COLUMN_SADNESS))));
            }
        }
        if (name.equals(EmotionListEntry.COLUMN_SURPRISE)) {
            cursortemp = this.db.query(EmotionListEntry.TABLE_NAME, new String[]{EmotionListEntry.COLUMN_SURPRISE, EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListEntry.COLUMN_SURPRISE))));
            }
        }

        return results;
    }

    //TODO 27082017 method to return the ArrayList of Timestamp , since the results are not double
    private ArrayList<String>getUnixColumn(){
        Cursor cursortemp;
        ArrayList <String> results = new ArrayList<>();
        cursortemp = this.db.query(EmotionListEntry.TABLE_NAME,new String[]{EmotionListEntry.COLUMN_TIMESTAMP},null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP );
        while (cursortemp.moveToNext()){
            results.add(cursortemp.getString(cursortemp.getColumnIndex(EmotionListEntry.COLUMN_TIMESTAMP)));
        }

        return results;
    };
    //TODO 27082017 method from GraphActivity, used to create dates from the timestamp
    public ArrayList<Date>convertStringToDates(ArrayList<String>strings){

        ArrayList<Date>dates = new ArrayList();

        for (int i=0;i<strings.size();i++){
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try{
                Date date = formatter.parse(strings.get(i));
                dates.add(date);

            }catch (ParseException e){
                e.printStackTrace();
            }

        }

        return dates;
    }

    //TODO 27082017 Check if dates have passed 1 day. D1 & D2 need to be sequential however
    public boolean compareDates (Date d1, Date d2){
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        boolean sameday =cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);

        return sameday;
    }

    public String formatDateIntoDay (Date d1){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String resultDay  = formatter.format(d1);

        return resultDay;
    }

    //TODO 27082017 Combination method to calculate the DAILY average
    //TODO no printing method included here. Read documentation.
    public HashMap<String,Double>returnDailyAverages(ArrayList<Date>timestamps, ArrayList<Double>EmotionValues){
        int tracker=0;
        ArrayList<Double>tempArray= new ArrayList();
        HashMap<String,Double>DailyAverages =new HashMap<String,Double>();
        double average;
        String day;
        for (int i = 1;i<timestamps.size();i++){
            //TODO compare everyday pairs sequentially to see if we move on next day
            boolean sameday  = compareDates(timestamps.get(i),timestamps.get(i-1));
            Toast.makeText(EmotionRecyclerActivity.this, String.valueOf(sameday),Toast.LENGTH_SHORT).show();

            /*TODO Bingo! Time to take all the entries until this one in consideration
            if(sameday!=true){
                //TODO time to go to the last sameday and make an array of them
               ;
                for (int j = i-1;j==tracker;j--){
                    tempArray.add(EmotionValues.get(j));
                }
                //TODO calculate the average from tempArray
                average = getSum(tempArray)/tempArray.size();
                //Get the correct Date
                day = formatDateIntoDay(timestamps.get(i-1));
                DailyAverages.put(day,average);
                //TOAST METHOD FOR DEBUG


                //TODO Null array, and set tracker to final previous day entry
                tracker = i-1;
                tempArray.clear();


            }*/

        }

        return DailyAverages;

    }
    //TODO 28082017 If only one day value available
    public HashMap<String,Double> returnSingleDayAverage(ArrayList<Date>timestamps, ArrayList<Double>EmotionValues){

        HashMap<String,Double>DailyAverages =new HashMap<String,Double>();
        double average;
        //if all same day anyway, doesnt matter which one we pull out
        String day =formatDateIntoDay(timestamps.get(0));
        average = getSum(EmotionValues)/EmotionValues.size();
        DailyAverages.put(day,average);

        return DailyAverages;

    }

    private double getSum(ArrayList<Double>values){

        double sum = 0;
        for(int i = 0; i < values.size(); i++){
            sum += values.get(i);
        }
        return sum;
    }

    //TODO 28082017 we need to check if all the result are in the same day
    public boolean checkAllDaysInArray(ArrayList<Date>timestamps){
        boolean result = true;
        ArrayList<Boolean>allBooleans = new ArrayList<>();

        for (int i = 1;i<timestamps.size();i++){
            boolean sameday = compareDates(timestamps.get(i),timestamps.get(i-1));
            allBooleans.add(sameday);
        }
        //now that we are done , lets check if all values are of sameday
        for (int i =0;i<allBooleans.size();i++){
            if(allBooleans.get(i)==false){
                result=false;
            }
        }
        return result;
    }


    private long addEmotion(double anger, double contempt, double disgust, double fear, double happiness, double neutral, double sadness, double surprise) {
        ContentValues cv = new ContentValues();
        cv.put(EmotionListEntry.COLUMN_ANGER, Double.valueOf(anger));
        cv.put(EmotionListEntry.COLUMN_CONTEMPT, Double.valueOf(contempt));
        cv.put(EmotionListEntry.COLUMN_DISGUST, Double.valueOf(disgust));
        cv.put(EmotionListEntry.COLUMN_FEAR, Double.valueOf(fear));
        cv.put(EmotionListEntry.COLUMN_HAPPINESS, Double.valueOf(happiness));
        cv.put(EmotionListEntry.COLUMN_NEUTRAL, Double.valueOf(neutral));
        cv.put(EmotionListEntry.COLUMN_SADNESS, Double.valueOf(sadness));
        cv.put(EmotionListEntry.COLUMN_SURPRISE, Double.valueOf(surprise));
        return this.db.insert(EmotionListEntry.TABLE_NAME, null, cv);
    }
    //Get the count of rows
    public int getProfilesCount() {
        String countQuery = "SELECT  * FROM " + "emotionlist.db";

        Cursor cursor = db.rawQuery(countQuery, null);
        int cnt = cursor.getCount();
        cursor.close();
        return cnt;
    }

    private Cursor getAllEmotions() {
        return this.db.query(EmotionListEntry.TABLE_NAME, null, null, null, null, null, EmotionListEntry.COLUMN_TIMESTAMP);
    }

    private boolean exportDB() {

        File dbFile = this.getDatabasePath("emotionSQLTable.db");
        Log.v(TAG, "Db path is: " + dbFile); // get the path of db
        EmotionListDbHelper dbhelper = new EmotionListDbHelper(this);
        //switching out to internal directory here, for debu purposes and because we dont have sd card
        /*final String appPath = String.format
                (
                        "%s/Aletheia", Environment.getExternalStorageDirectory()
                );
                */

        File exportDir = new File(Environment.getExternalStorageDirectory(),"Aletheia");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }


        String fileName = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        //TODO changed rawquery to match database_Tablename in helper
        File file = new File(exportDir, "emotiondata-" +fileName+ ".csv");
            try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = dbhelper.getReadableDatabase();
                //Here we select from the TABLE NAME, which is emotionlist
            Cursor curCSV = db.rawQuery("SELECT * FROM "+EmotionListDbHelper.DATABASE_TABLE_NAME, null);
            csvWrite.writeNext(curCSV.getColumnNames());
                if(curCSV.getCount()>0) {
                    while (curCSV.moveToNext()) {
                        //Select which columns you would like to export
                        String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3),curCSV.getString(4),curCSV.getString(5),curCSV.getString(6),curCSV.getString(7),curCSV.getString(8),curCSV.getString(9)};
                        csvWrite.writeNext(arrStr);
                    }
                }else{
                    return false;
                }

            csvWrite.close();
            curCSV.close();
                return true;
        } catch (SQLException sqlEx) {
            Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
                return false;
        } catch (IOException sqlEx) {
                Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
                return false;
            }


    }

    //DEBUG code to write a simple text file
    private void writeToSDFile(){

        // Find the root of the external storage.
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

        File root = Environment.getExternalStorageDirectory();



        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        //File dir = new File (root.getAbsolutePath() + "/download");
        File dir = new File (root.getAbsolutePath()+"/Aletheia");


        dir.mkdirs();
        File file = new File(dir, "myData.txt");

        Log.d(TAG,"path is"+dir);

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.println("Hi , How are you");
            pw.println("Hello");
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("EmotionServiceActivity", "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private class ExportDatabaseCSVTask extends
            AsyncTask<String, Void, Boolean> {
        private final ProgressDialog dialog = new ProgressDialog(
                EmotionRecyclerActivity.this);

        @Override
        protected void onPreExecute() {

            this.dialog.setMessage("Exporting database...");
            this.dialog.show();

        }

        protected Boolean doInBackground(final String... args) {

            File dbFile = getDatabasePath("emotionSQLTable.db");
            EmotionListDbHelper dbhelper = new  EmotionListDbHelper(
                    getApplicationContext());
            Log.v("EmotionRecyclerActivity", "Db path is: " + dbFile); // get the path of db

            File exportDir = new File(
                    Environment.getExternalStorageDirectory(), "");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File file = new File(exportDir, "emotionSQLTable.csv");
            try {

                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                SQLiteDatabase db = dbhelper.getReadableDatabase(); // Cursor
                Cursor curCSV = db.rawQuery("SELECT * FROM emotionlist",
                        null);
                csvWrite.writeNext(curCSV.getColumnNames());
                while (curCSV.moveToNext()) {
                    String arrStr[] = { curCSV.getString(0),
                            curCSV.getString(1), curCSV.getString(2),
                            };
                    csvWrite.writeNext(arrStr);
                }
                csvWrite.close();
                curCSV.close();
                return true;
            } catch (Exception sqlEx) {
                Log.e("EmotionRecyclerActivity", sqlEx.getMessage(), sqlEx);
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
            if (success) {
                Toast.makeText(EmotionRecyclerActivity.this, "Export successful!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(EmotionRecyclerActivity.this, "Export failed!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class EmotionAdapter extends Adapter<EmotionHolder> {
        private Context mContext;
        private Cursor mCursor;

        public EmotionAdapter(Context context, Cursor cursor) {
            this.mContext = context;
            this.mCursor = cursor;
        }

        public EmotionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new EmotionHolder(LayoutInflater.from(this.mContext).inflate(R.layout.tableprototype, parent, false));
        }

        public void onBindViewHolder(EmotionHolder holder, int position) {
            if (this.mCursor.moveToPosition(position)) {
                int id = this.mCursor.getInt(this.mCursor.getColumnIndex("_id"));
                double anger = this.mCursor.getDouble(this.mCursor.getColumnIndex(EmotionListEntry.COLUMN_ANGER));
                double contempt = this.mCursor.getDouble(this.mCursor.getColumnIndex(EmotionListEntry.COLUMN_CONTEMPT));
                double disgust = this.mCursor.getDouble(this.mCursor.getColumnIndex(EmotionListEntry.COLUMN_DISGUST));
                double happiness = this.mCursor.getDouble(this.mCursor.getColumnIndex(EmotionListEntry.COLUMN_HAPPINESS));
                double sadness = this.mCursor.getDouble(this.mCursor.getColumnIndex(EmotionListEntry.COLUMN_SADNESS));
                holder.entryView.setText(String.valueOf(id));
                holder.angerView.setText(EmotionRecyclerActivity.this.roundAndFormat(Double.valueOf(anger)));
                holder.contemptView.setText(EmotionRecyclerActivity.this.roundAndFormat(Double.valueOf(contempt)));
                holder.disgustView.setText(EmotionRecyclerActivity.this.roundAndFormat(Double.valueOf(disgust)));
                holder.happinessView.setText(EmotionRecyclerActivity.this.roundAndFormat(Double.valueOf(happiness)));
                holder.sadnessView.setText(EmotionRecyclerActivity.this.roundAndFormat(Double.valueOf(sadness)));
            }
        }

        public int getItemCount() {
            return this.mCursor.getCount();
        }
    }

    private class EmotionHolder extends ViewHolder implements OnClickListener {
        TextView angerView;
        TextView contemptView;
        TextView disgustView;
        TextView entryView;
        TextView happinessView;
        TextView sadnessView;

        public EmotionHolder(View itemView) {
            super(itemView);
            this.entryView = (TextView) itemView.findViewById(R.id.entry);
            this.angerView = (TextView) itemView.findViewById(R.id.anger);
            this.contemptView = (TextView) itemView.findViewById(R.id.contempt);
            this.disgustView = (TextView) itemView.findViewById(R.id.disgust);
            this.happinessView = (TextView) itemView.findViewById(R.id.happiness);
            this.sadnessView = (TextView) itemView.findViewById(R.id.sadness);
            itemView.setOnClickListener(this);
        }

        public void onClick(View v) {
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        Intent goback = new Intent(this, CameraActivity.class);
        startActivity(goback);
        finish();
        //System.exit(0);
    }

    /* From http://stackoverflow.com/questions/8999947/how-to-retrieve-datetime-format-from-sqlite-database
Note DateTime is automade by SQLite, when retrieved its in string format
Method 1
Calendar t = new GregorianCalendar();
SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY",Locale.getDefault());
java.util.Date dt = sdf.parse(c.getString(4)); //replace 4 with the column index, c is the cursor
t.setTime(dt);

Method 2
http://stackoverflow.com/questions/38967087/how-to-format-datetime-from-sqlite-in-android-and-show-on-text-view

 String strCurrentDate= "2016-07-13 13:10:00";
        SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
        Date newDate = null;
        try {
            newDate = format.parse(strCurrentDate);
            format = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
            String date = format.format(newDate);
            System.out.println(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
     */
}

