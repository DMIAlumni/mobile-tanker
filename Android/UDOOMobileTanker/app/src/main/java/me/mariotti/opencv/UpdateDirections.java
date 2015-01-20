package me.mariotti.opencv;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import me.mariotti.tanker.R;
import me.mariotti.tanker.TankActivity;
import me.mariotti.tanker.TankLogic;
import org.opencv.core.Point;
import org.opencv.core.Rect;

public class UpdateDirections implements Runnable {

    private ImageView mImageDirection;
    private TextView mTextDirection = null;
    private int image;
    private String text;
    private int isVisible = View.INVISIBLE;
    private TankActivity mTankActivity;
    private static UpdateDirections instance;
    private boolean enabled = true;

    private UpdateDirections(TankActivity mTankActivity) {
        this.mTankActivity = mTankActivity;
        mImageDirection = (ImageView) mTankActivity.findViewById(R.id.DirectionsImageView);
        mTextDirection = (TextView) mTankActivity.findViewById(R.id.DirectionsTextView);
        instance = this;
    }

    UpdateDirections() {
    }

    static public UpdateDirections getInstance(TankActivity mTankActivity) {
        if (instance == null) {
            instance = new UpdateDirections(mTankActivity);
        }
        return instance;
    }

    public void run() {
        mTextDirection.setVisibility(isVisible);
        mImageDirection.setVisibility(isVisible);
        mTextDirection.setText(text);
        mImageDirection.setImageResource(image);
        if (!enabled && image == R.drawable.tavolozza) {
            unlock();
        }
    }

    public void left() {
        if (enabled) {
            text = "Turn Left";
            image = R.drawable.left;
            mTankActivity.runOnUiThread(this);
        }
    }

    public void right() {
        if (enabled) {
            text = "Turn Right";
            image = R.drawable.right;
            mTankActivity.runOnUiThread(this);
        }
    }

    public void aimed() {
        if (enabled) {
            text = "Target in sight, aimed!";
            image = R.drawable.aimed;
            mTankActivity.runOnUiThread(this);
        }
    }

    public void search() {
        if (enabled) {
            text = "Searching...";
            image = R.drawable.searching;
            mTankActivity.runOnUiThread(this);
        }
    }

    public void found() {
        lock();
        image = R.drawable.found;
        mTankActivity.runOnUiThread(this);
    }

    public void show() {
        isVisible = View.VISIBLE;
        mTankActivity.runOnUiThread(this);
    }

    public void hide() {
        isVisible = View.INVISIBLE;
        mTankActivity.runOnUiThread(this);
    }

    public void chooseColor() {
        text = "Pick a color on the camera stream";
        image = R.drawable.tavolozza;
        mTankActivity.runOnUiThread(this);
    }

    public void avoidingLeft() {
        text = "Avoiding obstacle to the left";
        image = R.drawable.avoid_left;
        mTankActivity.runOnUiThread(this);
    }

    public void avoidingRight() {

        text = "Avoiding obstacle to the right";
        image = R.drawable.avoid_right;
        mTankActivity.runOnUiThread(this);
    }

    public void lock() {
        enabled = false;
    }

    public void unlock() {
        enabled = true;
    }
}