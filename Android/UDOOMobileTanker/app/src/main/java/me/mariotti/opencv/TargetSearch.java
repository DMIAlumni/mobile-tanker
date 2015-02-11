package me.mariotti.opencv;


import android.util.Log;
import me.mariotti.ai.BaseAi;
import me.mariotti.logic.RobotActivity;
import me.mariotti.logic.UpdateDirections;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;


public class TargetSearch {
    private final String TAG = "TargetSearch";
    private RobotActivity mRobotActivity;
    private BaseAi mRobotLogic;
    private Mat mGrayscaleImage;
    private Rect mTarget;
    private Scalar mTargetColorRgba = new Scalar(0,0,0,0);
    private Scalar mTargetColorHsv = new Scalar(0,0,0,0);
    private static final Scalar RED = new Scalar(255, 0, 0);
    private static final Scalar GREEN = new Scalar(0, 255, 0);
    private static final Scalar BLUE = new Scalar(0, 0, 255);
    private Rect mNullTarget;


    public TargetSearch(BaseAi robotLogic, RobotActivity mRobotActivity) {
        this.mRobotActivity = mRobotActivity;
        mRobotLogic = robotLogic;
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
        UpdateDirections directionsUpdater = UpdateDirections.getInstance(mRobotActivity);

        if (mTarget != mNullTarget) {
            directionsUpdater.show();

            if (mFrameCenter.x - (mTarget.x + mTarget.width / 2) > 0) {
                mRobotLogic.targetPosition(mRgba, mTarget, BaseAi.TARGET_POSITION_LEFT);
                directionsUpdater.left();

            } else if (mFrameCenter.x - (mTarget.x + mTarget.width / 2) < 0) {
                mRobotLogic.targetPosition(mRgba, mTarget, BaseAi.TARGET_POSITION_RIGHT);
                directionsUpdater.right();
            } else {
                mRobotLogic.targetPosition(mRgba, mTarget, BaseAi.TARGET_POSITION_FRONT);
               directionsUpdater.aimed();
            }
        } else {
            mRobotLogic.targetPosition(mRgba, mTarget, BaseAi.TARGET_POSITION_NONE);
            directionsUpdater.show();
            directionsUpdater.search();
        }

        return mRgba;
    }

    int rectArea(Rect mRect) {
        return mRect.width * mRect.height;
    }

    public void setGrayscaleImage(Mat mGrayscaleImage) {
        this.mGrayscaleImage = mGrayscaleImage;
    }

    public void setTargetHsvColor(int hue, int saturation, int value) {
        mTargetColorRgba = ColorBlobDetector.convertScalarHsv2Rgba(new Scalar(hue, saturation, value));
        mTargetColorHsv=new Scalar(hue,saturation,value);
    }
    public void setTargetHsvColor(Scalar mHsvColor) {
        mTargetColorRgba = ColorBlobDetector.convertScalarHsv2Rgba(mHsvColor);
        mTargetColorHsv=mHsvColor;
    }


}
