package me.mariotti.udoomobiletanker;

/**
 * Created by simone on 28/10/14.
 */

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import me.mariotti.opencv.TargetSearch;
import me.palazzetti.adktoolkit.AdkManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class TankActivity extends Activity implements CvCameraViewListener {

    private final String TAG = "TankActivity";
    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier faceCascadeClassifier;
    public Communicator mCommunicator;
    public AdkManager mArduino;
    private Mat grayscaleImage;
    private TargetSearch mTargetSearch;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    private void initializeOpenCVDependencies() {
        try {
            // Copy the Face cascade file into a temp file so OpenCV can load it
            InputStream isf = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFileF = new File(cascadeDir, "faceDetection.xml");
            FileOutputStream osf = new FileOutputStream(mCascadeFileF);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = isf.read(buffer)) != -1) {
                osf.write(buffer, 0, bytesRead);
            }
            isf.close();
            osf.close();
            // Load the cascade classifier
            faceCascadeClassifier = new CascadeClassifier(mCascadeFileF.getAbsolutePath());
            Log.i(TAG, "Face Cascade File loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading cascades", e);
        }
        // And we are ready to go
        openCvCameraView.enableView();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.mobile_tank);
        mArduino = new AdkManager((UsbManager) getSystemService(Context.USB_SERVICE));
        mCommunicator = new Communicator(this);
        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraPreview);
        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);
        openCvCameraView.getHolder().setFixedSize(960, 540);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mTargetSearch.setGrayscaleImage(new Mat(height, width, CvType.CV_8UC1));
        // The faces will be about a 20% of the height of the screen
        mTargetSearch.setAbsoluteFaceSize((int) (height * 0.2));
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        return mTargetSearch.AnalyzeFrame(aInputFrame, faceCascadeClassifier);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCommunicator.setKeepAlive(false);
        mArduino.close();
    }

    @Override
    protected void onStart() {
        super.onStart();
        final ScrollView mScrollLog = (ScrollView) findViewById(R.id.scrollView);
        mScrollLog.postDelayed(new Runnable() {

            @Override
            public void run() {
                mScrollLog.fullScroll(ScrollView.FOCUS_DOWN);
            }
        },100);
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
        mTargetSearch = new TargetSearch(this);
        mArduino.open();
        mCommunicator.setKeepAlive(true);
        mCommunicator.execute();
    }
}