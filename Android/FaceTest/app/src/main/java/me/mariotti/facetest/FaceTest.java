package me.mariotti.facetest;

/**
 * Created by simone on 28/10/14.
 */

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class FaceTest extends Activity implements CvCameraViewListener {


    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier faceCascadeClassifier;
    private CascadeClassifier eyeCascadeClassifier;
    private CascadeClassifier smileCascadeClassifier;
    private Mat grayscaleImage;
    private int absoluteFaceSize;
    private int centerCrossSize = 20;
    private ImageView imageDirection;
    private TextView textDirection;
    private Rect target;
    private static final Scalar RED = new Scalar(255, 0, 0);
    private static final Scalar GREEN = new Scalar(0, 255, 0);
    private static final Scalar BLUE = new Scalar(0, 0, 255);


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
            InputStream isf = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFileF = new File(cascadeDir, "faceDetection.xml");
            FileOutputStream osf = new FileOutputStream(mCascadeFileF);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = isf.read(buffer)) != -1) {
                osf.write(buffer, 0, bytesRead);
            }
            isf.close();
            osf.close();
            // Load the cascade classifier
            faceCascadeClassifier = new CascadeClassifier(mCascadeFileF.getAbsolutePath());
            Log.e("FaceTest", "Face Cascade File loaded");

            // Copy the Eye cascade file into a temp file so OpenCV can load it
            InputStream ise = getResources().openRawResource(R.raw.haarcascade_righteye_2splits);
            File mCascadeFileE = new File(cascadeDir, "eyeDetection.xml");
            FileOutputStream ose = new FileOutputStream(mCascadeFileE);
            buffer = new byte[4096];
            while ((bytesRead = ise.read(buffer)) != -1) {
                ose.write(buffer, 0, bytesRead);
            }
            ise.close();
            ose.close();
            // Load the cascade classifier
            eyeCascadeClassifier = new CascadeClassifier(mCascadeFileE.getAbsolutePath());
            Log.e("FaceTest", "eye Cascade File loaded");

            // Copy the Smile cascade file into a temp file so OpenCV can load it
            InputStream iss = getResources().openRawResource(R.raw.haarcascade_smile);
            File mCascadeFileS = new File(cascadeDir, "smileDetection.xml");
            FileOutputStream oss = new FileOutputStream(mCascadeFileS);
            buffer = new byte[4096];
            while ((bytesRead = iss.read(buffer)) != -1) {
                oss.write(buffer, 0, bytesRead);
            }
            iss.close();
            oss.close();
            // Load the cascade classifier
            smileCascadeClassifier = new CascadeClassifier(mCascadeFileS.getAbsolutePath());
            Log.e("FaceTest", "smile Cascade File loaded");
        } catch (Exception e) {
            Log.e("FaceTest", "Error loading cascades", e);
        }


        // And we are ready to go
        openCvCameraView.enableView();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //openCvCameraView = new JavaCameraView(this, -1);

        //openCvCameraView=R.layout.facetest;

        //setContentView(openCvCameraView);
        setContentView(R.layout.facetest);

        //openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_calibration_java_surface_view);
        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.CameraPreview);

        openCvCameraView.setVisibility(SurfaceView.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);
        openCvCameraView.getHolder().setFixedSize(960, 540);
        imageDirection = (ImageView) findViewById(R.id.imageView);
        imageDirection.setImageResource(R.drawable.ok);
        textDirection = (TextView) findViewById(R.id.textView);
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);


        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);
    }


    @Override
    public void onCameraViewStopped() {
    }


    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        Scalar color;
        // Create a grayscale image
        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);


        MatOfRect faces = new MatOfRect();


        // Use the classifier to detect faces
        if (faceCascadeClassifier != null) {
            faceCascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                                                   new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }


        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();
        target = null;
        /*
        int targetArea = -1;
        for (int j = 0; j < facesArray.length; j++) {
            int faceArea = facesArray[j].width * facesArray[j].height;
            if (faceArea > targetArea) {
                targetArea = faceArea;
                target = facesArray[j];
            }
        }
        */
        if (facesArray.length > 0) {
            for (Rect face : facesArray) {
                //Create a submatrix containing the top half face
                Mat faceImage = grayscaleImage.submat(face.y, face.y + face.height/2,face.x, face.x + face.width);
                // Mat faceImage = grayscaleImage.rowRange(face.y, face.y + face.width).colRange(face.x, face.x + face.width);

                //eyeCascadeClassifier.detectMultiScale(faceImage, eyes, 1.03, 2, 3, new Size(1, 1), new Size(100,100));
                MatOfRect eyes = new MatOfRect();
                eyeCascadeClassifier.detectMultiScale(faceImage, eyes, 1.1, 3, 3, new Size(15, 15), new Size(100,100));

                Rect[] eyesArray = eyes.toArray();
                if (eyesArray.length > 0) {
                    for (Rect eye : eyesArray) {
                        Core.circle(aInputFrame, new Point(face.x+eye.x + eye.width / 2, face.y+eye.y + eye.height / 2), eye.width / 2, BLUE, 3);
                    }
                }
            }
        }
        for (int i = 0; i < facesArray.length; i++) {
            if (facesArray[i] != target)
                color = GREEN;
            else
                color = RED;
            Core.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), color, 3);
            Core.line(aInputFrame, new Point(facesArray[i].x + facesArray[i].width, facesArray[i].y), new Point(facesArray[i].x, facesArray[i].y + facesArray[i].height), color, 3);
            Core.line(aInputFrame, facesArray[i].tl(), facesArray[i].br(), color, 3);
        }
        Point frameCenter = new Point(aInputFrame.width() / 2, aInputFrame.height() / 2);
        Core.line(aInputFrame, new Point(frameCenter.x - centerCrossSize / 2, frameCenter.y - centerCrossSize / 2), new Point(frameCenter.x + centerCrossSize / 2, frameCenter.y + centerCrossSize / 2), BLUE, 2);
        Core.line(aInputFrame, new Point(frameCenter.x + centerCrossSize / 2, frameCenter.y - centerCrossSize / 2), new Point(frameCenter.x - centerCrossSize / 2, frameCenter.y + centerCrossSize / 2), BLUE, 2);


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
                    if (frameCenter.x - (target.x + target.width / 2) > 10) {
                        textDirection.setText("Turn Left");
                        imageDirection.setImageResource(R.drawable.right);
                    } else if (frameCenter.x - (target.x + target.width / 2) < -10) {
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
        runOnUiThread(new updateDirections(target, frameCenter));


        return aInputFrame;
    }


    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }

}