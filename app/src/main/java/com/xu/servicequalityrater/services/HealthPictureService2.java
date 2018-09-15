package com.xu.servicequalityrater.services;

import android.Manifest;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;
import com.intentfilter.androidpermissions.PermissionManager;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.xu.servicequalityrater.CameraActivity;
import com.xu.servicequalityrater.R;
import com.xu.servicequalityrater.data.EmotionListContract;
import com.xu.servicequalityrater.data.EmotionListDbHelper;
import com.xu.servicequalityrater.listeners.OnTaskCompleted;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Collections.list;
import static java.util.Collections.singleton;

/** TODO 04-05 Added in Toast making capability, in order to test if Service has been running
 * TODO 04-05 Seems to be working to some degree, but not storing it necessarily. Testing it separvately
 * TODO 0605 Storage in MYGALLERY seems to be working, with back camera action. Doesnt work when both cameras
 * TODO are active (k.s. CameraActivity). Use TestActivity to test the services.
 * TODO 0705 Tests show that if no emotions detected, NO RESULTS RETURNED AND STORED IN SQL
 * TODO 0705 Update- "Error" received, so not getting any results whatsoever
 * TODO 0705 Update 2  = WORKS WHEN UPSIDE DOWN CAMERA! Basically replaced all the code for camera onPictureTaken callback
 * TODO 0805 Rotate at 270 works, but we need to test with other phones, make sure its unverisal solution
 * http://sankarganesh-info-exchange.blogspot.co.uk/2011/03/dispalying-toast-message-in-service-in.html
 * Second candidate for Taking pictures, using Camera API 1
 * The start command here should be onStartCommand, which is called by an intent with StartService, I think there
 * Should be some stopService related
 *
 * Think this is the same as CameraOneService, but I dont think I did this well
 *
 */

public class HealthPictureService2 extends Service implements
        SurfaceHolder.Callback, OnTaskCompleted {


        // Camera variables
        // a surface holder
        // a variable to control the camera
        private Camera mCamera;
        // the camera parameters
        private Camera.Parameters parameters;
        Bitmap bmp;
        FileOutputStream fo;
        private String FLASH_MODE;
        int QUALITY_MODE = 100;
    //TODO 0605 manually setting front camera request, actually set via intent using broadcastReceiver
        private boolean isFrontCamRequest = true;
        private Camera.Size pictureSize;
        SurfaceView sv;
        private SurfaceHolder sHolder;
        private WindowManager windowManager;
        WindowManager.LayoutParams params;
        public Intent cameraIntent;
        SharedPreferences pref;
        SharedPreferences.Editor editor;
        int width = 0, height = 0;
    EmotionListDbHelper helper;
    public SQLiteDatabase db;
    private EmotionServiceRestClient client;
    int counter = 0; //TODO, amend with timestamp

    //TODO BitmapFactory Options
    private BitmapFactory.Options o;
    private BitmapFactory.Options o2;
    private BitmapFactory.Options options;
    private File dir_image;
    private File dir_image2;
    private FileInputStream fis;
    ByteArrayInputStream fis2;
    private FileOutputStream fos;
    

    /*
    Thread t = new Thread(){
        public void run(){

            Message myMessage=new Message();
            Bundle resBundle = new Bundle();
            resBundle.putString("status", "SUCCESS");
            myMessage.obj=resBundle;
            threadhandler.sendMessage(myMessage);
        }
    };
    //http://sankarganesh-info-exchange.blogspot.co.uk/2011/03/dispalying-toast-message-in-service-in.html
    private Handler threadhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), "Service is Running", Toast.LENGTH_LONG).show();
        }
    };
    */



    /** Called when the activity is first created. */
        @Override
        public void onCreate() {
            super.onCreate();
            //t.start();
            this.helper = new EmotionListDbHelper(this);
            this.db = this.helper.getWritableDatabase();
            if (this.client == null) {
                this.client = new EmotionServiceRestClient(getString(R.string.api_key));
            }
            PermissionManager permissionManager = PermissionManager.getInstance(this);
            permissionManager.checkPermissions(singleton(Manifest.permission.CAMERA), new PermissionManager.PermissionRequestListener() {
                @Override
                public void onPermissionGranted() {
                    //Toast.makeText(getApplicationContext(), "Permissions Granted", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onPermissionDenied() {
                    //Toast.makeText(getApplicationContext(), "Permissions Denied", Toast.LENGTH_SHORT).show();
                }
            });




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

        private void setBesttPictureResolution() {
            // get biggest picture size
            width = pref.getInt("Picture_Width", 0);
            height = pref.getInt("Picture_height", 0);

            if (width == 0 | height == 0) {
                pictureSize = getBiggesttPictureSize(parameters);
                if (pictureSize != null)
                    parameters
                            .setPictureSize(pictureSize.width, pictureSize.height);
                // save width and height in sharedprefrences
                width = pictureSize.width;
                height = pictureSize.height;
                editor.putInt("Picture_Width", width);
                editor.putInt("Picture_height", height);
                editor.commit();

            } else {
                // if (pictureSize != null)
                parameters.setPictureSize(width, height);
            }
        }

        private Camera.Size getBiggesttPictureSize(Camera.Parameters parameters) {
            Camera.Size result = null;

            for (Camera.Size size : parameters.getSupportedPictureSizes()) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }

            return (result);
        }

        /** Check if this device has a camera */
        private boolean checkCameraHardware(Context context) {
            if (context.getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA)) {
                // this device has a camera
                return true;
            } else {
                // no camera on this device
                return false;
            }
        }

        /** Check if this device has front camera */
        private boolean checkFrontCamera(Context context) {
            if (context.getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FRONT)) {
                // this device has front camera
                return true;
            } else {
                // no front camera on this device
                return false;
            }
        }

        Handler handler = new Handler();

        private class TakeImage extends AsyncTask<Intent, Void, Void> {

            @Override
            protected Void doInBackground(Intent... params) {
                takeImage(params[0]);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
            }
        }

        private synchronized void takeImage(Intent intent) {

            if (checkCameraHardware(getApplicationContext())) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String flash_mode = extras.getString("FLASH");
                    FLASH_MODE = flash_mode;

                    //TODO while its great that we had this for starting using a BroadCastReceiver,
                    /*TODO but for starting manually, we will be setting front request ourselves
                    boolean front_cam_req = extras.getBoolean("Front_Request");
                    isFrontCamRequest = front_cam_req;
                    */

                    /*TODO while its great that we had this for starting using a BroadCastReceiver,
                    TODO but for starting manually, we will be setting quality mode ourselves
                    int quality_mode = extras.getInt("Quality_Mode");
                    QUALITY_MODE = quality_mode;
                    */
                }

                if (isFrontCamRequest) {

                    // set flash 0ff
                    FLASH_MODE = "off";
                    // only for gingerbread and newer versions
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {

                        mCamera = openFrontFacingCameraGingerbread();
                        if (mCamera != null) {

                            try {
                                mCamera.setPreviewDisplay(sv.getHolder());
                            } catch (IOException e) {
                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(),
                                                "API dosen't support front camera",
                                                Toast.LENGTH_LONG).show();
                                    }
                                });

                                stopSelf();
                            }
                            Camera.Parameters parameters = mCamera.getParameters();
                            pictureSize = getBiggesttPictureSize(parameters);
                            if (pictureSize != null)
                                parameters
                                        .setPictureSize(pictureSize.width, pictureSize.height);

                            // set camera parameters
                            mCamera.setParameters(parameters);
                            mCamera.startPreview();
                            mCamera.takePicture(null, null, mCall);

                            // return 4;

                        } else {
                            mCamera = null;
                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    Toast.makeText(
                                            getApplicationContext(),
                                            "Your Device dosen't have Front Camera !",
                                            Toast.LENGTH_LONG).show();
                                }
                            });

                            stopSelf();
                        }
                    /*
                     * sHolder = sv.getHolder(); // tells Android that this
                     * surface will have its data // constantly // replaced if
                     * (Build.VERSION.SDK_INT < 11)
                     *
                     * sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
                     */
                    } else {
                        if (checkFrontCamera(getApplicationContext())) {
                            mCamera = openFrontFacingCameraGingerbread();

                            if (mCamera != null) {

                                try {
                                    mCamera.setPreviewDisplay(sv.getHolder());
                                } catch (IOException e) {
                                    handler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            Toast.makeText(
                                                    getApplicationContext(),
                                                    "API dosen't support front camera",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    stopSelf();
                                }
                                Camera.Parameters parameters = mCamera.getParameters();
                                pictureSize = getBiggesttPictureSize(parameters);
                                if (pictureSize != null)
                                    parameters
                                            .setPictureSize(pictureSize.width, pictureSize.height);

                                // set camera parameters
                                mCamera.setParameters(parameters);
                                mCamera.startPreview();
                                mCamera.takePicture(null, null, mCall);
                                // return 4;

                            } else {
                                mCamera = null;
                            /*
                             * Toast.makeText(getApplicationContext(),
                             * "API dosen't support front camera",
                             * Toast.LENGTH_LONG).show();
                             */
                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        Toast.makeText(
                                                getApplicationContext(),
                                                "Your Device dosen't have Front Camera !",
                                                Toast.LENGTH_LONG).show();

                                    }
                                });

                                stopSelf();

                            }
                            // Get a surface
                        /*
                         * sHolder = sv.getHolder(); // tells Android that this
                         * surface will have its data // constantly // replaced
                         * if (Build.VERSION.SDK_INT < 11)
                         *
                         * sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS
                         * );
                         */
                        }

                    }

                } else {

                    if (mCamera != null) {
                        mCamera.stopPreview();
                        mCamera.release();
                        mCamera = Camera.open();
                    } else
                        mCamera = getCameraInstance();

                    try {
                        if (mCamera != null) {
                            mCamera.setPreviewDisplay(sv.getHolder());
                            parameters = mCamera.getParameters();
                            if (FLASH_MODE == null || FLASH_MODE.isEmpty()) {
                                FLASH_MODE = "auto";
                            }
                            parameters.setFlashMode(FLASH_MODE);
                            // set biggest picture
                            setBesttPictureResolution();
                            // log quality and image format
                            Log.d("Qaulity", parameters.getJpegQuality() + "");
                            Log.d("Format", parameters.getPictureFormat() + "");

                            // set camera parameters
                            mCamera.setParameters(parameters);
                            mCamera.startPreview();
                            Log.d("ImageTakin", "OnTake()");
                            mCamera.takePicture(null, null, mCall);
                        } else {
                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(),
                                            "Camera is unavailable !",
                                            Toast.LENGTH_LONG).show();
                                }
                            });

                        }
                        // return 4;

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        Log.e("TAG", "CmaraHeadService()::takePicture", e);
                    }
                    // Get a surface
                /*
                 * sHolder = sv.getHolder(); // tells Android that this surface
                 * will have its data constantly // replaced if
                 * (Build.VERSION.SDK_INT < 11)
                 *
                 * sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                 */

                }

            } else {
                // display in long period of time
            /*
             * Toast.makeText(getApplicationContext(),
             * "Your Device dosen't have a Camera !", Toast.LENGTH_LONG)
             * .show();
             */
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Your Device dosen't have a Camera !",
                                Toast.LENGTH_LONG).show();
                    }
                });
                stopSelf();
            }

            // return super.onStartCommand(intent, flags, startId);

        }

        @SuppressWarnings("deprecation")
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {

            cameraIntent = intent;
            Log.d("ImageTakin", "StartCommand()");
            pref = getApplicationContext().getSharedPreferences("MyPref", 0);
            editor = pref.edit();

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.width = 1;
            params.height = 1;
            params.x = 0;
            params.y = 0;
            sv = new SurfaceView(getApplicationContext());

            windowManager.addView(sv, params);
            sHolder = sv.getHolder();
            sHolder.addCallback(this);

            // tells Android that this surface will have its data constantly
            // replaced
            if (Build.VERSION.SDK_INT < 11) {
                sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            }
            return Service.START_STICKY;
        }

        Camera.PictureCallback mCall = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
               dir_image2 = new File(Environment.getExternalStorageDirectory() + File.separator + "My Custom Folder");
                dir_image2.mkdirs();
                File tmpFile = new File(dir_image2, "TempImage.jpg");
                try {
                   fos = new FileOutputStream(tmpFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "Error",  Toast.LENGTH_LONG).show();
                } catch (IOException e2) {
                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                }
               options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bmp = decodeFile(tmpFile);
               processResults();
                tmpFile.delete();
            }
        };

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public static Camera getCameraInstance() {
            Camera c = null;
            try {
                c = Camera.open(); // attempt to get a Camera instance
            } catch (Exception e) {
                // Camera is not available (in use or does not exist)
            }
            return c; // returns null if camera is unavailable
        }
        //TODO need to also destroy camera in CameraActivity
        @Override
        public void onDestroy() {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            if (sv != null)
                windowManager.removeView(sv);
            Intent intent = new Intent("custom-event-name");
            // You can also include some extra data.
            intent.putExtra("message", "This is my message!");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            super.onDestroy();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            // TODO Auto-generated method stub

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (cameraIntent != null)
                new TakeImage().execute(cameraIntent);

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }


    //TODO 0705 Second, change the method below to match those of cameraActivity, FIRST SAVE
    public Bitmap decodeFile(File f) {
        try {
           o = new BitmapFactory.Options();
          o.inJustDecodeBounds = true;
            fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
          this.fis.close();
            int scale = 1;
            if (o.outHeight > 1000 || o.outWidth > 1000) {
                scale = (int) Math.pow(2.0d, (double) ((int) Math.round(Math.log(1000.0d / ((double) Math.max(o.outHeight, o.outWidth))) / Math.log(0.5d))));
            }
            o2 = new BitmapFactory.Options();
           o2.inSampleSize = scale;
           fis = new FileInputStream(f);
            Bitmap b = BitmapFactory.decodeStream(fis, null, o2);
           fis.close();
            return rotateBitmap(b, 270);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    //Following method disabled for now
    private Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        if (angle == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate((float) angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    //TODO 0705 I wonder if the problem here is the fact that these outputstreams are NOT CONNECTED! TO BITMAP!
    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");
        Gson gson = new Gson();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        this.bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);

        // TODO 0705 Created our own outputstream instead
        //TODO 0705 For the first time, NO EMOTION DETECT toast seen! However, went back to error soon

        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        long startTime = System.currentTimeMillis();
        List<RecognizeResult> result = this.client.recognizeImage(inputStream);
        Log.d("result", gson.toJson(result));
        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", new Object[]{Long.valueOf(System.currentTimeMillis() - startTime)}));
        return result;
    }
    public void processResults( ) {
        //this in argument here refers to OnTaskCompletedListener
        new doRequest(this).execute();
    }
    @Override
    public void onTaskCompleted (List<RecognizeResult> result){
        for (RecognizeResult r : result) {
            Toast.makeText(getApplicationContext(),"Emotion detected",Toast.LENGTH_LONG).show();


            addEmotion(r.scores.anger, r.scores.contempt, r.scores.disgust, r.scores.fear, r.scores.happiness, r.scores.neutral, r.scores.sadness, r.scores.surprise);

            HealthPictureService2.this.counter+=1 ;
        }
    }

    private long addEmotion(double anger, double contempt, double disgust, double fear, double happiness, double neutral, double sadness, double surprise) {
        ContentValues cv = new ContentValues();
        cv.put(EmotionListContract.EmotionListEntry.COLUMN_ANGER, Double.valueOf(anger));
        cv.put(EmotionListContract.EmotionListEntry.COLUMN_CONTEMPT, Double.valueOf(contempt));
        cv.put(EmotionListContract.EmotionListEntry.COLUMN_DISGUST, Double.valueOf(disgust));
        cv.put(EmotionListContract.EmotionListEntry.COLUMN_FEAR, Double.valueOf(fear));
        cv.put(EmotionListContract.EmotionListEntry.COLUMN_HAPPINESS, Double.valueOf(happiness));
        cv.put(EmotionListContract.EmotionListEntry.COLUMN_NEUTRAL, Double.valueOf(neutral));
        cv.put(EmotionListContract.EmotionListEntry.COLUMN_SADNESS, Double.valueOf(sadness));
        cv.put(EmotionListContract.EmotionListEntry.COLUMN_SURPRISE, Double.valueOf(surprise));

        return db.insert(EmotionListContract.EmotionListEntry.TABLE_NAME, null, cv);
    }
    /*
    private void storeTimeInUnix(){
        long nowLong = System.currentTimeMillis()/1000;
        int nowInt = (int)nowLong;

        ContentValues cv = new ContentValues();
        cv.put(EmotionListContract.EmotionListEntry.COLUMN_UNXSTAMP, nowInt);
        db.insert(EmotionListContract.EmotionListEntry.TABLE_NAME, null, cv);;
    }
    */


        //Modified DoRequest Class

    class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        private Exception e = null;
        private OnTaskCompleted listen;


        public doRequest(OnTaskCompleted listener ){
            this.listen = listener;

        }



        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            try {
                return processWithAutoFaceDetection();
            } catch (Exception e) {
                this.e = e;
                return null;
            }
        }

        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            if (this.e != null) {
                Toast.makeText(getApplicationContext(),"Error" +  e.getMessage(),Toast.LENGTH_LONG).show();

                this.e = null;
            } else if (result.size() == 0) {
                Toast.makeText(getApplicationContext(),"No emotion detected",Toast.LENGTH_LONG).show();

            } else {
                listen.onTaskCompleted(result);

                /*for (RecognizeResult r : result) {
                    Toast.makeText(getApplicationContext(),"Emotion detect",Toast.LENGTH_LONG).show();


                    addEmotion(r.scores.anger, r.scores.contempt, r.scores.disgust, r.scores.fear, r.scores.happiness, r.scores.neutral, r.scores.sadness, r.scores.surprise);
                    HealthPictureService2.this.counter+=1 ;
                }*/
            }



            /*


            if (this.e != null) {
                Toast.makeText(getApplicationContext(),"Error" +  e.getMessage(),Toast.LENGTH_LONG).show();

                this.e = null;
            } else if (result.size() == 0) {
                Toast.makeText(getApplicationContext(),"No emotion detect",Toast.LENGTH_LONG).show();

            } else {

                for (RecognizeResult r : result) {
                    Toast.makeText(getApplicationContext(),"Emotion detect",Toast.LENGTH_LONG).show();


                    addEmotion(r.scores.anger, r.scores.contempt, r.scores.disgust, r.scores.fear, r.scores.happiness, r.scores.neutral, r.scores.sadness, r.scores.surprise);
                    HealthPictureService2.this.counter+=1 ;
                }
            }


            if (this.e != null) {
                Toast.makeText(getApplicationContext(),"Error" +  e.getMessage(),Toast.LENGTH_LONG).show();

                this.e = null;
            }else{
                listen.onTaskCompleted(result);
            }

            */


        }
    }


}


