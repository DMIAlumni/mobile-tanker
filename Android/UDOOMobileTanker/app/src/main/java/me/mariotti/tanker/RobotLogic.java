package me.mariotti.tanker;

import android.util.Log;
import me.mariotti.ai.BaseAi;
import me.mariotti.messaging.Communicator;
import me.mariotti.messaging.ArduinoMessage;
import me.mariotti.messaging.IncomingMessage;
import me.mariotti.messaging.MessageEncoder;
import org.opencv.core.Point;

import java.util.Observable;
import java.util.Random;

public class RobotLogic extends BaseAi {
    private final String TAG = "RobotLogic";
    private Communicator mCommunicator;
    private RobotActivity mRobotActivity;
    private Point mLastTargetCenter;
    private int mTurnVelocity = MessageEncoder.DEFAULT_VELOCITY;
    private boolean mIsMovingForward;
    private int mDistance = Integer.MAX_VALUE;
    private boolean mIsAvoidingAnObstacle = false;
    private int mAvoidingDirection;
    private int mAvoidingPhase = -1;
    private long mCurrentPhaseTime = -1;
    private int mDefaultPhaseTimeSet = 500;
    private boolean mTargetFound = false;
    private boolean mCheerPhase = false;
    private long mStartCheerTime = -1;


    public RobotLogic(Communicator communicator, RobotActivity robotActivity) {
        IncomingMessage.getInstance().addObserver(this);

        mCommunicator = communicator;
        mRobotActivity = robotActivity;
    }

    @Override
    public void update(Observable observable, Object data) {
        ArduinoMessage incomingMessage = IncomingMessage.getInstance().getIncoming();

        if (!incomingMessage.hasError()) {
            if (incomingMessage.isInfoMessage() && incomingMessage.hasDistance()) {
                mDistance = incomingMessage.getData();
            }
            // for future use only. Gives to Arduino the ability to close this app.
            if (incomingMessage.isTerminateCommand()) {
                mRobotActivity.finish();
            }
        }
    }

    public void think() {
        if (mRobotActivity.canGo()) {
            if (mCheerPhase) {
                if (mStartCheerTime == -1) {
                    mStartCheerTime = System.currentTimeMillis();
                    Log.i(TAG, "Target at " + mDistance + "cm. CHEER.");
                    UpdateDirections.getInstance(mRobotActivity).found();
                }
                mCommunicator.setOutgoing(MessageEncoder.turnRight(MessageEncoder.DEFAULT_VELOCITY + 50, MessageEncoder.TURN_ON_SPOT));
                int mCheerLength = 3000;
                if (mStartCheerTime + mCheerLength < System.currentTimeMillis()) {
                    mCheerPhase = false;
                    mTargetFound = false;
                    mStartCheerTime = -1;
                    mRobotActivity.reset();
                    Log.i(TAG, "Stop searching. Choose a new color to find");
                    mCommunicator.setOutgoing(MessageEncoder.stop());
                    UpdateDirections.getInstance(mRobotActivity).chooseColor();
                }
                return;
            }

            if (mIsAvoidingAnObstacle) {
                if (mAvoidingDirection == LEFT) {
                    UpdateDirections.getInstance(mRobotActivity).avoidingLeft();
                } else {
                    UpdateDirections.getInstance(mRobotActivity).avoidingRight();
                }
                //While arounding the obstacle he see the target
                if (mTargetInSight) {
                    Log.i(TAG, "Target seen while arounding the obstacle");
                    mCommunicator.setOutgoing(MessageEncoder.stop());
                    stopAvoidingPhase();
                    return;
                }

                Log.i(TAG, "Phase: " + mAvoidingPhase);

                switch (mAvoidingPhase) {
                    case 1:
                        if (mCurrentPhaseTime == -1) {
                            mCurrentPhaseTime = System.currentTimeMillis();
                        }
                        if (mAvoidingDirection == LEFT) {
                            mCommunicator.setOutgoing(MessageEncoder.turnLeft(MessageEncoder.DEFAULT_VELOCITY + 30, MessageEncoder.TURN_ON_SPOT));
                        } else {
                            mCommunicator.setOutgoing(MessageEncoder.turnRight(MessageEncoder.DEFAULT_VELOCITY + 25, MessageEncoder.TURN_ON_SPOT));
                        }
                        checkPhaseTimeElapsed(mDefaultPhaseTimeSet);
                        break;
                    case 2:
                        mCommunicator.setOutgoing(MessageEncoder.moveForward(MessageEncoder.DEFAULT_VELOCITY, MessageEncoder.DEFAULT_VELOCITY));
                        checkPhaseTimeElapsed(mDefaultPhaseTimeSet * 2);
                        break;
                    case 3:
                        if (mAvoidingDirection == LEFT) {
                            mCommunicator.setOutgoing(MessageEncoder.turnRight(MessageEncoder.DEFAULT_VELOCITY + 25, MessageEncoder.TURN_ON_SPOT));
                        } else {
                            mCommunicator.setOutgoing(MessageEncoder.turnLeft(MessageEncoder.DEFAULT_VELOCITY + 30, MessageEncoder.TURN_ON_SPOT));
                        }
                        checkPhaseTimeElapsed(mDefaultPhaseTimeSet);
                        break;
                    case 4:
                }
            } else {
                //Search Phase
                UpdateDirections.getInstance(mRobotActivity).unlock();
                //Target Found
                if (mDistance != 0 && mDistance < 30 && mTargetInSight) {
                    if (mTargetFound) {
                        mCheerPhase = true;
                        mIsMovingForward = false;
                        mLastTargetCenter = null;
                        mTurnVelocity = MessageEncoder.DEFAULT_VELOCITY;
                        return;
                    } else {
                        mTargetFound = true;
                        return;
                    }
                }
                //Obstacle on my way
                if (mDistance != 0 && mDistance < 30 && !mTargetInSight) {
                    UpdateDirections.getInstance(mRobotActivity).lock();
                    Log.i(TAG, "Obstacle at " + mDistance + "cm. Starting arounding orocess.");
                    mCommunicator.setOutgoing(MessageEncoder.stop());
                    mIsMovingForward = false;
                    mLastTargetCenter = null;
                    mTurnVelocity = MessageEncoder.DEFAULT_VELOCITY;
                    startAvoidingPhase(true);
                    return;
                }
                if (!mTargetInSight) {
                    Log.i(TAG, "None Object at in sight. Searching.");
                    mCommunicator.setOutgoing(MessageEncoder.search());
                    mIsMovingForward = false;
                    mLastTargetCenter = null;
                    mTurnVelocity = MessageEncoder.DEFAULT_VELOCITY;
                    return;
                }
                //if target is thinner than 100px consider it 100px width
                mTargetWidth = mTargetWidth < 100 ? 100 : mTargetWidth;
                // Target in sight
                if (mTargetCenter.x < mFrameWidth / 2 - mTargetWidth / 2 || mTargetCenter.x > mFrameWidth / 2 + mTargetWidth / 2) {
                    Log.i(TAG, "Target insight");
                    //TODO add a minimum width of target
                    //power up velocity if since last frame we didn't move, and wen we start moving maintain it since aim OK
                    if (isNotTurning()) {
                        int mVelocityStep = 1;
                        mTurnVelocity += mVelocityStep;
                        if (mTurnVelocity > 255 - mVelocityStep)
                            mTurnVelocity = MessageEncoder.DEFAULT_VELOCITY;
                        //TODO send rover in emergency mode cause it's stuck
                    }
                    //target not aimed
                    if (mTargetDirection == TARGET_POSITION_LEFT) {
                        if (mIsMovingForward) {
                            mCommunicator.setOutgoing(MessageEncoder.moveForward(MessageEncoder.DEFAULT_VELOCITY, MessageEncoder.DEFAULT_VELOCITY + 20));
                        } else {
                            mCommunicator.setOutgoing(MessageEncoder.turnLeft(mTurnVelocity, MessageEncoder.TURN_NORMALLY));
                            mIsMovingForward = false;
                        }
                    }
                    if (mTargetDirection == TARGET_POSITION_RIGHT) {
                        if (mIsMovingForward) {
                            mCommunicator.setOutgoing(MessageEncoder.moveForward(MessageEncoder.DEFAULT_VELOCITY + 20, MessageEncoder.DEFAULT_VELOCITY));
                        } else {
                            mCommunicator.setOutgoing(MessageEncoder.turnRight(mTurnVelocity, MessageEncoder.TURN_NORMALLY));
                            mIsMovingForward = false;
                        }
                    }
                } else {// Target aimed
                    mTurnVelocity = MessageEncoder.DEFAULT_VELOCITY;
                    mCommunicator.setOutgoing(MessageEncoder.moveForward(MessageEncoder.DEFAULT_VELOCITY, MessageEncoder.DEFAULT_VELOCITY));
                    mIsMovingForward = true;
                }
                mLastTargetCenter = mTargetCenter;
            }
        } else {
            mCommunicator.setOutgoing(MessageEncoder.stop());
            stopAvoidingPhase();
            mIsMovingForward = false;
            mLastTargetCenter = null;
            mTurnVelocity = MessageEncoder.DEFAULT_VELOCITY;
        }
    }

    // Internals
    // ---------

    private void startAvoidingPhase(boolean newDirection) {
        mCommunicator.setOutgoing(MessageEncoder.stop());
        mIsAvoidingAnObstacle = true;
        mAvoidingPhase = 1;
        if (newDirection) {
            mAvoidingDirection = new Random(System.currentTimeMillis()).nextInt(2) == 0 ? LEFT : RIGHT;
        }
    }

    private void stopAvoidingPhase() {
        mCurrentPhaseTime = mAvoidingPhase = -1;
        mIsAvoidingAnObstacle = false;
    }

    void checkPhaseTimeElapsed(long phaseTimeSet) {
        //Phase time elapsed
        if (System.currentTimeMillis() > mCurrentPhaseTime + phaseTimeSet) {
            //se c'è ancora un ostacolo a meno di 30 centimetri aumenta la durata della fase corrente (tranne che in fase 2)
            if (mDistance != 0 && mDistance < 30 && mAvoidingPhase != 2) {
                mCurrentPhaseTime += mDefaultPhaseTimeSet / 5;
            } else {
                mCurrentPhaseTime = System.currentTimeMillis();
                if (++mAvoidingPhase > 3) {
                    stopAvoidingPhase();
                }
            }
        }
    }

    private boolean isNotTurning() {
        return mLastTargetCenter != null && Math.abs(mTargetCenter.x - mLastTargetCenter.x) < 10;
    }

    public void frameWidth(int width) {
        mFrameWidth = width;
    }
}
