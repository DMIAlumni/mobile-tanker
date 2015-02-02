package me.mariotti.opencv;


import android.util.Log;
import android.widget.ImageView;
import me.mariotti.tanker.R;
import me.mariotti.tanker.RobotActivity;
import me.mariotti.tanker.RobotLogic;
import me.mariotti.tanker.UpdateDirections;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;


public class TargetSearch {
    private final String TAG = "TargetSearch";
    private RobotActivity mRobotActivity;
    private RobotLogic mRobotLogic;
    private Mat mGrayscaleImage;
    private Rect mTarget;
    private Scalar mTargetColorRgba = new Scalar(0,0,0,0);
    private Scalar mTargetColorHsv = new Scalar(0,0,0,0);
    private static final Scalar RED = new Scalar(255, 0, 0);
    private static final Scalar GREEN = new Scalar(0, 255, 0);
    private static final Scalar BLUE = new Scalar(0, 0, 255);
    private UpdateDirections mDirectionsUpdater = null;
    private Rect mNullTarget;


    public TargetSearch(RobotActivity mRobotActivity) {
        this.mRobotActivity = mRobotActivity;
        mRobotLogic = mRobotActivity.mRobotLogic;
        ImageView mImageDirection = (ImageView) mRobotActivity.findViewById(R.id.DirectionsImageView);
        mImageDirection.setImageResource(R.drawable.tavolozza);
        mNullTarget = new Rect(0, 0, 0, 0);
    }

    public Mat searchColours(Mat mIncomingFrame) {
        Point mFrameCenter = new Point(mIncomingFrame.width() / 2, mIncomingFrame.height() / 2);
        int mMinDetectArea = 5000;
        Mat mRgba;
        ColorBlobDetector mDetector = new ColorBlobDetector();
        Mat mSpectrum = new Mat();
        Size SPECTRUM_SIZE = new Size(200, 30);
        Scalar CONTOUR_COLOR = GREEN;
        Scalar mColorRadius = new Scalar(20, 70, 70, 0);
        mDetector.setColorRadius(mColorRadius);
        mDetector.setHsvColor(mTargetColorHsv);

        mRgba = mIncomingFrame;
        mDetector.process(mRgba);

        List<MatOfPoint> mContours = mDetector.getContours();
        Log.e(TAG, "Contours count: " + mContours.size());
        Imgproc.drawContours(mRgba, mContours, -1, CONTOUR_COLOR);
        LinkedList<Rect> mRects = new LinkedList<Rect>();
        Rect mTargetRect = mNullTarget;

        for (MatOfPoint mContour : mContours) {
            Rect mTempRect = Imgproc.boundingRect(mContour);
            if (rectArea(mTempRect) >= mMinDetectArea) {
                mRects.add(mTempRect);
                Core.rectangle(mRgba, mRects.getLast().tl(), mRects.getLast().br(), BLUE, 1);
                if (rectArea(mTempRect) > rectArea(mTargetRect)) {
                    mTargetRect = mTempRect;
                }
            }
        }
        mTarget=mTargetRect;
        Core.rectangle(mRgba, mTargetRect.tl(), mTargetRect.br(), RED, 3);

        //mTargetColorRgba = ColorBlobDetector.convertScalarHsv2Rgba(new Scalar(360,255,255));
        Mat colorLabel = mRgba.submat(4, 34, 4, 34);
        colorLabel.setTo(mTargetColorRgba);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
        Mat mSpectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 38, 38 + mSpectrum.cols());
        mSpectrum.copyTo(mSpectrumLabel);
        mDirectionsUpdater = UpdateDirections.getInstance(mRobotActivity);

        if (mTarget != mNullTarget) {
            mDirectionsUpdater.show();
            //if after a resume the tank logic instance has changed update it
            mRobotLogic = mRobotLogic != mRobotActivity.mRobotLogic ? mRobotActivity.mRobotLogic : mRobotLogic;

            mRobotLogic.frameWidth(mRgba.width());
            mRobotLogic.frameHeight(mRgba.height());
            mRobotLogic.targetWidth(mTarget.width);
            mRobotLogic.targetHeight(mTarget.height);
            mRobotLogic.targetCenter(new Point(mTarget.x + mTarget.width / 2, mTarget.y + mTarget.height / 2));


            if (mFrameCenter.x - (mTarget.x + mTarget.width / 2) > 0) {
                mRobotLogic.targetPosition(RobotLogic.TARGET_POSITION_LEFT);
                mDirectionsUpdater.left();

            } else if (mFrameCenter.x - (mTarget.x + mTarget.width / 2) < 0) {
                mRobotLogic.targetPosition(RobotLogic.TARGET_POSITION_RIGHT);
                mDirectionsUpdater.right();
            } else {
                mRobotLogic.targetPosition(RobotLogic.TARGET_POSITION_FRONT);
               mDirectionsUpdater.aimed();
            }
        } else {
            mRobotLogic.targetPosition(RobotLogic.TARGET_POSITION_NONE);
            mDirectionsUpdater.show();
            mDirectionsUpdater.search();
        }

        //mDirectionsUpdater.setTarget(targetRect);
        //mRobotActivity.runOnUiThread(mDirectionsUpdater);

        return mRgba;
    }

    int rectArea(Rect mRect) {
        return mRect.width * mRect.height;
    }

    public void setGrayscaleImage(Mat mGrayscaleImage) {
        this.mGrayscaleImage = mGrayscaleImage;
    }

    public UpdateDirections getmDirectionsUpdater() {
        return mDirectionsUpdater;
    }

    public void setTargetHSVColor(int hue, int saturation, int value) {
        mTargetColorRgba = ColorBlobDetector.convertScalarHsv2Rgba(new Scalar(hue, saturation, value));
        mTargetColorHsv=new Scalar(hue,saturation,value);
    }
    public void setTargetHSVColor(Scalar mHsvColor) {
        mTargetColorRgba = ColorBlobDetector.convertScalarHsv2Rgba(mHsvColor);
        mTargetColorHsv=mHsvColor;
    }


}
