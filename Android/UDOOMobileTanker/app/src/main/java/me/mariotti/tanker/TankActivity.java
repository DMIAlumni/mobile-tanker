package me.mariotti.tanker;

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
import me.mariotti.tanker.messaging.Communicator;
import me.palazzetti.adktoolkit.AdkManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class TankActivity extends Activity implements CvCameraViewListener {

    private final String TAG = "TankActivity";
    public static boolean DEBUG = false;
    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier mFaceCascadeClassifier;
    public Communicator mCommunicator;
    public AdkManager mArduino;
    private TargetSearch mTargetSearch;
    public TankLogic mTankLogic;
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
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File mCascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFileF = new File(mCascadeDir, "faceDetection.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFileF);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            // Load the cascade classifier
            mFaceCascadeClassifier = new CascadeClassifier(mCascadeFileF.getAbsolutePath());
            Log.i(TAG, "Face Cascade File loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading cascades", e);
        }
        // And we are ready to go
        mOpenCvCameraView.enableView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.mobile_tank);
        mArduino = new AdkManager((UsbManager) getSystemService(Context.USB_SERVICE));

        mCommunicator = new Communicator(this);
        mTankLogic=new TankLogic(mCommunicator);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraPreview);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.getHolder().setFixedSize(960, 540);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mTargetSearch.setmGrayscaleImage(new Mat(height, width, CvType.CV_8UC1));
        // The faces will be about a 20% of the height of the screen
        mTargetSearch.setAbsoluteFaceSize((int) (height * 0.2));
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        //flip horizontally and vertically due to camera physical position
        Core.flip(aInputFrame,aInputFrame,-1);
        return mTargetSearch.searchFaces(aInputFrame, mFaceCascadeClassifier);
        //return mTargetSearch.searchContour(aInputFrame);
        //return mTargetSearch.searchColours(aInputFrame);
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
        mScrollLog.post(new Runnable() {
            public void run() {
                mScrollLog.scrollTo(0, mScrollLog.getBottom());
            }
        });
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

    public void toggleDebug(View w) {
        DEBUG=!DEBUG;
    }
}