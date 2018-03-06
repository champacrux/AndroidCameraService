package com.acruxtek.cameratest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraService extends Service {
    public static final String EXTRA_CAM_INDEX = "camera_index";
    private static final String TAG = "CameraService";
    private volatile boolean isRunning = false;

    private int camIndex = -1;

    private volatile int framesCount = 10;
    private volatile boolean pitureCommandExecuted = false;

    private Camera mCamera;

    public CameraService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Staring camera service");

        Bundle extra = intent.getExtras();
        if (extra != null && extra.containsKey(EXTRA_CAM_INDEX)) {
            camIndex = extra.getInt(EXTRA_CAM_INDEX);
        }

        if (!isRunning) {
            takePhoto();
        }

        AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int streamType = AudioManager.STREAM_SYSTEM;
        mgr.setStreamSolo(streamType, true);
        mgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        mgr.setStreamMute(streamType, true);
        mgr.setStreamMute(AudioManager.STREAM_MUSIC, true);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void takePhoto() {

        isRunning = true;

        System.out.println("Preparing to take photo");

        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        SystemClock.sleep(1000);

        if (camIndex != -1) {
            mCamera = Camera.open(camIndex);
        } else {
            mCamera = Camera.open();
        }
        try {
            if (null == mCamera) {
                System.out.println("Could not get camera instance");
            } else {
                System.out.println("Got the camera, creating the dummy surface texture");
                //SurfaceTexture dummySurfaceTextureF = new SurfaceTexture(0);
                try {

                    // set preview size to maximum
                    Camera.Parameters params = mCamera.getParameters();
                    List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
//                    Camera.Size prevSize = getBigSize(supportedPreviewSizes);
                    Camera.Size prevSize = getOptimalSize(params, 640, 480);
                    params.setPreviewSize(prevSize.width, prevSize.height);

                    List<Camera.Size> supportedPictureSizes = params.getSupportedPictureSizes();
                    Camera.Size picSize = getBigSize(supportedPictureSizes);
                    params.setPictureSize(picSize.width, picSize.height);

                    mCamera.setParameters(params);
                    mCamera.enableShutterSound(false);

                    //camera.setPreviewTexture(dummySurfaceTextureF);
                    SurfaceTexture sf = new SurfaceTexture(0);
                    sf.setDefaultBufferSize(prevSize.width, prevSize.height);
                    mCamera.setPreviewTexture(new SurfaceTexture(0));
                    //<editor-fold desc="PreviewCallback for camera">
                    mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] bytes, Camera camera) {

                            Camera.Size previewSize = camera.getParameters().getPreviewSize();

                            if (framesCount > 0) {
                                framesCount--;
                            }
                            if (framesCount <= 0 && !pitureCommandExecuted) {
                                writeBytesToFile(bytes, previewSize);
                            }

                            Log.d(TAG, "onPreviewFrame: frameCount: " + framesCount);
                        }
                    });
                    //</editor-fold>
                    mCamera.startPreview();
                } catch (Exception e) {
                    System.out.println("Could not set the surface preview texture");
                    e.printStackTrace();
                }

                Thread.sleep(1000);
                //<editor-fold desc="Camera take picture call back">
//                mCamera.takePicture(null, null, new Camera.PictureCallback() {
//
//                    @Override
//                    public void onPictureTaken(byte[] data, Camera camera) {
//                        File pictureFileDir = getDirectory();
//                        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
//                            return;
//                        }
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
//                        String date = dateFormat.format(new Date());
//                        String photoFile = "PictureFront_" + camIndex + "_" + date + ".jpg";
//                        String filename = pictureFileDir.getPath() + File.separator + photoFile;
//                        File mainPicture = new File(filename);
//
//                        try {
//                            FileOutputStream fos = new FileOutputStream(mainPicture);
//                            fos.write(data);
//                            fos.close();
//                            System.out.println("image saved");
//                        } catch (Exception error) {
//                            System.out.println("Image could not be saved");
//                        }
//                        camera.release();
//                        isRunning = false;
//                        stopSelf();
//                    }
//                });
                //</editor-fold>
            }
        } catch (Exception e) {
            e.printStackTrace();
            mCamera.release();
        }
    }

    private void writeBytesToFile(byte[] data, Camera.Size previewSize) {
        pitureCommandExecuted = true;
        File pictureFileDir = getDirectory();
        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
            return;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "PictureFront_" + camIndex + "_" + date + ".jpg";
        String filename = pictureFileDir.getPath() + File.separator + photoFile;
        File mainPicture = new File(filename);

        try {
            int width = previewSize.width;
            int height = previewSize.height;
            FileOutputStream fos = new FileOutputStream(mainPicture);
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, fos);
//            fos.close();
//            fos.write(data);
            fos.close();
            System.out.println("image saved");
        } catch (Exception error) {
            System.out.println("Image could not be saved");
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: stopping service");
        isRunning = false;
        mCamera.release();

        AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int streamType = AudioManager.STREAM_SYSTEM;
        mgr.setStreamSolo(streamType, false);
        mgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        mgr.setStreamMute(streamType, false);
        mgr.setStreamMute(AudioManager.STREAM_MUSIC, false);
    }

    private Camera.Size getBigSize(List<Camera.Size> sizes) {
        Camera.Size big = sizes.get(0);

        for (Camera.Size s : sizes) {
            if (s.height > big.height || s.width > big.width) {
                big = s;
            }
        }

        return big;
    }

    private File getDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory(), "camera-test");
        if (!directory.exists()) {
            directory.mkdir();
        }
        return directory;
    }

    private Camera.Size getOptimalSize(Camera.Parameters params, int w, int h) {

        List<Camera.Size> sizes = params.getSupportedPreviewSizes();

        final double ASPECT_TOLERANCE = 0.2;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}
