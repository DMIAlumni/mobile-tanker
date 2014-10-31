package me.mariotti.opencv;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import me.mariotti.udoomobiletanker.R;
import me.mariotti.udoomobiletanker.TankActivity;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 * Created by simone on 31/10/14.
 */
public class TargetSearch {
    private TankActivity mTankActivity;
    private Mat grayscaleImage;
    private int absoluteFaceSize,centerCrossSize = 20;
    private ImageView imageDirection;
    private TextView textDirection;
    private Rect target;
    private static final Scalar RED = new Scalar(255, 0, 0);
    private static final Scalar GREEN = new Scalar(0, 255, 0);
    private static final Scalar BLUE = new Scalar(0, 0, 255);
    //Target is correctly aimed if x-pos of target center is ± AIM_DELTA from x-poss center of frame center
    private static final int AIM_DELTA = 10;

    public TargetSearch(TankActivity mTankActivity) {
        this.mTankActivity=mTankActivity;
        imageDirection = (ImageView) mTankActivity.findViewById(R.id.DirectionsImageView);
        imageDirection.setImageResource(R.drawable.ok);
        textDirection = (TextView) mTankActivity.findViewById(R.id.DirectionsTextView);

    }
    public Mat AnalyzeFrame(Mat mIncomingFrame, CascadeClassifier faceCascadeClassifier){
        Scalar color;
        // Create a grayscale version of the image
        Imgproc.cvtColor(mIncomingFrame, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);

        MatOfRect faces = new MatOfRect();

        // Use the classifier to detect faces
        if (faceCascadeClassifier != null) {
            faceCascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 6, 2,
                                                   new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // If there are more than one face select the bigger (should be the closest)
        Rect[] facesArray = faces.toArray();
        target = null;
        int targetArea = -1;
        for (Rect face : facesArray) {
            int faceArea = face.width * face.height;
            if (faceArea > targetArea) {
                targetArea = faceArea;
                target = face;
            }
        }

        if (facesArray.length > 0) {
            for (Rect face : facesArray) {
                if (face != target)
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
            Rect target;
            Point frameCenter;

            updateDirections(Rect target, Point frameCenter) {
                this.target = target;
                this.frameCenter = frameCenter;
            }

            public void run() {
                if (target != null) {
                    textDirection.setVisibility(View.VISIBLE);
                    imageDirection.setVisibility(View.VISIBLE);
                    if (frameCenter.x - (target.x + target.width / 2) > AIM_DELTA) {
                        textDirection.setText("Turn Left");
                        imageDirection.setImageResource(R.drawable.right);
                    } else if (frameCenter.x - (target.x + target.width / 2) < -AIM_DELTA) {
                        imageDirection.setImageResource(R.drawable.left);
                        textDirection.setText("Turn Right");
                    } else {
                        imageDirection.setImageResource(R.drawable.ok);
                        textDirection.setText("STOP!");
                    }
                } else {
                    textDirection.setVisibility(View.INVISIBLE);
                    imageDirection.setVisibility(View.INVISIBLE);
                }
            }
        }
        mTankActivity.runOnUiThread(new updateDirections(target, frameCenter));
        return mIncomingFrame;
    }
    public void setAbsoluteFaceSize(int absoluteFaceSize) {
        this.absoluteFaceSize = absoluteFaceSize;
    }
    public void setGrayscaleImage(Mat grayscaleImage) {
        this.grayscaleImage = grayscaleImage;
    }

}
