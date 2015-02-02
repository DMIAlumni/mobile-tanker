package me.mariotti.tanker;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class UpdateDirections implements Runnable {

    private ImageView mImageDirection;
    private TextView mTextDirection = null;
    private int mImage;
    private String mText;
    private int mIsVisible = View.INVISIBLE;
    private TankActivity mTankActivity;
    private static UpdateDirections mInstance;
    private boolean mEnabled = true;

    private UpdateDirections(TankActivity mTankActivity) {
        this.mTankActivity = mTankActivity;
        mImageDirection = (ImageView) mTankActivity.findViewById(R.id.DirectionsImageView);
        mTextDirection = (TextView) mTankActivity.findViewById(R.id.DirectionsTextView);
        mInstance = this;
    }

    private UpdateDirections() {}

    static public synchronized UpdateDirections getInstance(TankActivity mTankActivity) {
        if (mInstance == null) {
            mInstance = new UpdateDirections(mTankActivity);
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
            mTankActivity.runOnUiThread(this);
        }
    }

    public void right() {
        if (mEnabled) {
            mText = "Turn Right";
            mImage = R.drawable.right;
            mTankActivity.runOnUiThread(this);
        }
    }

    public void aimed() {
        if (mEnabled) {
            mText = "Target in sight, aimed!";
            mImage = R.drawable.aimed;
            mTankActivity.runOnUiThread(this);
        }
    }

    public void search() {
        if (mEnabled) {
            mText = "Searching...";
            mImage = R.drawable.searching;
            mTankActivity.runOnUiThread(this);
        }
    }

    public void found() {
        lock();
        mImage = R.drawable.found;
        mTankActivity.runOnUiThread(this);
    }

    public void show() {
        mIsVisible = View.VISIBLE;
        mTankActivity.runOnUiThread(this);
    }

    public void hide() {
        mIsVisible = View.INVISIBLE;
        mTankActivity.runOnUiThread(this);
    }

    public void chooseColor() {
        mText = "Pick a color on the camera stream";
        mImage = R.drawable.tavolozza;
        mTankActivity.runOnUiThread(this);
    }

    public void avoidingLeft() {
        mText = "Avoiding obstacle to the left";
        mImage = R.drawable.avoid_left;
        mTankActivity.runOnUiThread(this);
    }

    public void avoidingRight() {

        mText = "Avoiding obstacle to the right";
        mImage = R.drawable.avoid_right;
        mTankActivity.runOnUiThread(this);
    }

    public void lock() {
        mEnabled = false;
    }

    public void unlock() {
        mEnabled = true;
    }
}