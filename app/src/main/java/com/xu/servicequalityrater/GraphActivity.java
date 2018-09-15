package com.xu.servicequalityrater;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.xu.servicequalityrater.data.EmotionListContract;
import com.xu.servicequalityrater.data.EmotionListDbHelper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

//2405 All labels check out, instead we will create an integer based index system
//2605 todo added a integer-based graphing system (TestInts)
//TODO check that healthReceiver system works with same framework, seems to, fix bugs too
//TODO remove focus from edittext, check that it saves
//TODO also make scrolling work properly
public class GraphActivity extends AppCompatActivity implements OnChartGestureListener{


    //Datemap works with XAxisFormatter to produce good labels that make sense
    HashMap<Integer, String> datemap;

    int Day;
    float Close;
    float value;
    int month;
    int year;
    String stockname;
    String stockticker;
    LineChart chart;

    //Smaller chart to display histograms, macds etc. should eventualy be nestscrollviewed
    LineChart auxchart;

    Date datefrom;
    Date dateto;

    TextView menulabel;







    SharedPreferences sp;
    SharedPreferences.Editor editor;
    SharedPreferences settingspreferences;


    SharedPreferences dateStorage;
    SharedPreferences.Editor dateeditor;
    int REQUEST_SETTINGS = 0;
    public HashMap<String, Boolean> settings;


    //All indicators TODO may have to change all to floats
    ArrayList<Double>Sadness;
    ArrayList<Double>Happiness;
    ArrayList<Double>Fear;
    ArrayList<Double>Disgust;
    ArrayList<Double>Contempt;
    ArrayList<Double>Anger;
    ArrayList<Double>Neutral;
    ArrayList<String>UnixStamps;
    ArrayList<Date>DateStamps;
    ArrayList<Long>LongStamps;
    ArrayList<Float>testStamps;
    //Reversed ArrayLists
    ArrayList<Double>reversedSadness;
    ArrayList<Double>reversedHappiness;
    ArrayList<Double>reversedFear;
    ArrayList<Double>reversedDisgust;
    ArrayList<Double>reversedContempt;
    ArrayList<Double>reversedAnger;
    ArrayList<Double>reversedNeutral;
    ArrayList<String>reversedUnixStamps;
    //2505 TODO for integer graphing, creating Integer ArrayList
    ArrayList<Integer>testInts;

    LineDataSet SadnessDataset;
    LineDataSet HappinessDataset;
    LineDataSet FearDataset;
    LineDataSet DisgustDataset;
    LineDataSet ContemptDataset;
    LineDataSet AngerDataset;
    LineDataSet NeutralDataset;

    LineData EmotionData;

    ArrayList<String> indicatorSettings;
    private static final String TAG = "DetailActivity";



    //Calendar for loading initial data of 14 days
    Calendar current;
    Calendar past;

    YAxis auxyaxisleft;
    YAxis auxyaxisright;
    ProgressDialog pDialog;
    String startDate;
    String endDate;
    boolean isOnline;
    SharedPreferences defaultDates;
    //TODO we need to put userModified into db as well
    boolean userModified;
    String userDefaultInterval;
    public SQLiteDatabase db;
    public EmotionListDbHelper helper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);


        datemap = new HashMap<Integer, String>();
        testInts = new ArrayList<Integer>();
        settings = new HashMap<String, Boolean>();
        menulabel =(TextView) findViewById(R.id.menulabel);

        //Load name and ticker for selected stock from List?

        //Calling FetchXDayDatprsicker);
        //TODO loadStockData should create a dummy 2 week timeframe

        isOnline = checkInternetAccess();

        //Load SavedPrefs for User interval dates, we do this because it happens on NEXT startup
        defaultDates = this.getSharedPreferences("defaultDates", Context.MODE_PRIVATE);
        userModified = defaultDates.getBoolean("userModified",false);
        userDefaultInterval = defaultDates.getString("defaultInterval", "NULL");
        //default date objects for blocking use of technical indicators

        current = Calendar.getInstance();
        past = Calendar.getInstance();
        testStamps = new ArrayList<>();
        testStamps.add(0F);
        testStamps.add(1F);








        settingspreferences = getSharedPreferences("settingsprefs", Context.MODE_PRIVATE);
        editor = settingspreferences.edit();

        dateStorage = getSharedPreferences("defaultDates", Context.MODE_PRIVATE);
        dateeditor = dateStorage.edit();


        //initialize chart
        chart = (LineChart) findViewById(R.id.chart);

        //allow for touch
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        //allow for highlighting when touching values on chart
        chart.setHighlightPerDragEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setOnChartGestureListener(this);
        chart.setNoDataText("Loading...");
        chart.setAutoScaleMinMaxEnabled(true);
        Description empty = new Description();
        empty.setText("");
        chart.setDescription(empty);


        chart.animateX(2000);
        this.helper = new EmotionListDbHelper(this);
        this.db = this.helper.getWritableDatabase();


        //start fetching the data values
        Sadness= getEmotionColumn("sadness");
        Neutral = getEmotionColumn("neutral");
        Happiness = getEmotionColumn("happiness");
        Fear = getEmotionColumn("fear");
        Disgust=getEmotionColumn("disgust");
        Contempt=getEmotionColumn("contempt");
        Anger =getEmotionColumn("anger");
        UnixStamps=getUnixstampColumn();

        //TODO 2810 data was the wrong way round, disabling data reverse
        /*
        reversedSadness  =reverse(Sadness);
        reversedNeutral = reverse(Neutral);
        reversedHappiness= reverse(Happiness);
        reversedFear = reverse(Fear);
        reversedDisgust=reverse(Disgust);
        reversedContempt= reverse(Contempt);
        reversedAnger = reverse(Anger);
        */
        reversedUnixStamps= reverseStr(UnixStamps);
        //2810 see if this adhoc works
        DateStamps = convertStringToDates(UnixStamps);
        testInts =createTestInts();
        datemap= createDateMap();
        String fromdate = parseDateIntoString(DateStamps.get(0));
        String todate  = parseDateIntoString(DateStamps.get(DateStamps.size()-1));
        menulabel.setText("Emotion data from "+fromdate+" to "+todate);







        //2605 we will  remove Longstamps from use, as we now use datemap
        //LongStamps = convertDatesToLongs(DateStamps);

        /*TODO according to github link: axisformatter will convert final date strings, done at end
        //DEBUG code for printing out dates below

        for(int i=0;i<UnixStamps.size();i++){
            Toast.makeText(this,LongStamps.get(i).toString(),Toast.LENGTH_LONG).show();
        }
        */


        //Next lets turn these things into entries, and then LineDataSets7
        SadnessDataset= makeDataIntoGraph(Sadness, "Sadness");
        SadnessDataset.setColor(Color.BLUE);
        SadnessDataset.setCircleColor(Color.BLUE);
        HappinessDataset = makeDataIntoGraph(Happiness,"Happiness");
        /*TODO 23-05 Debug code
        for (int i =0 ;i<Happiness.size();i++){
            Toast.makeText(this,Happiness.get(i).toString(),Toast.LENGTH_LONG).show();
        }
        */
        HappinessDataset.setColor(Color.GREEN);
        HappinessDataset.setCircleColor(Color.GREEN);
        NeutralDataset = makeDataIntoGraph(Neutral,"Neutral");
        NeutralDataset.setColor(Color.BLACK);
        NeutralDataset.setCircleColor(Color.BLACK);
        FearDataset= makeDataIntoGraph(Fear, "Fear");
        FearDataset.setColor(Color.MAGENTA);
        FearDataset.setCircleColor(Color.MAGENTA);
        DisgustDataset= makeDataIntoGraph(Disgust, "Disgust");
        DisgustDataset.setColor(Color.YELLOW);
        DisgustDataset.setCircleColor(Color.YELLOW);
        ContemptDataset= makeDataIntoGraph(Contempt,"Contempt");
        ContemptDataset.setColor(Color.GRAY);
        ContemptDataset.setCircleColor(Color.GRAY);
        AngerDataset = makeDataIntoGraph(Anger,"Anger");
        AngerDataset.setColor(Color.RED);
        AngerDataset.setCircleColor(Color.RED);
        //TODO tweak each datasets colors and radius etc

        //Add to a list of DataSets, then put it to LineData, then set to chart
        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(HappinessDataset);

        dataSets.add(NeutralDataset);
        dataSets.add(SadnessDataset);
        dataSets.add(FearDataset);
        dataSets.add(DisgustDataset);
        dataSets.add(ContemptDataset);
        dataSets.add(AngerDataset);


        EmotionData = new LineData(dataSets);

        chart.setData(EmotionData);
        chart.setExtraOffsets(10, 10, 10, 10);
        //Fetch the x Axis to format the date

        XAxis xAxis= chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {

            return datemap.get((int) value);
        }
        });
        xAxis.setGranularity(1f);
        xAxis.setDrawLabels(true);
        //xAxis.setCenterAxisLabels(true);
        //Vertical labels.
        //TODO possible bug, since dates are long, but formatter only takes float
        //xAxis.setLabelRotationAngle(90f);

        Legend legend = chart.getLegend();
        legend.setPosition(Legend.LegendPosition.ABOVE_CHART_CENTER);
        chart.invalidate();

        //24-05 Bug solving
        //UnixStamps displaying as format "24-05-2017 22:08:40"
        //DateStamps displaying as format " Wed May 24 22:03:40 GMT+01:00 2017"
        //LongStamps displaying as 14957650000 - Already in Long form, no need to multiply by 1000
        //TestStamps integer-based system work. So the problem is in The LongStamps
        //Approach now will shift to using an intermediate index as we did with investr
        //Meaning LongStamps exist only as labels, not as data.
    }

    //2605 testInts making methods
    public ArrayList<Integer>createTestInts(){
        ArrayList<Integer> newints = new ArrayList<>();

        for (int i = 0; i<UnixStamps.size();i++){
            newints.add(i);


        }

        return newints;
    }
    //2810 investr Datamap, we need to flip the dataset here
    public HashMap<Integer,String>createDateMap(){
        HashMap<Integer,String>map = new HashMap<>();


        for (int i =0;i<testInts.size();i++){

            map.put(i,parseDateIntoString(DateStamps.get(i)));
        }

        return map;


    }


    //Method from Investr
    public static String parseDateIntoString(Date inputDate) {
        String outputDate;

        //Create a format that user understands in String, then parse the date out of it

        SimpleDateFormat original = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        outputDate = original.format(inputDate);
        return outputDate;


    }

    //23-05 Also works effectively
    public ArrayList<Long>convertDatesToLongs(ArrayList<Date>dates){
        ArrayList<Long>longs= new ArrayList();

        for (int i=0;i<dates.size();i++){
            Date date = dates.get(i);
            long unixtime = date.getTime();
            longs.add(unixtime);

        }
        //Reverse necessary as again, its being plot wrong way around


        return longs;

    }
    //Custom reverse method, because I dont trust Collections
    public static ArrayList<Double> reverse(List<Double> list) {
        int length = list.size();
        ArrayList<Double> result = new ArrayList<Double>(length);

        for (int i = length - 1; i >= 0; i--) {
            result.add(list.get(i));
        }
        return result;
    }

    public static ArrayList<Integer> reverseInt(List<Integer> list) {
        int length = list.size();
        ArrayList<Integer> result = new ArrayList<Integer>(length);

        for (int i = length - 1; i >= 0; i--) {
            result.add(list.get(i));
        }
        return result;
    }

    public static ArrayList<String> reverseStr(List<String> list) {
        int length = list.size();
        ArrayList<String> result = new ArrayList<String>(length);

        for (int i = length - 1; i >= 0; i--) {
            result.add(list.get(i));
        }
        return result;
    }


    //todo WITH ALOT OF HELP FROM https://www.mkyong.com/java/how-to-convert-string-to-date-java///
    //Code works, but converts to Date of form Tue May 23 21:09:59 GMT +01:00 2017
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
    //2605 Added new method, but may not use this
    public ArrayList<String>convertDateFormat(ArrayList<Date>dateinputs){

        ArrayList<String>datesmod = new ArrayList();
        String outputDate;

        for (int i=0;i<dateinputs.size();i++){
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                outputDate = formatter.format(dateinputs.get(i));
               datesmod.add(outputDate);


        }

        return datesmod;
    }


    //OnChartGestureListener
    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }



public boolean checkInternetAccess() {

        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }
    //Very much simplified version of investr's code, since we are using the UNIX timestamp system, and not
    //A fake integer replacement date system ,TODO note that LineDataSets need to be added to LineData
    public LineDataSet makeDataIntoGraph(ArrayList<Double> lists, String name) {
        List<Entry> dataEntries = new ArrayList<Entry>();

        //TODO Apparently java casts long automatically to float, but could be a possible bug place
        float value = 0;
        int date =0;
       // float test;

        for (int i = 0; i < lists.size(); i++) {

            //TODO 2605 switching to testInts for all datasets
            value =lists.get(i).floatValue() ;


            date = testInts.get(i);


            dataEntries.add(new Entry(date, value));

        }
        //Turn into data
        LineDataSet indicatorData = new LineDataSet(dataEntries, name);
        //indicatorData.setAxisDependency(YAxis.AxisDependency.LEFT);



        return indicatorData;
    }



    public void resizeCharts() {
        if (auxchart.getVisibility() != View.VISIBLE) {
            ViewGroup.LayoutParams params = chart.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            chart.setLayoutParams(params);
        } else {
            ViewGroup.LayoutParams params = chart.getLayoutParams();
            float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, getResources().getDisplayMetrics());
            params.height = (int) pixels;
            chart.setLayoutParams(params);
        }

    }

    //Access SQLiteDB, based on similiar methods for emotionRecyclerActivity
    private ArrayList<Double> getEmotionColumn(String name) {
        Cursor cursortemp;
        ArrayList<Double> results = new ArrayList();
        if (name.equals(EmotionListContract.EmotionListEntry.COLUMN_ANGER)) {
            cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{EmotionListContract.EmotionListEntry.COLUMN_ANGER, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_ANGER))));
            }
        }
        if (name.equals(EmotionListContract.EmotionListEntry.COLUMN_CONTEMPT)) {
            cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{EmotionListContract.EmotionListEntry.COLUMN_CONTEMPT, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_CONTEMPT))));
            }
        }
        if (name.equals(EmotionListContract.EmotionListEntry.COLUMN_DISGUST)) {
            cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{EmotionListContract.EmotionListEntry.COLUMN_DISGUST, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_DISGUST))));
            }
        }
        if (name.equals(EmotionListContract.EmotionListEntry.COLUMN_FEAR)) {
            cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{EmotionListContract.EmotionListEntry.COLUMN_FEAR, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_FEAR))));
            }
        }
        if (name.equals(EmotionListContract.EmotionListEntry.COLUMN_HAPPINESS)) {
            cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{EmotionListContract.EmotionListEntry.COLUMN_HAPPINESS, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_HAPPINESS))));
            }
        }
        if (name.equals(EmotionListContract.EmotionListEntry.COLUMN_NEUTRAL)) {
            cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{EmotionListContract.EmotionListEntry.COLUMN_NEUTRAL, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_NEUTRAL))));
            }
        }
        if (name.equals(EmotionListContract.EmotionListEntry.COLUMN_SADNESS)) {
            cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{EmotionListContract.EmotionListEntry.COLUMN_SADNESS, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_SADNESS))));
            }
        }
        if (name.equals(EmotionListContract.EmotionListEntry.COLUMN_SURPRISE)) {
            cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{EmotionListContract.EmotionListEntry.COLUMN_SURPRISE, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);
            while (cursortemp.moveToNext()) {
                results.add(Double.valueOf(cursortemp.getDouble(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_SURPRISE))));
            }
        }
        return results;
    }
    //Returns the UnixColumn, which is in integer form after having been divided by 1000
    /*DEBUG only- TODO confirmed that is probably due to app still having old schema, now works, but we need to parse out the date out!!!
    //TODO try to parse out dates, otherwise we are forced to use currentTimeMills
    private ArrayList<Long> getUnixstampColumn(){
        Cursor cursortemp;
        ArrayList<Long> results = new ArrayList();
        cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{ EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);
        while (cursortemp.moveToNext()) {
            results.add(Long.valueOf(cursortemp.getString(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP))));
        }
        return results;
    }

    ERROR

    Unable to start activity ComponentInfo{com.xu.servicequalityrater/com.xu.servicequalityrater.GraphActivity}:
    java.lang.NumberFormatException: Invalid long: "2017-05-21 22:53:30"  NOTE THIS IS GMT not local
    */
//2305 changed to integer
   private ArrayList<String> getUnixstampColumn(){
        Cursor cursortemp;
        ArrayList<String> results = new ArrayList();
        cursortemp = this.db.query(EmotionListContract.EmotionListEntry.TABLE_NAME, new String[]{ EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP}, null, null, null, null, EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP);

       while (cursortemp.moveToNext()) {
            results.add(cursortemp.getString(cursortemp.getColumnIndex(EmotionListContract.EmotionListEntry.COLUMN_TIMESTAMP)));
        }
        return results;
    }

    //Takes the UNIXStamp column and turns it into the long value column
    //http://www.jiahaoliuliu.com/2011/09/sqlite-saving-date-as-integer.html
    private ArrayList<Long>turnIntegerIntoLongDate(ArrayList<Integer>ints){
        ArrayList<Long>timestamps = new ArrayList();
        for (int i = 0;i<timestamps.size();i++){
            long dateLong = new Long(ints.get(i));
            dateLong *= 1000;
            timestamps.add(dateLong);
        }
        return timestamps;
    }

    //Formatter code in order to turn longs to dates TODO 2605 No longer in use

    IAxisValueFormatter createDateFormatter() {
        IAxisValueFormatter formatter = new IAxisValueFormatter() {


            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Date date = new Date((long) value);

                SimpleDateFormat fmt;


//                switch (labelModeSelected) {
//                    case HOURS_FORMAT:
//                        fmt = new SimpleDateFormat("h:mm a");
//                        break;
//
//                    case DAYS_FORMAT:
//                        fmt = new SimpleDateFormat("E d");
//                        break;
//
//                    case WEEKS_FORMAT:
//                        fmt = new SimpleDateFormat("d MMM");
//                        break;
//
//                    case MONTHS_FORMAT:
//                        fmt = new SimpleDateFormat("MMM yyyy");
//                        break;
//
//                    case YEARS_FORMAT:
//                        fmt = new SimpleDateFormat("yyyy");
//
//                        break;
//
//                    default:
//                        fmt = new SimpleDateFormat("E d MMM");
//                        break;
//                }


                fmt = new SimpleDateFormat("MMM d HH:mm:ss"); //TODO remove after tests and add switch
                fmt.setTimeZone(TimeZone.getDefault()); // sets time zone... I think I did this properly...


                String s = fmt.format(date);


                return s;
            }

            // we don't draw numbers, so no decimal digits needed
            public int getDecimalDigits() {
                return 0;
            }


        };

        return formatter;
    }



}
