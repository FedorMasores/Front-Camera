package com.frontcamera.appcamerfrontmasoresasdbjvu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private Camera mCam;
    private MirrorView mCamPreview;
    private int mCameraId = 0;
    private FrameLayout mPreviewLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraId = findFirstFrontFacingCamera();
        mPreviewLayout = (FrameLayout) findViewById(R.id.camPreview);
        mPreviewLayout.removeAllViews();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 50);
        }
        startCameraInLayout(mPreviewLayout, mCameraId);
    }


    private int findFirstFrontFacingCamera() {
        int foundId = -1;
        int numCams = Camera.getNumberOfCameras();
        for (int camId = 0; camId < numCams; camId++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(camId, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                foundId = camId;
                break;
            }
        }
        return foundId;
    }

    private void startCameraInLayout(FrameLayout layout, int cameraId) {
        if (mCam != null) {
            mCam.lock();
            mCam.stopPreview();
            mCam.release();
            mCam = null;
        }
        try {
            mCam = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            if (mCam != null) {
                mCamPreview = new MirrorView(this, mCam);
                mCamPreview.setCameraDisplayOrientationAndSize();
                layout.addView(mCamPreview);
            }
        } catch (Exception e) {
        }

    }

    public class MirrorView extends SurfaceView implements
            SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public MirrorView(Context context, Camera camera) {
            super(context);
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (Exception error) {
                Log.d("DEBUG_TAG", "Error starting mPreviewLayout: " + error.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            if (mHolder.getSurface() == null) {
                return;
            }

            try {
                mCamera.stopPreview();
            } catch (Exception e) {
            }

            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception error) {
                Log.d("DEBUG_TAG", "Error starting mPreviewLayout: " + error.getMessage());
            }
        }

        public void setCameraDisplayOrientationAndSize() {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, info);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int degrees = rotation * 90;

            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;
            } else {
                result = (info.orientation - degrees + 360) % 360;
            }
            mCamera.setDisplayOrientation(result);

            Camera.Size previewSize = mCam.getParameters().getPreviewSize();
            if (result == 90 || result == 270) {
                mHolder.setFixedSize(previewSize.height, previewSize.width);
            } else {
                mHolder.setFixedSize(previewSize.width, previewSize.height);

            }
        }

    }

}