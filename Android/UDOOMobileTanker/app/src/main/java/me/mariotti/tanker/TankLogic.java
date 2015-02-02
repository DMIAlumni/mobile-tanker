package me.mariotti.tanker;

import android.util.Log;
import me.mariotti.tanker.messaging.Communicator;
import me.mariotti.tanker.messaging.DecodedMessage;
import me.mariotti.tanker.messaging.MessageEncoderDecoder;
import org.opencv.core.Point;

import java.util.Observable;
import java.util.Observer;
import java.util.Random;

public class TankLogic implements Observer {
    public final static int TARGET_POSITION_FRONT = 3;
    public final static int TARGET_POSITION_LEFT = 1;
    public final static int TARGET_POSITION_RIGHT = 2;
    public final static int TARGET_POSITION_NONE = 0;
    public final static int LEFT = 0;
    public final static int RIGHT = 1;
    private final String TAG = "TankLogic";
    private Communicator mCommunicator;
    private RobotActivity mRobotActivity;
    private Boolean mTargetInSight = false;
    private int mTargetDirection;
    private Point mTargetCenter;
    private Point mLastTargetCenter;
    private int mTargetWidth;
    private int mTargetHeight;
    private int frameHeight;
    private int mFrameWidth;
    private int mTurnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
    private boolean mIsMovingForward;
    private int mDistance = Integer.MAX_VALUE;
    private boolean mIsAvoidingAnObstacle = false;
    private int mAvoidingDirection;
    private int mAvoidingPhase = -1;
    private long mCurrentPhaseTime = -1;
    private int mDefaultlPhaseTimeSet = 500;
    private boolean mTargetFound = false;
    private boolean mCheer = false;
    private long mStartCheerTime = -1;


    public TankLogic(Communicator mCommunicator, RobotActivity robotActivity) {
        this.mCommunicator = mCommunicator;
        mRobotActivity = robotActivity;
        mCommunicator.mIncomingMessageObservable.addObserver(this);
    }

    @Override
    public void update(Observable observable, Object data) {
        decodeMessage();
    }

    public void targetPosition(int mPosition) {
        mTargetInSight = mPosition != TARGET_POSITION_NONE;
        mTargetDirection = mPosition;
        think();
    }

    public void targetCenter(Point mPoint) {
        mTargetCenter = mPoint;
    }

    public void targetWidth(int mWidth) {
        mTargetWidth = mWidth;
    }

    public void targetHeight(int mHeight) {
        mTargetHeight = mHeight;
    }

    private void think() {
        if (mRobotActivity.canGo()) {
            if (mCheer) {
                if (mStartCheerTime == -1) {
                    mStartCheerTime = System.currentTimeMillis();
                    Log.i(TAG, "Target at " + mDistance + "cm. CHEER.");
                    UpdateDirections.getInstance(mRobotActivity).found();
                }
                mCommunicator.setOutgoing(MessageEncoderDecoder.turnRight(MessageEncoderDecoder.DEFAULT_VELOCITY + 50, MessageEncoderDecoder.TURN_ON_SPOT));
                int mCheerLength = 3000;
                if (mStartCheerTime + mCheerLength < System.currentTimeMillis()) {
                    mCheer = false;
                    mTargetFound = false;
                    mStartCheerTime = -1;
                    mRobotActivity.reset();
                    Log.i(TAG, "Stop searching. Choose a new color to find");
                    mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
                    UpdateDirections.getInstance(mRobotActivity).chooseColor();
                }
                return;
            }
            // Stop robot with an object within 5cm
            if (mDistance != 0 && mDistance < 5) {
                stopAvoidingPhase();
                Log.i(TAG, "Object at " + mDistance + "cm. Stopped.");
                mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
                mIsMovingForward = false;
                mLastTargetCenter = null;
                mTurnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
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
                    mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
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
                            mCommunicator.setOutgoing(MessageEncoderDecoder.turnLeft(MessageEncoderDecoder.DEFAULT_VELOCITY + 30, MessageEncoderDecoder.TURN_ON_SPOT));
                        } else {
                            mCommunicator.setOutgoing(MessageEncoderDecoder.turnRight(MessageEncoderDecoder.DEFAULT_VELOCITY + 25, MessageEncoderDecoder.TURN_ON_SPOT));
                        }
                        checkPhaseTimeElapsed(mDefaultlPhaseTimeSet);
                        break;
                    case 2:
                        mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY, MessageEncoderDecoder.DEFAULT_VELOCITY));
                        checkPhaseTimeElapsed(mDefaultlPhaseTimeSet * 2);
                        break;
                    case 3:
                        if (mAvoidingDirection == LEFT) {
                            mCommunicator.setOutgoing(MessageEncoderDecoder.turnRight(MessageEncoderDecoder.DEFAULT_VELOCITY + 25, MessageEncoderDecoder.TURN_ON_SPOT));
                        } else {
                            mCommunicator.setOutgoing(MessageEncoderDecoder.turnLeft(MessageEncoderDecoder.DEFAULT_VELOCITY + 30, MessageEncoderDecoder.TURN_ON_SPOT));
                        }
                        checkPhaseTimeElapsed(mDefaultlPhaseTimeSet);
                        break;
                    case 4:
                }
            } else {
                UpdateDirections.getInstance(mRobotActivity).unlock();
                //Target Found
                if (mDistance != 0 && mDistance < 30 && mTargetInSight) {
                    if (mTargetFound) {
                        mCheer = true;
                        mIsMovingForward = false;
                        mLastTargetCenter = null;
                        mTurnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
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
                    mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
                    mIsMovingForward = false;
                    mLastTargetCenter = null;
                    mTurnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
                    startAvoidingPhase(true);
                    return;
                }
                if (!mTargetInSight) {
                    Log.i(TAG, "None Object at in sight. Searching.");
                    mCommunicator.setOutgoing(MessageEncoderDecoder.search());
                    mIsMovingForward = false;
                    mLastTargetCenter = null;
                    mTurnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
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
                            mTurnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
                        //TODO send rover in emergency mode cause it's stuck
                    }
                    //target not aimed
                    if (mTargetDirection == TARGET_POSITION_LEFT) {
                        if (mIsMovingForward) {
                            mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY, MessageEncoderDecoder.DEFAULT_VELOCITY + 20));
                        } else {
                            mCommunicator.setOutgoing(MessageEncoderDecoder.turnLeft(mTurnVelocity, MessageEncoderDecoder.TURN_NORMALLY));
                            mIsMovingForward = false;
                        }
                    }
                    if (mTargetDirection == TARGET_POSITION_RIGHT) {
                        if (mIsMovingForward) {
                            mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY + 20, MessageEncoderDecoder.DEFAULT_VELOCITY));
                        } else {
                            mCommunicator.setOutgoing(MessageEncoderDecoder.turnRight(mTurnVelocity, MessageEncoderDecoder.TURN_NORMALLY));
                            mIsMovingForward = false;
                        }
                    }
                } else {// Target aimed
                    mTurnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
                    mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY, MessageEncoderDecoder.DEFAULT_VELOCITY));
                    mIsMovingForward = true;
                }
                mLastTargetCenter = mTargetCenter;
            }
        } else {
            mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
            stopAvoidingPhase();
            mIsMovingForward = false;
            mLastTargetCenter = null;
            mTurnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
        }
    }

    private void startAvoidingPhase(boolean newDirection) {
        mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
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
                mCurrentPhaseTime += mDefaultlPhaseTimeSet / 5;
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

    private void decodeMessage() {
        DecodedMessage incomingMessage = MessageEncoderDecoder.decodeIncomingMessage(mCommunicator.getIncoming());
        if (!incomingMessage.hasError()) {
            if (incomingMessage.isInfoMessage() && incomingMessage.hasDistance()) {
                mDistance = incomingMessage.getData();
            }
            if (incomingMessage.isTerminateCommand()) {
                mRobotActivity.finish(); //TODO da eliminare, ho tolto il pulsante su Arduino. Controllare e eliminare
            }
        }
    }

    public void frameWidth(int width) {
        mFrameWidth = width;
    }

    public void frameHeight(int height) {
        frameHeight = height;
    }
}

