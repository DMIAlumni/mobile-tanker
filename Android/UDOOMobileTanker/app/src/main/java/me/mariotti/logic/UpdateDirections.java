package me.mariotti.logic;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class UpdateDirections implements Runnable {

    private ImageView mImageDirection;
    private TextView mTextDirection = null;
    private int mImage;
    private String mText;
    private int mIsVisible = View.INVISIBLE;
    private RobotActivity mRobotActivity;
    private static UpdateDirections mInstance;
    private boolean mEnabled = true;

    private UpdateDirections(RobotActivity mRobotActivity) {
        this.mRobotActivity = mRobotActivity;
        mImageDirection = (ImageView) mRobotActivity.findViewById(R.id.DirectionsImageView);
        mTextDirection = (TextView) mRobotActivity.findViewById(R.id.DirectionsTextView);
        mInstance = this;
    }

    static public synchronized UpdateDirections getInstance(RobotActivity mRobotActivity) {
        if (mInstance == null) {
            mInstance = new UpdateDirections(mRobotActivity);
        }
        return mInstance;
    }

    public void run() {
        mTextDirection.setVisibility(mIsVisible);
        mImageDirection.setVisibility(mIsVisible);
        mTextDirection.setText(mText);
        mImageDirection.setImageResource(mImage);
        if (!mEnabled && mImage == R.drawable.tavolozza) {
            unlock();
        }
    }

    public void left() {
        if (mEnabled) {
            mText = "Turn Left";
            mImage = R.drawable.left;
            mRobotActivity.runOnUiThread(this);
        }
    }

    public void right() {
        if (mEnabled) {
            mText = "Turn Right";
            mImage = R.drawable.right;
            mRobotActivity.runOnUiThread(this);
        }
    }

    public void aimed() {
        if (mEnabled) {
            mText = "Target in sight, aimed!";
            mImage = R.drawable.aimed;
            mRobotActivity.runOnUiThread(this);
        }
    }

    public void search() {
        if (mEnabled) {
            mText = "Searching...";
            mImage = R.drawable.searching;
            mRobotActivity.runOnUiThread(this);
        }
    }

    public void found() {
        lock();
        mImage = R.drawable.found;
        mRobotActivity.runOnUiThread(this);
    }

    public void show() {
        mIsVisible = View.VISIBLE;
        mRobotActivity.runOnUiThread(this);
    }

    public void hide() {
        mIsVisible = View.INVISIBLE;
        mRobotActivity.runOnUiThread(this);
    }

    public void chooseColor() {
        mText = "Pick a color on the camera stream";
        mImage = R.drawable.tavolozza;
        mRobotActivity.runOnUiThread(this);
    }

    public void avoidingLeft() {
        mText = "Avoiding obstacle to the left";
        mImage = R.drawable.avoid_left;
        mRobotActivity.runOnUiThread(this);
    }

    public void avoidingRight() {

        mText = "Avoiding obstacle to the right";
        mImage = R.drawable.avoid_right;
        mRobotActivity.runOnUiThread(this);
    }

    public void lock() {
        mEnabled = false;
    }

    public void unlock() {
        mEnabled = true;
    }
}