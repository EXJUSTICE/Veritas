package com.xu.servicequalityrater.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import com.xu.servicequalityrater.listeners.OnPictureCapturedListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;

//https://github.com/hzitoun/android-camera2-secret-picture-taker/blob/master/app/src/main/java/com/hzitoun/camera2SecretPictureTaker/services/PictureService.java
//Actually no longer used
@TargetApi(21)
public class PictureService {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final String TAG = "PictureService";
    private CameraDevice cameraDevice;
    private Queue<String> cameraIds;
    private final CaptureCallback captureListener = new CaptureCallback() {
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (PictureService.this.picturesTaken.lastEntry() != null) {
                PictureService.this.capturedListener.onCaptureDone((String) PictureService.this.picturesTaken.lastEntry().getKey(), (byte[]) PictureService.this.picturesTaken.lastEntry().getValue());
                Log.i(PictureService.TAG, "done taking picture from camera " + PictureService.this.cameraDevice.getId());
            }
            PictureService.this.closeCamera();
        }
    };
    private OnPictureCapturedListener capturedListener;
    private Activity context;
    private String currentCameraId;
    private ImageReader imageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CameraManager manager;
    private TreeMap<String, byte[]> picturesTaken;
    private final StateCallback stateCallback = new StateCallback() {
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(PictureService.TAG, "camera " + camera.getId() + " opened");
            PictureService.this.cameraDevice = camera;
            Log.i(PictureService.TAG, "Taking picture from camera " + camera.getId());
            PictureService.this.takePicture();
        }

        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(PictureService.TAG, " camera " + camera.getId() + " disconnected");
            if (PictureService.this.cameraDevice != null) {
                PictureService.this.cameraDevice.close();
            }
        }

        public void onClosed(@NonNull CameraDevice camera) {
            Log.d(PictureService.TAG, "camera " + camera.getId() + " closed");
            PictureService.this.stopBackgroundThread();
            if (PictureService.this.cameraIds.isEmpty()) {
                PictureService.this.capturedListener.onDoneCapturingAllPhotos(PictureService.this.picturesTaken);
            } else {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        PictureService.this.takeAnotherPicture();
            }
        }, 100);
            }
        }

        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(PictureService.TAG, "camera in error, int code " + error);
            if (PictureService.this.cameraDevice != null) {
                PictureService.this.cameraDevice.close();
            } else {
                PictureService.this.cameraDevice = null;
            }
        }
    };
    private WindowManager windowManager;

    static {
        ORIENTATIONS.append(0, 90);
        ORIENTATIONS.append(1, 0);
        ORIENTATIONS.append(2, 270);
        ORIENTATIONS.append(3, 180);
    }

    public void startCapturing(Activity activity, OnPictureCapturedListener capturedListener) {
        this.picturesTaken = new TreeMap();
        this.context = activity;
        this.manager = (CameraManager) this.context.getSystemService("camera");
        this.windowManager = this.context.getWindowManager();
        this.capturedListener = capturedListener;
        this.cameraIds = new LinkedList();
        try {
            String[] cameraIdList = this.manager.getCameraIdList();
            if (cameraIdList == null || cameraIdList.length == 0) {
                capturedListener.onDoneCapturingAllPhotos(this.picturesTaken);
                return;
            }
            for (String cameraId : cameraIdList) {
                this.cameraIds.add(cameraId);
            }
            this.currentCameraId = (String) this.cameraIds.poll();
            openCameraAndTakePicture();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCameraAndTakePicture() {
        startBackgroundThread();
        Log.d(TAG, "opening camera " + this.currentCameraId);
        try {
            if (ActivityCompat.checkSelfPermission(this.context, "android.permission.CAMERA") == 0 && ActivityCompat.checkSelfPermission(this.context, "android.permission.WRITE_EXTERNAL_STORAGE") == 0) {
                this.manager.openCamera(this.currentCameraId, this.stateCallback, null);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, " exception opening camera " + this.currentCameraId + e.getMessage());
        }
    }

    private void takePicture() {
        if (this.cameraDevice == null) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        try {
            CameraCharacteristics characteristics = this.manager.getCameraCharacteristics(this.cameraDevice.getId());
            Size[] jpegSizes = null;
            if (!(characteristics == null || characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) == null)) {
                jpegSizes = ((StreamConfigurationMap) characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(256);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, 256, 1);
            List<Surface> outputSurfaces = new ArrayList(2);
            outputSurfaces.add(reader.getSurface());
            final Builder captureBuilder = this.cameraDevice.createCaptureRequest(2);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(ORIENTATIONS.get(this.windowManager.getDefaultDisplay().getRotation())));
            reader.setOnImageAvailableListener(new OnImageAvailableListener() {
                public void onImageAvailable(ImageReader readerL) {
                    Image image = readerL.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    PictureService.this.saveImageToDisk(bytes);
                    if (image != null) {
                        image.close();
                    }
                }
            }, this.mBackgroundHandler);
            this.cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), PictureService.this.captureListener, PictureService.this.mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, this.mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void saveImageToDisk(byte[] bytes) {
        final File file = new File(Environment.getExternalStorageDirectory() + "/" + this.cameraDevice.getId() + "_pic.jpg");
        try (final OutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            this.picturesTaken.put(file.getPath(), bytes);
        } catch (IOException e) {
            Log.e(TAG, "Exception occurred while saving picture to external storage ", e);
        }
    }

    private void startBackgroundThread() {
        if (this.mBackgroundThread == null) {
            this.mBackgroundThread = new HandlerThread("Camera Background" + this.currentCameraId);
            this.mBackgroundThread.start();
            this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        this.mBackgroundThread.quitSafely();
        try {
            this.mBackgroundThread.join();
            this.mBackgroundThread = null;
            this.mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "exception stopBackgroundThread" + e.getMessage());
        }
    }

    private void takeAnotherPicture() {
        startBackgroundThread();
        this.currentCameraId = (String) this.cameraIds.poll();
        openCameraAndTakePicture();
    }

    private void closeCamera() {
        Log.d(TAG, "closing camera " + this.cameraDevice.getId());
        if (this.cameraDevice != null) {
            this.cameraDevice.close();
            this.cameraDevice = null;
        }
        if (this.imageReader != null) {
            this.imageReader.close();
            this.imageReader = null;
        }
    }
}
