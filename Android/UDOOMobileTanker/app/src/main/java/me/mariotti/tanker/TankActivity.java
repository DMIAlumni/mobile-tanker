package me.mariotti.tanker;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import me.mariotti.opencv.ColorBlobDetector;
import me.mariotti.opencv.TargetSearch;
import me.mariotti.tanker.messaging.Communicator;
import me.mariotti.voice.VoiceActivity;
import me.palazzetti.adktoolkit.AdkManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;


public class TankActivity extends VoiceActivity implements CvCameraViewListener, View.OnTouchListener {
    private static final int VOICE_COLOR = 1;
    private final String TAG = "TankActivity";
    public static boolean DEBUG = false;
    private CameraBridgeViewBase mOpenCvCameraView;
//    private CascadeClassifier mFaceCascadeClassifier;
    public Communicator mCommunicator;
    public AdkManager mArduino;
    private TargetSearch mTargetSearch;
    public TankLogic mTankLogic;
    private boolean isColorChosen = false;
    SeekBar hue, saturation, value;
    private Mat mInputFrame;

    private TextView text_currentHue, text_currentSaturation, text_currentValue;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    mOpenCvCameraView.setOnTouchListener(TankActivity.this);
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    private void initializeOpenCVDependencies() {
    /*    try {
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
        }*/
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
//        mOpenCvCameraView.getHolder().setFixedSize(960, 540);
        mOpenCvCameraView.getHolder().setFixedSize(720, 576);
        mTargetSearch = new TargetSearch(this);
        SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress / 10;
                if (seekBar == hue) {
                    text_currentHue.setText(String.valueOf(progress) + "°");
                }
                if (seekBar == saturation) {
                    text_currentSaturation.setText(String.valueOf(progress) + "%");
                }
                if (seekBar == value) {
                    text_currentValue.setText(String.valueOf(progress) + "%");
                }
                //Convert the seekbars range (360*10 for H and 100*10 for S,V) to OpenCV value for HSV (H: 0-255, S: 0-255, V: 0-255)
                mTargetSearch.setTargetHSVColor(hue.getProgress() / 10 * 255 / 360,
                                                saturation.getProgress() / 10 * 255 / 100,
                                                value.getProgress() / 10 * 255 / 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        hue.setOnSeekBarChangeListener(seekBarListener);
        saturation.setOnSeekBarChangeListener(seekBarListener);
        value.setOnSeekBarChangeListener(seekBarListener);
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
        mInputFrame = aInputFrame;
//        return mTargetSearch.searchFaces(aInputFrame, mFaceCascadeClassifier);
//        return mTargetSearch.searchContour(aInputFrame);
        return isColorChosen ? mTargetSearch.searchColours(aInputFrame) : aInputFrame;
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
        if (!isColorChosen) {
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
        isColorChosen = true;
        Log.w(TAG, bestMatch);
    }

    public void toggleDebug(View w) {
        DEBUG = !DEBUG;
    }


    public boolean onTouch(View v, MotionEvent event) {
        int cols = mInputFrame.cols();
        int rows = mInputFrame.rows();


        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mInputFrame.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        Scalar mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        Scalar mBlobColorRgba = ColorBlobDetector.convertScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched RGBA color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] + ", "
                   + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
        Log.i(TAG, "Touched HSV color: (" + mBlobColorHsv.val[0] + ", " + mBlobColorHsv.val[1] +
                   ", " + mBlobColorHsv.val[2] + ", " + mBlobColorHsv.val[3] + ")");

        mTargetSearch.setTargetHSVColor(mBlobColorHsv);
        hue.setProgress((int) mBlobColorHsv.val[0] * 10 * 360 / 255);
        saturation.setProgress((int) mBlobColorHsv.val[1] * 10 * 100 / 255);
        value.setProgress((int) mBlobColorHsv.val[2] * 10 * 100 / 255);

        isColorChosen = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }
}