package me.mariotti.tanker;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import me.mariotti.opencv.TargetSearch;
import me.mariotti.tanker.messaging.Communicator;
import me.mariotti.voice.VoiceActivity;
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


public class TankActivity extends VoiceActivity implements CvCameraViewListener {
    private static final int VOICE_COLOR = 1;
    private final String TAG = "TankActivity";
    public static boolean DEBUG = false;
    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier mFaceCascadeClassifier;
    public Communicator mCommunicator;
    public AdkManager mArduino;
    private TargetSearch mTargetSearch;
    public TankLogic mTankLogic;
    private boolean colorChoosen = false;
    private boolean colorSchema = false;
    private Switch mColorSchemaSwitch;
    SeekBar hue, saturation, value;
    private TextView text_currentHue, text_currentSaturation, text_currentValue;
    private SeekBar.OnSeekBarChangeListener seekBarListiner;
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
        hue = (SeekBar) findViewById(R.id.seekBarHue);
        saturation = (SeekBar) findViewById(R.id.seekBarSaturation);
        value = (SeekBar) findViewById(R.id.seekBarValue);
        text_currentHue = (TextView) findViewById(R.id.text_currentHue);
        text_currentSaturation = (TextView) findViewById(R.id.text_currentSaturation);
        text_currentValue = (TextView) findViewById(R.id.text_currentValue);
        mArduino = new AdkManager((UsbManager) getSystemService(Context.USB_SERVICE));

//        mCommunicator = new Communicator(this);
//        mTankLogic=new TankLogic(mCommunicator,this);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraPreview);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.getHolder().setFixedSize(960, 540);
        mColorSchemaSwitch = (Switch) findViewById(R.id.switch1);
        mTargetSearch = new TargetSearch(this);
        seekBarListiner = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress / 10;
                if (seekBar == hue) {
                    text_currentHue.setText(String.valueOf(progress));
                }
                if (seekBar == saturation) {
                    text_currentSaturation.setText(String.valueOf(progress));
                }
                if (seekBar == value) {
                    text_currentValue.setText(String.valueOf(progress));
                }
                if (colorSchema) {
                    mTargetSearch.setTargetHSVColor(hue.getProgress() / 10, saturation.getProgress() / 10, value.getProgress() / 10);
                } else {
                    mTargetSearch.setTargetRGBColor(hue.getProgress() / 10, saturation.getProgress() / 10, value.getProgress() / 10);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        hue.setOnSeekBarChangeListener(seekBarListiner);
        saturation.setOnSeekBarChangeListener(seekBarListiner);
        value.setOnSeekBarChangeListener(seekBarListiner);
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
        Core.flip(aInputFrame, aInputFrame, -1);
//        return mTargetSearch.searchFaces(aInputFrame, mFaceCascadeClassifier);
//        return mTargetSearch.searchContour(aInputFrame);
        return mTargetSearch.searchColours(aInputFrame);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCommunicator.setKeepAlive(false);
        //mArduino.close();
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
        restoreTargetAnalysisAndCommunication();
        if (!colorChoosen) {
            //listen(VOICE_COLOR); TODO Uncomment
        }
    }

    void restoreTargetAnalysisAndCommunication() {
        mArduino.open();

        //let communicator's AsyncTask ends and clear the reference to it. !=null check is for avoid NullPointException on
        //first run
        if (mCommunicator != null) {
            mCommunicator.setKeepAlive(false);
        }
        mCommunicator = new Communicator(this);
        mTankLogic = new TankLogic(mCommunicator, this);
    }

    @Override
    public void recognitionResults(int requestCode, String bestMatch) {
        if (requestCode == VOICE_COLOR) {
            if (bestMatch.contains("blu")) {
                mTargetSearch.setTargetColorToBlue();
            } else if (bestMatch.contains("rosso")) {
                mTargetSearch.setTargetColorToRed();
            } else if (bestMatch.contains("giallo")) {
                mTargetSearch.setTargetColorToYellow();
            } else if (bestMatch.contains("verde")) {
                mTargetSearch.setTargetColorToGreen();
            } else {//try until we get a color
                listen(VOICE_COLOR);
            }
        }
        colorChoosen = true;
        Log.w(TAG, bestMatch);
    }

    public void toggleDebug(View w) {
        DEBUG = !DEBUG;
    }

    public void switchColorSchema(View v) {
        colorSchema = !colorSchema;
        if (colorSchema) {
            hue.setMax(3600);
            saturation.setMax(1000);
            value.setMax(1000);
        } else {
            hue.setMax(2550);
            saturation.setMax(2550);
            value.setMax(2550);
        }


    }

}