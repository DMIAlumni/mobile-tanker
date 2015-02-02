package me.mariotti.tanker;

import android.app.Activity;
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
import android.widget.ToggleButton;
import me.mariotti.opencv.ColorBlobDetector;
import me.mariotti.opencv.TargetSearch;
import me.mariotti.tanker.messaging.Communicator;
import me.palazzetti.adktoolkit.AdkManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;


public class RobotActivity extends Activity implements CvCameraViewListener, View.OnTouchListener {
    private final String TAG = "RobotActivity";
    public static boolean DEBUG = false;
    private CameraBridgeViewBase mOpenCvCameraView;
    public Communicator mCommunicator;
    public AdkManager mArduino;
    private TargetSearch mTargetSearch;
    public RobotLogic mRobotLogic;
    private boolean mIsColorChosen = false;
    private SeekBar mHue, mSaturation, mValue;
    private Mat mInputFrame;
    private boolean mGo = false;
    private ToggleButton mButtonGo;
    private TextView mTextCurrentHue, mTextCurrentSaturation, mTextCurrentValue;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCV();
                    mOpenCvCameraView.setOnTouchListener(RobotActivity.this);
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.mobile_tank);
        mHue = (SeekBar) findViewById(R.id.seekBarHue);
        mSaturation = (SeekBar) findViewById(R.id.seekBarSaturation);
        mValue = (SeekBar) findViewById(R.id.seekBarValue);
        mTextCurrentHue = (TextView) findViewById(R.id.text_currentHue);
        mTextCurrentSaturation = (TextView) findViewById(R.id.text_currentSaturation);
        mTextCurrentValue = (TextView) findViewById(R.id.text_currentValue);
        mButtonGo = (ToggleButton) findViewById(R.id.goButton);
        mArduino = new AdkManager((UsbManager) getSystemService(Context.USB_SERVICE));
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraPreview);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.getHolder().setFixedSize(320, 240);
        mTargetSearch = new TargetSearch(this);

        SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress / 10;
                if (seekBar == mHue) {
                    mTextCurrentHue.setText(String.valueOf(progress) + "°");
                }
                if (seekBar == mSaturation) {
                    mTextCurrentSaturation.setText(String.valueOf(progress) + "%");
                }
                if (seekBar == mValue) {
                    mTextCurrentValue.setText(String.valueOf(progress) + "%");
                }
                //Convert the seekbars range (360*10 for H and 100*10 for S,V) to OpenCV value for HSV (H: 0-255, S: 0-255, V: 0-255)
                mTargetSearch.setTargetHSVColor(mHue.getProgress() / 10 * 255 / 360,
                                                mSaturation.getProgress() / 10 * 255 / 100,
                                                mValue.getProgress() / 10 * 255 / 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        mHue.setOnSeekBarChangeListener(seekBarListener);
        mSaturation.setOnSeekBarChangeListener(seekBarListener);
        mValue.setOnSeekBarChangeListener(seekBarListener);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mTargetSearch.setGrayscaleImage(new Mat(height, width, CvType.CV_8UC1));
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        //flip horizontally and vertically due to camera physical position
        Core.flip(aInputFrame, aInputFrame, -1);
        mInputFrame = aInputFrame;
        return mIsColorChosen ? mTargetSearch.searchColours(aInputFrame) : aInputFrame;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCommunicator.setKeepAlive(false);
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
        if (!mIsColorChosen) {
            //listen(VOICE_COLOR); TODO Uncomment
        }
    }

    public boolean canGo() {
        return mGo;
    }

    public void reset(){
        mIsColorChosen =false;
        mGo = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mButtonGo.setChecked(false);
            }
        });
    }

    void restoreTargetAnalysisAndCommunication() {
        mArduino.open();
        //let communicator's AsyncTask ends and clear the reference to it. !=null check is for avoid NullPointException on
        //first run
        if (mCommunicator != null) {
            mCommunicator.setKeepAlive(false);
        }
        mCommunicator = new Communicator(this);
        mRobotLogic = new RobotLogic(mCommunicator, this);
    }



    public void toggleDebug(View w) {
        DEBUG = !DEBUG;
    }

    public void toggleGo(View w) {
        mGo = !mGo;
    }

    private void initializeOpenCV() {
        mOpenCvCameraView.enableView();
    }


    public boolean onTouch(View v, MotionEvent event) {
        Mat mTouchedRegionHsv = new Mat();
        int cols = mInputFrame.cols();
        int rows = mInputFrame.rows();


        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect mTouchedRect = new Rect();

        mTouchedRect.x = (x > 4) ? x - 4 : 0;
        mTouchedRect.y = (y > 4) ? y - 4 : 0;

        mTouchedRect.width = (x + 4 < cols) ? x + 4 - mTouchedRect.x : cols - mTouchedRect.x;
        mTouchedRect.height = (y + 4 < rows) ? y + 4 - mTouchedRect.y : rows - mTouchedRect.y;

        Mat mTouchedRegionRgba = mInputFrame.submat(mTouchedRect);

        Imgproc.cvtColor(mTouchedRegionRgba, mTouchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        Scalar mBlobColorHsv = Core.sumElems(mTouchedRegionHsv);
        int mPointCount = mTouchedRect.width * mTouchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= mPointCount;

        Scalar mBlobColorRgba = ColorBlobDetector.convertScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched RGBA color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] + ", "
                   + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
        Log.i(TAG, "Touched HSV color: (" + mBlobColorHsv.val[0] + ", " + mBlobColorHsv.val[1] +
                   ", " + mBlobColorHsv.val[2] + ", " + mBlobColorHsv.val[3] + ")");

        mTargetSearch.setTargetHSVColor(mBlobColorHsv);
        mHue.setProgress((int) mBlobColorHsv.val[0] * 10 * 360 / 255);
        mSaturation.setProgress((int) mBlobColorHsv.val[1] * 10 * 100 / 255);
        mValue.setProgress((int) mBlobColorHsv.val[2] * 10 * 100 / 255);

        mIsColorChosen = true;

        mTouchedRegionRgba.release();
        mTouchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }
}