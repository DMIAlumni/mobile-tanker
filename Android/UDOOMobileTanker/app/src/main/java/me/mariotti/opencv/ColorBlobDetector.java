package me.mariotti.opencv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ColorBlobDetector {
    private static final String TAG = "ColorBlobDetector";
    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mLowerBound = new Scalar(0);
    private Scalar mUpperBound = new Scalar(0);
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.1;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(25, 50, 50, 0);

    private Mat mSpectrum = new Mat();
    private List<MatOfPoint> mContours = new ArrayList<>();

    // Cache
    Mat mPyrDownMat = new Mat();
    Mat mHsvMat = new Mat();
    Mat mMask = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();



    public static Scalar convertScalarHsv2Rgba(Scalar mHsvColor) {
        Mat mPointMatRgba = new Mat();
        Mat mPointMatHsv = new Mat(1, 1, CvType.CV_8UC3, mHsvColor);
        Imgproc.cvtColor(mPointMatHsv, mPointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);
        Log.i(TAG, "Source HSV color: (" + mHsvColor.val[0] + ", " + mHsvColor.val[1] +
                  ", " + mHsvColor.val[2] + ", " + mHsvColor.val[3] + ")");
        Log.i(TAG, "Converted RGBA color: (" + (new Scalar(mPointMatRgba.get(0, 0))).val[0] +
                   ", " + (new Scalar(mPointMatRgba.get(0, 0))).val[1] + ", " + (new Scalar(mPointMatRgba.get(0, 0))).val[2] + ", " + (new Scalar(mPointMatRgba.get(0, 0))).val[3] + ")");
        return new Scalar(mPointMatRgba.get(0, 0));
    }

    public Scalar convertScalarRgba2Hsv(Scalar rgbaColor) {
        Mat mPointMatHsv = new Mat();
        Mat mPointMatRgba = new Mat(1, 1, CvType.CV_8UC3, rgbaColor);
        Imgproc.cvtColor(mPointMatRgba, mPointMatHsv, Imgproc.COLOR_RGB2HSV_FULL);
        return new Scalar(mPointMatHsv.get(0, 0));
    }

    public void setHsvColor(Scalar mHsvColor) {
        double mMinH = (mHsvColor.val[0] >= mColorRadius.val[0]) ? mHsvColor.val[0] - mColorRadius.val[0] : 0;
        double mMaxH = (mHsvColor.val[0] + mColorRadius.val[0] <= 360) ? mHsvColor.val[0] + mColorRadius.val[0] : 360;

        mLowerBound.val[0] = mMinH;
        mUpperBound.val[0] = mMaxH;
        mLowerBound.val[1] = mHsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = mHsvColor.val[1] + mColorRadius.val[1];
        mLowerBound.val[2] = mHsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = mHsvColor.val[2] + mColorRadius.val[2];
        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat mSpectrumHsv = new Mat(1, (int) (mMaxH - mMinH), CvType.CV_8UC3);

        for (int j = 0; j < mMaxH - mMinH; j++) {
            byte[] tmp = {(byte) (mMinH + j), (byte) mHsvColor.val[1], (byte) mHsvColor.val[2]};
            mSpectrumHsv.put(0, j, tmp);
        }


        Imgproc.cvtColor(mSpectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
    }
    public void process(Mat rgbaImage) {
        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
        Imgproc.dilate(mMask, mDilatedMask, new Mat());

        List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mDilatedMask, mContours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double mMaxArea = 0;
        Iterator<MatOfPoint> mEach = mContours.iterator();
        while (mEach.hasNext()) {
            MatOfPoint mWrapper = mEach.next();
            double mArea = Imgproc.contourArea(mWrapper);
            if (mArea > mMaxArea)
                mMaxArea = mArea;
        }

        // Filter contours by area and resize to fit the original image size
        this.mContours.clear();
        mEach = mContours.iterator();
        while (mEach.hasNext()) {
            MatOfPoint mContour = mEach.next();
            if (Imgproc.contourArea(mContour) > mMinContourArea * mMaxArea) {
                Core.multiply(mContour, new Scalar(4, 4), mContour);
                this.mContours.add(mContour);
            }
        }
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }

    public Mat getSpectrum() {
        return mSpectrum;
    }

    public void setMinContourArea(double mArea) {
        mMinContourArea = mArea;
    }

    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    }
}
