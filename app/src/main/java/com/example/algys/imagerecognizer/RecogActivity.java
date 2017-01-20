package com.example.algys.imagerecognizer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.sqrt;

public class RecogActivity extends AppCompatActivity {
    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    List<Surface> outputSurfaces;
    ImageReader reader;
    CaptureRequest.Builder captureBuilder;
    ImageReader.OnImageAvailableListener readerListener;
    CameraCaptureSession.CaptureCallback captureListener;
    CameraCaptureSession.StateCallback callback;

    private String name;
    private String author;
    private String info;
    private String url;
    private String year;
    TextView ind;
    int alpha;

    int rotation;
    byte[] buf;
    boolean flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recog);
        setTitle("Распозназание");

        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        ind = (TextView) findViewById(R.id.textView7);
        alpha = 0;
        MyTask3 indicator = new MyTask3();
        indicator.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        buf = new byte[1024];
        flag = true;

    }
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            if(flag) {
                MyTask2 task = new MyTask2();
                task.execute(textureView.getBitmap());
            }
        }
    };


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }

    };


    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected int corr(byte x){
        if(x>=0){
            return (int)x;
        }
        else {
            return (int)(256+x);
        }
    }


    class MyTask2 extends AsyncTask<Bitmap, String, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            flag = false;
        }

        @Override
        protected Boolean doInBackground(Bitmap... images) {
            Bitmap image = images[0];

            Bitmap bmp = Bitmap.createScaledBitmap(image, 480, 640, true);
            byte[] big_buf = new byte[480*640];

            for(int i=0; i<480; i++){
                for(int j=0; j<640; j++){
                    int color = bmp.getPixel(i,j);
                    byte res = (byte) sqrt(0.299*Color.red(color)*Color.red(color) + 0.587*Color.green(color)*Color.green(color) + 0.114*Color.blue(color)*Color.blue(color));
                    //     0.2126 * R + 0.7152 * G + 0.0722 * B
                    //     sqrt(0.299 * R^2 + 0.587 * G^2 + 0.114 * B^2)
                    big_buf[i + j*480] = res;
                }
            }
            try {
                Socket sock = new Socket(InetAddress.getByName("192.168.1.6"), 8081);
                OutputStream out = sock.getOutputStream();
                InputStream in = sock.getInputStream();

                out.write(big_buf);

                if(in.read(buf,0,256)<=1) {
                    return false;
                }
                int len = corr(buf[0])<<24 | corr(buf[1])<<16 | corr(buf[2])<<8 | corr(buf[3]);
                String author = new String(buf, 4, len);

                in.read(buf,0,1024);
                len = corr(buf[0])<<24 | corr(buf[1])<<16 | corr(buf[2])<<8 | corr(buf[3]);

                String info = new String(buf, 4, len);

                in.read(buf,0,256);
                len = corr(buf[0])<<24 | corr(buf[1])<<16 | corr(buf[2])<<8 | corr(buf[3]);
                String name = new String(buf, 4, len);

                in.read(buf,0,256);
                len = corr(buf[0])<<24 | corr(buf[1])<<16 | corr(buf[2])<<8 | corr(buf[3]);
                String path = new String(buf, 4, len);

                in.read(buf,0,256);
                len = corr(buf[0])<<24 | corr(buf[1])<<16 | corr(buf[2])<<8 | corr(buf[3]);
                String year = new String(buf, 4, len);
                publishProgress(author, info, name, path, year);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        protected void onProgressUpdate(String... progress) {
            author = progress[0];
            info = progress[1];
            name = progress[2];
            url = progress[3];
            year = progress[4];
        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(result == true){
                Log.e(TAG, "OK !");
                ChangeActivity();
            }
            else{
                flag = true;
                Log.e(TAG, "BAD :(");
            }
        }
    }

    class MyTask3 extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            while(true) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                publishProgress();
            }
        }

        protected void onProgressUpdate(Void... voids) {
            alpha = (alpha + 5)%511;
            if(alpha>255) {
                ind.setTextColor(ColorStateList.valueOf(Color.argb(510-alpha, 0, 0, 0)));
            } else {
                ind.setTextColor(ColorStateList.valueOf(Color.argb(alpha, 0, 0, 0)));
            }
        }

        protected void onPostExecute(Void voids){
        }
    }

    protected void ChangeActivity(){
        Intent intent = new Intent(this, InfoActivity.class);
        intent.putExtra("author", author);
        intent.putExtra("name", name);
        intent.putExtra("year", year);
        intent.putExtra("url", url);
        intent.putExtra("info", info);

        startActivity(intent);

    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(RecogActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(RecogActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(RecogActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        flag = true;
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}