package com.xu.servicequalityrater;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements Callback {
    public Camera mCamera;
    public SurfaceHolder mSurfaceHolder;

    public void startCameraPreview()
    {
        try{
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public CameraPreview(Context context, Camera camera) {
        super(context);
        this.mCamera = camera;
        this.mSurfaceHolder = getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mSurfaceHolder.setType(3);
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            //TODO we need this here too because on SurfaceCreated we always need to open the camera, in case its released

            this.mCamera.setPreviewDisplay(surfaceHolder);
            this.mCamera.setDisplayOrientation(90);
            //this.mCamera.startPreview();
        } catch (IOException e) {
        }
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

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {



    }

    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        try {
            this.mCamera.setPreviewDisplay(surfaceHolder);
            this.mCamera.startPreview();
        } catch (Exception e) {
        }
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        double targetRatio = ((double) w) / ((double) h);
        if (sizes == null) {
            return null;
        }
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        for (Size size : sizes) {
            if (Math.abs((((double) size.width) / ((double) size.height)) - targetRatio) <= 0.05d && ((double) Math.abs(size.height - targetHeight)) < minDiff) {
                optimalSize = size;
                minDiff = (double) Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize != null) {
            return optimalSize;
        }
        minDiff = Double.MAX_VALUE;
        for (Size size2 : sizes) {
            if (((double) Math.abs(size2.height - targetHeight)) < minDiff) {
                optimalSize = size2;
                minDiff = (double) Math.abs(size2.height - targetHeight);
            }
        }
        return optimalSize;
    }
}
