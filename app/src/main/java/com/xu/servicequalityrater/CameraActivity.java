package com.xu.servicequalityrater;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.xu.servicequalityrater.data.EmotionListContract;
import com.xu.servicequalityrater.data.EmotionListContract.EmotionListEntry;
import com.xu.servicequalityrater.data.EmotionListDbHelper;
import com.xu.servicequalityrater.services.AlarmReceiver;
import com.xu.servicequalityrater.services.HealthAlarmReceiver;
import com.xu.servicequalityrater.services.HealthPictureService2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

//TODO 02082017 trial code disabled in camActivity and HPS2 for SGH
//TODO 0805 AlarmyReceiver works
//TODO, Export existing, then start changing
//TODO, camera facing front, check output csv, start polish, remove excess apps, change to veritas, plan attachment to amazonDB + Graphing

//TODO Maybe disable preview until the dialogfragment is disabled? Use the boolean check
// TODO 14-05-2017 all  camera issues solved, thanks to  https://gist.github.com/tolmachevroman/c267024f49109955d135

//BUG clearDB doesnt clear counter
public class CameraActivity extends AppCompatActivity implements ProfileDialogWindow.EditProfileListener {
    public static final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    private static final int REQUEST_RUNTIME_PERMISSION = 123;

    Bitmap b;
    private ByteArrayOutputStream bos;
    Button captureButton;
    Button clear;
    private EmotionServiceClient client;
    int counter = 0;
    public SQLiteDatabase db;
    private File dir_image;
    private File dir_image2;
    private FileInputStream fis;
    ByteArrayInputStream fis2;
    private FileOutputStream fos;
    public EmotionListDbHelper helper;
    Button launchView;
    Bitmap mBitmap;
    SharedPreferences welcomesp;
    SharedPreferences profileSave;
    SharedPreferences checkDate;
    SharedPreferences.Editor edit;

    boolean shown;

    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private FrameLayout mImage;
    Boolean autostart;
    Button alarmsync;
    AlarmReceiver receiver;
    String buttonText;
    String profiletext;
    TextView profileView;

    long trialstart;
    long trialend;





    PictureCallback mPicture = new PictureCallback() {
        private static final int IMAGE_MAX_SIDE_LENGTH = 1280;

        public void onPictureTaken(byte[] data, Camera camera) {
            CameraActivity.this.dir_image2 = new File(Environment.getExternalStorageDirectory() + File.separator + "My Custom Folder");
            CameraActivity.this.dir_image2.mkdirs();
            File tmpFile = new File(CameraActivity.this.dir_image2, "TempImage.jpg");
            try {
                CameraActivity.this.fos = new FileOutputStream(tmpFile);
                CameraActivity.this.fos.write(data);
                CameraActivity.this.fos.close();
            } catch (FileNotFoundException e) {
                Toast.makeText(CameraActivity.this.getApplicationContext(), "Error",  Toast.LENGTH_LONG).show();
            } catch (IOException e2) {
                Toast.makeText(CameraActivity.this.getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
            }
            CameraActivity.this.options = new Options();
            CameraActivity.this.options.inPreferredConfig = Config.ARGB_8888;
            CameraActivity.this.mBitmap = decodeFile(tmpFile);
            CameraActivity.this.processResults();
            tmpFile.delete();
        }

        public Bitmap decodeFile(File f) {
            try {
                CameraActivity.this.o = new Options();
                CameraActivity.this.o.inJustDecodeBounds = true;
                CameraActivity.this.fis = new FileInputStream(f);
                BitmapFactory.decodeStream(CameraActivity.this.fis, null, CameraActivity.this.o);
                CameraActivity.this.fis.close();
                int scale = 1;
                if (CameraActivity.this.o.outHeight > 1000 || CameraActivity.this.o.outWidth > 1000) {
                    scale = (int) Math.pow(2.0d, (double) ((int) Math.round(Math.log(1000.0d / ((double) Math.max(CameraActivity.this.o.outHeight, CameraActivity.this.o.outWidth))) / Math.log(0.5d))));
                }
                CameraActivity.this.o2 = new Options();
                CameraActivity.this.o2.inSampleSize = scale;
                CameraActivity.this.fis = new FileInputStream(f);
                Bitmap b = BitmapFactory.decodeStream(CameraActivity.this.fis, null, CameraActivity.this.o2);
                CameraActivity.this.fis.close();
                return rotateBitmap(b, 270);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        private Bitmap rotateBitmap(Bitmap bitmap, int angle) {
            if (angle == 0) {
                return bitmap;
            }
            Matrix matrix = new Matrix();
            matrix.postRotate((float) angle);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
    };
    private Options o;
    private Options o2;
    private Options options;
    Boolean permissionsGranted = Boolean.valueOf(true);
    public TextView resultsView;
    public ScrollView myscroll;
    Timer timer;
    Integer count = Integer.valueOf(0);

    class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        private Exception e = null;
        // The best way to save timestamp is to do it raw, and then conver it later
        //http://stackoverflow.com/questions/18206257/how-to-insert-data-type-dateyyyy-mm-dd-in-sqlite-database-and-retrive-data-bet
        //TODO Currently not used due to SQLite's own datetime, also removed cv.put(EmotionListEntry.COLUMN_TIMESTAMP, Long.valueOf(currenttimestamp));
        //If this works gotta mod servie and also emotionrecycler
        long currenttimestamp =  System.currentTimeMillis();

        //TODO i think I know why it didnt work before- db.insert makes a whole new row!
        // TODO you'd be inserting 2 rows with old method
        private long storeTimeInUnix(){
            long nowLong = System.currentTimeMillis()/1000;
            int nowInt = (int)nowLong;

            ContentValues cv = new ContentValues();
            cv.put(EmotionListEntry.COLUMN_TIMESTAMP, nowInt);
            return CameraActivity.this.db.insert(EmotionListEntry.TABLE_NAME, null, cv);


        }



        private long addEmotion(double anger, double contempt, double disgust, double fear, double happiness, double neutral, double sadness, double surprise ) {
            ContentValues cv = new ContentValues();
            cv.put(EmotionListEntry.COLUMN_ANGER, Double.valueOf(anger));
            cv.put(EmotionListEntry.COLUMN_CONTEMPT, Double.valueOf(contempt));
            cv.put(EmotionListEntry.COLUMN_DISGUST, Double.valueOf(disgust));
            cv.put(EmotionListEntry.COLUMN_FEAR, Double.valueOf(fear));
            cv.put(EmotionListEntry.COLUMN_HAPPINESS, Double.valueOf(happiness));
            cv.put(EmotionListEntry.COLUMN_NEUTRAL, Double.valueOf(neutral));
            cv.put(EmotionListEntry.COLUMN_SADNESS, Double.valueOf(sadness));
            cv.put(EmotionListEntry.COLUMN_SURPRISE, Double.valueOf(surprise));



            return CameraActivity.this.db.insert(EmotionListEntry.TABLE_NAME, null, cv);
        }

        protected List<RecognizeResult> doInBackground(String... args) {
            try {
                return CameraActivity.this.processWithAutoFaceDetection();
            } catch (Exception e) {
                this.e = e;
                return null;
            }
        }

        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            CameraActivity.this.resultsView.setMovementMethod(new ScrollingMovementMethod());
            CameraActivity.this.resultsView.append("\n\nRecognizing emotions with auto-detected face rectangles...\n");
            if (this.e != null) {
                CameraActivity.this.resultsView.setText("Error: " + this.e.getMessage());
                this.e = null;
            } else if (result.size() == 0) {
                CameraActivity.this.resultsView.append("No emotion detected :(");
            } else {

                for (RecognizeResult r : result) {
                    CameraActivity.this.resultsView.append(String.format("\n", new Object[0]));
                    CameraActivity.this.resultsView.append(String.format("\nFace #%1$d \n", new Object[]{count}));
                    CameraActivity.this.resultsView.append(String.format("\t anger: %1$.5f\n", new Object[]{Double.valueOf(r.scores.anger)}));
                    CameraActivity.this.resultsView.append(String.format("\t contempt: %1$.5f\n", new Object[]{Double.valueOf(r.scores.contempt)}));
                    CameraActivity.this.resultsView.append(String.format("\t disgust: %1$.5f\n", new Object[]{Double.valueOf(r.scores.disgust)}));
                    CameraActivity.this.resultsView.append(String.format("\t fear: %1$.5f\n", new Object[]{Double.valueOf(r.scores.fear)}));
                    CameraActivity.this.resultsView.append(String.format("\t happiness: %1$.5f\n", new Object[]{Double.valueOf(r.scores.happiness)}));
                    CameraActivity.this.resultsView.append(String.format("\t neutral: %1$.5f\n", new Object[]{Double.valueOf(r.scores.neutral)}));
                    CameraActivity.this.resultsView.append(String.format("\t sadness: %1$.5f\n", new Object[]{Double.valueOf(r.scores.sadness)}));
                    CameraActivity.this.resultsView.append(String.format("\t surprise: %1$.5f\n", new Object[]{Double.valueOf(r.scores.surprise)}));
                    CameraActivity.this.resultsView.append(String.format("\n", new Object[0]));
                    CameraActivity.this.resultsView.append("Results saved into database");
                    myscroll.fullScroll(ScrollView.FOCUS_DOWN);
                    //TODO make the textView Scroll down http://stackoverflow.com/questions/19826693/how-can-i-make-a-textview-automatically-scroll-as-i-add-more-lines-of-text
                    addEmotion(r.scores.anger, r.scores.contempt, r.scores.disgust, r.scores.fear, r.scores.happiness, r.scores.neutral, r.scores.sadness, r.scores.surprise);

                    count = Integer.valueOf(count.intValue() + 1);
                }
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //this.mCamera = getCameraInstance();

        Toolbar mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        checkDate = getSharedPreferences("checkdate",Context.MODE_PRIVATE);
        edit = checkDate.edit();
        //0208 temporaily disabled for SGH
        this.client = new EmotionServiceRestClient(getString(R.string.api_key));
        //checkTrialValidity();

        /*getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);*/

        this.mCamera = openFrontFacingCameraGingerbread();
        if (VERSION.SDK_INT >= 23 && !(checkSelfPermission("android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED && checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED  && checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED && checkSelfPermission("android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE","android.permission.READ_PHONE_STATE"}, PERMISSIONS_MULTIPLE_REQUEST);
        }
        this.mCameraPreview = new CameraPreview(this, this.mCamera);
        this.mImage = (FrameLayout) findViewById(R.id.selectedImage);
        this.mImage.addView(this.mCameraPreview);


        CameraActivity.this.timer = new Timer();
        receiver= new AlarmReceiver();





         //TODO autostart code removed, 11-05 Seems to work, need to test more. Toasts changed to text
        alarmsync= (Button)findViewById(R.id.syncAlarm);
        //upon clicking alarmsync, we set receiver register/deregister
        alarmsync.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final ComponentName component = new ComponentName(CameraActivity.this, HealthAlarmReceiver.class);
                final int status = CameraActivity.this.getPackageManager().getComponentEnabledSetting(component);
                if(status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    resultsView.setText("Automatic evaluation disabled");
                    CameraActivity.this.getPackageManager()
                            .setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP);

                }else if(status == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    resultsView.setText("Automatic evaluation enabled");
                    CameraActivity.this.getPackageManager()
                            .setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                    PackageManager.DONT_KILL_APP);

                }
            }
        });



        profileSave=getSharedPreferences("profileSave",Context.MODE_PRIVATE);

        this.helper = new EmotionListDbHelper(this);
        this.db = this.helper.getWritableDatabase();
       captureButton = (Button) findViewById(R.id.buttonCaptureImage);
        this.resultsView = (TextView) findViewById(R.id.editTextResult);
        this.myscroll = (ScrollView)findViewById(R.id.SCROLLER_ID);
        captureButton.setOnClickListener(new OnClickListener() {


            public void onClick(View v) {
                String buttonText = captureButton.getText().toString();
                if (client ==null){
                    resultsView.setText("");
                    resultsView.append("Your Veritas subscription has expired. Please renew your prescription at your doctor");
                }else{
                    if (buttonText.equals("Test Analysis")) {
                        if(CameraActivity.this.timer ==null){
                            CameraActivity.this.timer = new Timer();
                        }
                        resultsView.setText("Analysis activated. Please wait...");

                        if (CameraActivity.this.permissionsGranted.booleanValue()) {
                            CameraActivity.this.resultsView.clearComposingText();
                            captureButton.setText("Stop Test");

                            CameraActivity.this.timer.schedule(new TimerTask() {
                                public void run() {
                                    CameraActivity.this.mCamera.startPreview();

                                    //Start the picture taking process
                                    mCamera.takePicture(null, null, mPicture);


                                }
                            }, 0, 10000);
                        }
                    } else if (buttonText.equals("Stop Test")) {
                        captureButton.setText("Test Analysis");
                        //TODO 11-05 FIXED! with cancel, purge + set null combo!
                        //Or otherwise stuck on on second loop, not sure

                        CameraActivity.this.timer.cancel();
                        CameraActivity.this.timer.purge();
                        CameraActivity.this.timer=null;



                        CameraActivity.this.mCameraPreview = new CameraPreview(CameraActivity.this, CameraActivity.this.mCamera);


                    }
                }


            }
        });
        this.launchView = (Button) findViewById(R.id.buttonViewResults);
        this.launchView.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if(CameraActivity.this.timer!= null){
                    CameraActivity.this.timer.cancel();
                }

                CameraActivity.this.startActivity(new Intent(CameraActivity.this, EmotionRecyclerActivity.class));
            }
        });
        this.clear = (Button) findViewById(R.id.buttonClear);
        this.clear.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {

                CameraActivity.this.deleteAll();
                resultsView.setText("All local records deleted");
            }
        });
        welcomesp = PreferenceManager.getDefaultSharedPreferences(this);
        shown = welcomesp.getBoolean("welcomeShown",false);
        if(shown == false){
            new TutorialDialogWindow().show(getSupportFragmentManager(), "");
        }
        profileView =(TextView)findViewById(R.id.nameView);
        profileView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new ProfileDialogWindow().show(getSupportFragmentManager(),"");
            }
        });
        //0106 Following is code for creating a user profile
        attemptLoadProfile();








    }
    public void attemptLoadProfile(){
        profiletext=profileSave.getString("profilename","");
        if(!profiletext.equals("")){
            profileView.setText("Name: "+profiletext);
        };
    }

    @Override
    public void profileEdited(){

                attemptLoadProfile();
    }




    /*public void surfaceDestroyed(SurfaceHolder holder) {
    Code exists in Camera Preview

        mCamera.stopPreview();
        mCamera.release();
        mCamera=null;
    }
    */
    // http://stackoverflow.com/questions/11495842/how-surfaceholder-callbacks-are-related-to-activity-lifecycle
    @Override
    public void onResume() {
        super.onResume();
       try{
           mCamera = openFrontFacingCameraGingerbread();
          // mCamera.startPreview();
           this.mCameraPreview = new CameraPreview(this, this.mCamera);
            mImage.removeAllViews();
           this.mImage.addView(this.mCameraPreview);

       }catch (RuntimeException ex){

        }



    }


    @Override
    public void onPause() {
        super.onPause();
        captureButton.setText("Test Analysis");
        if(CameraActivity.this.timer !=null) {
            CameraActivity.this.timer.cancel();
            CameraActivity.this.timer.purge();
            CameraActivity.this.timer = null;
        }
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCameraPreview.getHolder().removeCallback(mCameraPreview);
            mCamera.release();
            mCamera = null;
        }


    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        releaseCameraAndPreview();
    }

    private void releaseCameraAndPreview() {

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        if(mCameraPreview != null){
            mCameraPreview.destroyDrawingCache();
            mCameraPreview.mCamera = null;
        }
    }


    public void deleteAll() {
        this.db.delete(EmotionListEntry.TABLE_NAME, null, null);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST /*123*/:
                if (grantResults.length > 0) {
                    boolean cameraPermission;
                    if (grantResults[0] == 0) {
                        cameraPermission = true;
                    } else {
                        cameraPermission = false;
                    }
                    boolean readExternalFile;
                    if (grantResults[1] == 0) {
                        readExternalFile = true;
                    } else {
                        readExternalFile = false;
                    }
                    boolean writeExternalFile;
                    if (grantResults[2] == 0) {
                        writeExternalFile = true;
                    } else {
                        writeExternalFile = false;
                    }
                    if (cameraPermission && readExternalFile && writeExternalFile) {
                        this.captureButton.setEnabled(true);
                        return;
                    }
                    return;
                }
                Snackbar.make(findViewById(R.id.coordinator), "Please Grant Permissions to take photos", Snackbar.LENGTH_SHORT).setAction("ENABLE", new OnClickListener() {
                    public void onClick(View v) {
                        if (VERSION.SDK_INT >= 23) {
                            CameraActivity.this.requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.CAMERA"}, CameraActivity.PERMISSIONS_MULTIPLE_REQUEST);
                        }
                    }
                }).show();
                return;
            default:
                return;
        }
    }

    public void processResults() {
        new doRequest().execute(new String[0]);
    }

    private Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
        }
        return camera;
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
        if (mediaStorageDir.exists() || mediaStorageDir.mkdirs()) {
            return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg");
        }
        Log.d("MyCameraApp", "failed to create directory");
        return null;
    }

    //TODO 0805 Method to programatically disable/enable the broadcastReceiver FROM MANIFEST
    //TODO I dont think this entire onClick is being called, try moving it
    private void changeReceiverStatusInManifest(Class<? extends BroadcastReceiver> clazz, final Context context) {
        final ComponentName component = new ComponentName(context, clazz);
        final int status = context.getPackageManager().getComponentEnabledSetting(component);
        if(status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            context.getPackageManager()
                    .setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
            this.resultsView.setText("Automatic evaluation disabled");
        }else if(status == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            context.getPackageManager()
                    .setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
            this.resultsView.setText("Automatic evaluation enabled");
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_instructions ){
            new TutorialDialogWindow().show(getSupportFragmentManager(), "");
        }

        if(id == R.id.action_profile){
            new ProfileDialogWindow().show(getSupportFragmentManager(),"");
        }
        return true;
    }


        private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");
        Gson gson = new Gson();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        this.mBitmap.compress(CompressFormat.JPEG, 100, output);
        InputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        long startTime = System.currentTimeMillis();
        List<RecognizeResult> result = this.client.recognizeImage(inputStream);
        Log.d("result", gson.toJson(result));
        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", new Object[]{Long.valueOf(System.currentTimeMillis() - startTime)}));
        return result;
}

    private Camera openFrontFacingCameraGingerbread() {
            /*if (mCamera != null) {

                mCamera.stopPreview();
                mCamera.release();
            }
            */
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e("Camera",
                            "Camera failed to open: " + e.getLocalizedMessage());
                    /*
                     * Toast.makeText(getApplicationContext(),
                     * "Front Camera failed to open", Toast.LENGTH_LONG)
                     * .show();
                     */
                }
            }
        }
        return cam;
    }

    //TODO 30082017 TrialModeData

    public void checkTrialValidity(){
        trialstart = checkDate.getLong("trialstart",0L);

        if (trialstart==0L){
            //First time using app, create and save time in sharedpreference
//https://stackoverflow.com/questions/17201848/comparing-current-time-in-milliseconds-to-time-saved-in-shared-preferences
            long currentDateTime = System.currentTimeMillis();

            edit.putLong("trialstart",currentDateTime);
            //End is incorrect



            edit.commit();
        }else{
            long twomin = 2592000000l;
            //Otherwise. fetch both values and check
           if (System.currentTimeMillis() - trialstart>twomin) {
                //Toast.makeText(CameraActivity.this,"twomin passed",Toast.LENGTH_LONG).show();
               client = null;
            }else {
                //Toast.makeText(CameraActivity.this,"Not passed yet",Toast.LENGTH_LONG).show();
               if (this.client == null) {
                   this.client = new EmotionServiceRestClient(getString(R.string.api_key));
               }
            }

        }


    }
    //TODO 0709 Post Beider meeting addon

    @Override
    public void onBackPressed(){

        //new AuxTutorialFragment().show(getSupportFragmentManager(), "");

        new AlertDialog.Builder(this)
                .setTitle("Automatic Analysis Active")
                .setMessage("Veritas will continue operating in the background. Please use your device as normal.\n\nIf you wish to view or send off your records, please re-open the application and click 'View Records' ")

                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        CameraActivity.super.onBackPressed();
                    }
                }).create().show();

    }

}
