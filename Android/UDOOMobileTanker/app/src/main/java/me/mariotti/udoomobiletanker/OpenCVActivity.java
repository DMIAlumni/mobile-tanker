package me.mariotti.udoomobiletanker;

/**
 * Created by simone on 28/10/14.
 */

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import me.palazzetti.adktoolkit.AdkManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class OpenCVActivity extends Activity implements CvCameraViewListener {


    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier faceCascadeClassifier;
    public Communicator msgCenter;
    public AdkManager arduino;
    private Mat grayscaleImage;
    private int absoluteFaceSize;
    private int centerCrossSize = 20;
    private ImageView imageDirection;
    private TextView textDirection;
    private Rect target;
    private static final Scalar RED = new Scalar(255, 0, 0);
    private static final Scalar GREEN = new Scalar(0, 255, 0);
    private static final Scalar BLUE = new Scalar(0, 0, 255);
    //Target is correctly aimed if x-pos of target center is ± AIM_DELTA from x-poss center of frame center
    private static final int AIM_DELTA = 10;


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
            Log.i("OpenCVActivity", "Face Cascade File loaded");
        } catch (Exception e) {
            Log.i("OpenCVActivity", "Error loading cascades", e);
        }
        // And we are ready to go
        openCvCameraView.enableView();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.facetest);

        arduino = new AdkManager((UsbManager) getSystemService(Context.USB_SERVICE));
        msgCenter = new Communicator(this);

        //msgCenter.setOutocoming(String.valueOf(1));

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
        grayscaleImage = new Mat(height, width, CvType.CV_8UC1);
        // The faces will be about a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        Scalar color;
        // Create a grayscale image
        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);

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
                Core.rectangle(aInputFrame, face.tl(), face.br(), color, 3);
                Core.line(aInputFrame, new Point(face.x + face.width, face.y), new Point(face.x, face.y + face.height), color, 3);
                Core.line(aInputFrame, face.tl(), face.br(), color, 3);
            }
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
        runOnUiThread(new updateDirections(target, frameCenter));
        return aInputFrame;
    }

    @Override
    protected void onPause() {
        super.onPause();
        arduino.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
        arduino.open();
        msgCenter.execute();
    }
}