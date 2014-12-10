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


    private final ImageView mImageDirection;
    private final TextView mTextDirection;
    Rect mTarget;
    private TankLogic mTankLogic;
    private final TankActivity mTankActivity;
    Point frameCenter;
    private final int frameHeight;
    private final int frameWidth;
    Rect nullTarget;

    UpdateDirections(TankActivity mTankActivity, Point mFrameCenter,int frameHeight, int frameWidth, Rect nullTarget) {
        this.mTankActivity = mTankActivity;
        this.frameCenter = mFrameCenter;
        this.frameHeight = frameHeight;
        this.frameWidth = frameWidth;
        this.nullTarget = nullTarget;
        mTankLogic = mTankActivity.mTankLogic;
        mImageDirection = (ImageView) mTankActivity.findViewById(R.id.DirectionsImageView);
        mImageDirection.setImageResource(R.drawable.ok);
        mTextDirection = (TextView) mTankActivity.findViewById(R.id.DirectionsTextView);
    }

    public void run() {
        if (mTarget != nullTarget) {
            //if after a resume the tank logic instance has changed update it
            mTankLogic=mTankLogic!=mTankActivity.mTankLogic?mTankActivity.mTankLogic:mTankLogic;

            mTankLogic.frameWidth(frameWidth);
            mTankLogic.frameHeight(frameHeight);
            mTankLogic.targetWidth(mTarget.width);
            mTankLogic.targetHeight(mTarget.height);
            mTankLogic.targetCenter(new Point(mTarget.x + mTarget.width / 2, mTarget.y + mTarget.height / 2));
            mTextDirection.setVisibility(View.VISIBLE);
            mImageDirection.setVisibility(View.VISIBLE);

            if (frameCenter.x - (mTarget.x + mTarget.width / 2) > 0) {
                mTankLogic.targetPosition(TankLogic.TARGET_POSITION_LEFT);
                mTextDirection.setText("Turn Left");
                mTextDirection.append("\nTarget Area = " + mTarget.width*mTarget.height);
                mImageDirection.setImageResource(R.drawable.right);
            } else if (frameCenter.x - (mTarget.x + mTarget.width / 2) < 0) {
                mTankLogic.targetPosition(TankLogic.TARGET_POSITION_RIGHT);
                mImageDirection.setImageResource(R.drawable.left);
                mTextDirection.setText("Turn Right");
                mTextDirection.append("\nTarget Area = " + mTarget.width*mTarget.height);
            } else {
                mTankLogic.targetPosition(TankLogic.TARGET_POSITION_FRONT);
                mImageDirection.setImageResource(R.drawable.ok);
                mTextDirection.setText("STOP!");
                mTextDirection.append("\nTarget Area = " + mTarget.width*mTarget.height);
            }
        } else {
            mTankLogic.targetPosition(TankLogic.TARGET_POSITION_NONE);
            mTextDirection.setVisibility(View.INVISIBLE);
            mImageDirection.setVisibility(View.INVISIBLE);
        }
    }
    public void setTarget(Rect mTarget) {
        this.mTarget = mTarget;
    }
}
