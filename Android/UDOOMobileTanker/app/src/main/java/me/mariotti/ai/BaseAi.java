package me.mariotti.ai;


import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.Observer;

public abstract class BaseAi implements IBaseAi, Observer {
    public final static int TARGET_POSITION_FRONT = 3;
    public final static int TARGET_POSITION_LEFT = 1;
    public final static int TARGET_POSITION_RIGHT = 2;
    public final static int TARGET_POSITION_NONE = 0;
    public final static int LEFT = 0;
    public final static int RIGHT = 1;

    protected int mFrameWidth;
    protected Point mTargetCenter;
    protected boolean mTargetInSight = false;
    protected int mTargetDirection;
    protected int mTargetWidth;

    @Override
    public void targetPosition(Mat frame, Rect target, int position) {
        mFrameWidth = frame.width();
        mTargetInSight = position != TARGET_POSITION_NONE;
        mTargetDirection = position;
        mTargetWidth = target.width;
        mTargetCenter = new Point(target.x + target.width / 2, target.y + target.height / 2);
        think();
    }
}
