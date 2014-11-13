package me.mariotti.opencv;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import me.mariotti.tanker.TankLogic;
import me.mariotti.tanker.R;
import me.mariotti.tanker.TankActivity;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 * Created by simone on 31/10/14.
 */
public class TargetSearch {
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
    //Target is correctly aimed if x-pos of mTarget center is ± AIM_DELTA from x-poss center of frame center
    private static final int AIM_DELTA = 10;

    public TargetSearch(TankActivity mTankActivity) {
        this.mTankActivity = mTankActivity;
        mImageDirection = (ImageView) mTankActivity.findViewById(R.id.DirectionsImageView);
        mImageDirection.setImageResource(R.drawable.ok);
        mTextDirection = (TextView) mTankActivity.findViewById(R.id.DirectionsTextView);
        mTankLogic = mTankActivity.mTankLogic;
    }

    public Mat AnalyzeFrame(Mat mIncomingFrame, CascadeClassifier faceCascadeClassifier) {
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
                    mTextDirection.setVisibility(View.VISIBLE);
                    mImageDirection.setVisibility(View.VISIBLE);
                    if (frameCenter.x - (mTarget.x + mTarget.width / 2) > AIM_DELTA) {
                        mTankLogic.targetPosition(mTankLogic.TARGET_POSITION_LEFT);
                        mTextDirection.setText("Turn Left");
                        mImageDirection.setImageResource(R.drawable.right);
                    } else if (frameCenter.x - (mTarget.x + mTarget.width / 2) < -AIM_DELTA) {
                        mTankLogic.targetPosition(mTankLogic.TARGET_POSITION_RIGHT);
                        mImageDirection.setImageResource(R.drawable.left);
                        mTextDirection.setText("Turn Right");
                    } else {
                        mTankLogic.targetPosition(mTankLogic.TARGET_POSITION_FRONT);
                        mImageDirection.setImageResource(R.drawable.ok);
                        mTextDirection.setText("STOP!");
                    }
                } else {
                    mTankLogic.targetPosition(mTankLogic.TARGET_POSITION_NONE);
                    mTextDirection.setVisibility(View.INVISIBLE);
                    mImageDirection.setVisibility(View.INVISIBLE);
                }
            }
        }
        mTankActivity.runOnUiThread(new updateDirections(mTarget, frameCenter));
        return mIncomingFrame;
    }

    public void setAbsoluteFaceSize(int absoluteFaceSize) {
        this.absoluteFaceSize = absoluteFaceSize;
    }

    public void setmGrayscaleImage(Mat mGrayscaleImage) {
        this.mGrayscaleImage = mGrayscaleImage;
    }

}
