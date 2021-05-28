package com.frontcamera.appcamerfrontmasoresasdbjvu;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private ImageView btnSwipe;
    private int i = 0;
    private Camera mCam;
    private MirrorView mCamPreview;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private FrameLayout mPreviewLayout;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 50) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                if (permission.equals(Manifest.permission.CAMERA)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        startCameraInLayout(mPreviewLayout, mCameraId);
                    } else {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 50);
                    }
                }
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSwipe = findViewById(R.id.btn_swipe);

        mCameraId = findFirstFrontFacingCamera();
        mPreviewLayout = (FrameLayout) findViewById(R.id.camPreview);
        mPreviewLayout.removeAllViews();

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 50);
        onRequestPermissionsResult(50, new String[] {Manifest.permission.CAMERA}, new int[]{50});

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 50);
//        }
//
//        startCameraInLayout(mPreviewLayout, mCameraId);

        btnSwipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (i != 0) {
                    startCameraInLayout(mPreviewLayout, mCameraId);
                    i = 0;
                } else {
                    startCameraInLayout(mPreviewLayout, Camera.CameraInfo.CAMERA_FACING_BACK);
                    i = 1;
                }
            }
        });
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
            mCam = Camera.open(cameraId);
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