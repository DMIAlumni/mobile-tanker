package me.mariotti.opencv;


import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import me.mariotti.tanker.TankLogic;
import me.mariotti.tanker.R;
import me.mariotti.tanker.TankActivity;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class TargetSearch {
    // Color try


    //
    private final String TAG = "TargetSearch";
    private TankActivity mTankActivity;
    private TankLogic mTankLogic;
    private Mat mGrayscaleImage;
    private int absoluteFaceSize, centerCrossSize = 20;
    private ImageView mImageDirection;
    private TextView mTextDirection;
    private Rect mTarget;
    private static final Scalar RED = new Scalar(255, 0, 0);
    private static final Scalar GREEN = new Scalar(0, 255, 0);
    private static final Scalar BLUE = new Scalar(0, 0, 255);
    private static final Scalar BLUE_BOX = new Scalar(0, 12, 127);
    private static final Scalar RED_NOTEBOOK = new Scalar(255, 48, 48);
    private static final Scalar YELLOW_SUGAR = new Scalar(255,230,48);

    //Target is correctly aimed if x-pos of mTarget center is ± AIM_DELTA from x-poss center of frame center
    private static final int AIM_DELTA = 50;
    UpdateDirections directionsUpdater = null;
    Rect nullTarget;

    public TargetSearch(TankActivity mTankActivity) {
        this.mTankActivity = mTankActivity;
        mImageDirection = (ImageView) mTankActivity.findViewById(R.id.DirectionsImageView);
        mImageDirection.setImageResource(R.drawable.ok);
        mTextDirection = (TextView) mTankActivity.findViewById(R.id.DirectionsTextView);
        mTankLogic = mTankActivity.mTankLogic;
        nullTarget = new Rect(0, 0, 0, 0);
    }

    public Mat searchFaces(final Mat mIncomingFrame, CascadeClassifier faceCascadeClassifier) {
        Scalar color;
        // Create a grayscale version of the image
        Imgproc.cvtColor(mIncomingFrame, mGrayscaleImage, Imgproc.COLOR_RGBA2GRAY);

        MatOfRect faces = new MatOfRect();

        // Use the classifier to detect faces
        if (faceCascadeClassifier != null) {
            faceCascadeClassifier.detectMultiScale(mGrayscaleImage, faces, 1.1, 6, 2,
                                                   new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // If there are more than one face select the bigger (should be the closest)
        Rect[] facesArray = faces.toArray();
        mTarget = null;
        int targetArea = -1;
        for (Rect face : facesArray) {
            int faceArea = face.width * face.height;
            if (faceArea > targetArea) {
                targetArea = faceArea;
                mTarget = face;
            }
        }

        if (facesArray.length > 0) {
            for (Rect face : facesArray) {
                if (face != mTarget)
                    color = GREEN;
                else
                    color = RED;
                Core.rectangle(mIncomingFrame, face.tl(), face.br(), color, 3);
                Core.line(mIncomingFrame, new Point(face.x + face.width, face.y), new Point(face.x, face.y + face.height), color, 3);
                Core.line(mIncomingFrame, face.tl(), face.br(), color, 3);
            }
        }

        Point frameCenter = new Point(mIncomingFrame.width() / 2, mIncomingFrame.height() / 2);
        Core.line(mIncomingFrame, new Point(frameCenter.x - centerCrossSize / 2, frameCenter.y - centerCrossSize / 2), new Point(frameCenter.x + centerCrossSize / 2, frameCenter.y + centerCrossSize / 2), BLUE, 2);
        Core.line(mIncomingFrame, new Point(frameCenter.x + centerCrossSize / 2, frameCenter.y - centerCrossSize / 2), new Point(frameCenter.x - centerCrossSize / 2, frameCenter.y + centerCrossSize / 2), BLUE, 2);

        class updateDirections implements Runnable {
            Rect mTarget;
            Point frameCenter;

            updateDirections(Rect mTarget, Point mFrameCenter) {
                this.mTarget = mTarget;
                this.frameCenter = mFrameCenter;
            }

            public void run() {
                if (mTarget != null) {
                    mTankLogic.frameWidth(mIncomingFrame.width());
                    mTankLogic.frameHeight(mIncomingFrame.height());
                    mTankLogic.targetWidth(mTarget.width);
                    mTankLogic.targetHeight(mTarget.height);
                    mTankLogic.targetCenter(new Point(mTarget.x + mTarget.width / 2, mTarget.y + mTarget.height / 2));
                    mTextDirection.setVisibility(View.VISIBLE);
                    mImageDirection.setVisibility(View.VISIBLE);

                    if (frameCenter.x - (mTarget.x + mTarget.width / 2) > 0) {
                        mTankLogic.targetPosition(TankLogic.TARGET_POSITION_LEFT);
                        mTextDirection.setText("Turn Left");
                        mTextDirection.append("" + mTarget.width);
                        mImageDirection.setImageResource(R.drawable.right);
                    } else if (frameCenter.x - (mTarget.x + mTarget.width / 2) < 0) {
                        mTankLogic.targetPosition(TankLogic.TARGET_POSITION_RIGHT);
                        mImageDirection.setImageResource(R.drawable.left);
                        mTextDirection.setText("Turn Right");
                        mTextDirection.append("" + mTarget.width);
                    } else {
                        mTankLogic.targetPosition(TankLogic.TARGET_POSITION_FRONT);
                        mImageDirection.setImageResource(R.drawable.ok);
                        mTextDirection.setText("STOP!");
                        mTextDirection.append("" + mTarget.width);
                    }
                } else {
                    mTankLogic.targetPosition(TankLogic.TARGET_POSITION_NONE);
                    mTextDirection.setVisibility(View.INVISIBLE);
                    mImageDirection.setVisibility(View.INVISIBLE);
                }
            }
        }
        mTankActivity.runOnUiThread(new updateDirections(mTarget, frameCenter));
        return mIncomingFrame;
    }

    public Mat searchContour(Mat incomingFrame) {
        Mat binaryImage = new Mat();
        // Create a grayscale version of the image
        Imgproc.cvtColor(incomingFrame, mGrayscaleImage, Imgproc.COLOR_RGBA2GRAY);

        // Convert it in a binary image
        Imgproc.threshold(mGrayscaleImage, binaryImage, /*Imgproc.THRESH_OTSU*/160f, 1, Imgproc.THRESH_BINARY);

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(binaryImage, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(incomingFrame, contours, -1, RED);

        return incomingFrame;
    }

    public Mat searchColours(Mat incomingFrame) {
        Point frameCenter = new Point(incomingFrame.width() / 2, incomingFrame.height() / 2);
        int minDetectArea = 5000;
        Mat mRgba;
        Scalar mTargetColorRgba;
        Scalar mTargetColorHsv;
        ColorBlobDetector mDetector = new ColorBlobDetector();
        Mat mSpectrum = new Mat();
        Size SPECTRUM_SIZE = new Size(200, 64);
        Scalar CONTOUR_COLOR = GREEN;
        mTargetColorRgba = BLUE_BOX;
        Scalar mColorRadius = new Scalar(10, 70, 70, 0);
        mTargetColorHsv = mDetector.converScalarRgba2Hsv(mTargetColorRgba);
        mDetector.setColorRadius(mColorRadius);
        mDetector.setHsvColor(mTargetColorHsv);

        mRgba = incomingFrame;
        mDetector.process(mRgba);

        List<MatOfPoint> contours = mDetector.getContours();
        Log.e(TAG, "Contours count: " + contours.size());
        Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
        LinkedList<Rect> rects = new LinkedList<Rect>();
        Rect targetRect = nullTarget;
        for (MatOfPoint contour : contours) {
            Rect tempRect = Imgproc.boundingRect(contour);
            if (rectArea(tempRect) >= minDetectArea) {
                rects.add(tempRect);
                Core.rectangle(mRgba, rects.getLast().tl(), rects.getLast().br(), BLUE, 1);
                if (rectArea(tempRect) > rectArea(targetRect)) {
                    targetRect = tempRect;
                }
            }
        }
        Core.rectangle(mRgba, targetRect.tl(), targetRect.br(), RED, 3);

        Mat colorLabel = mRgba.submat(4, 68, 4, 68);
        colorLabel.setTo(mTargetColorRgba);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
        Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
        mSpectrum.copyTo(spectrumLabel);

        if (directionsUpdater == null) {
            directionsUpdater = new UpdateDirections(mTankActivity, frameCenter, mRgba.height(), mRgba.width(),nullTarget);
        }
        directionsUpdater.setTarget(targetRect);
        mTankActivity.runOnUiThread(directionsUpdater);

        return mRgba;
    }

    int rectArea(Rect mRect) {
        return mRect.width * mRect.height;
    }

    public void setAbsoluteFaceSize(int absoluteFaceSize) {
        this.absoluteFaceSize = absoluteFaceSize;
    }

    public void setmGrayscaleImage(Mat mGrayscaleImage) {
        this.mGrayscaleImage = mGrayscaleImage;
    }

}
