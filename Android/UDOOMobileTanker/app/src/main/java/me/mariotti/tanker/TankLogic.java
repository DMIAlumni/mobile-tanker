package me.mariotti.tanker;

import android.util.Log;
import me.mariotti.tanker.messaging.Communicator;
import me.mariotti.tanker.messaging.DecodedMessage;
import me.mariotti.tanker.messaging.MessageEncoderDecoder;
import org.opencv.core.Point;

import java.util.Observable;
import java.util.Observer;

public class TankLogic implements Observer {
    public final static int TARGET_POSITION_FRONT = 3;
    public final static int TARGET_POSITION_LEFT = 1;
    public final static int TARGET_POSITION_RIGHT = 2;
    public final static int TARGET_POSITION_NONE = 0;
    public final static int LEFT = 0;
    public final static int RIGHT = 0;
    private final String TAG = "TankLogic";
    private Communicator mCommunicator;
    private TankActivity mTankActivity;
    private String incomingMessageTemp;
    private DecodedMessage incomingMessage;
    private Boolean targetInSight = false;
    private int targetDirection;
    private Point targetCenter;
    private Point lastTargetCenter;
    private int targetWidth;
    private int targetHeight;
    private int distanceFromCenter;
    private int frameHeight;
    private int frameWidth;
    private int turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
    private int velocityStep = 1;
    private boolean isMovingForward;
    private int distance = Integer.MAX_VALUE;
    private boolean isAroundingObstacle = false;
    private int aroundingDirection;
    private int aroundingPhase = -1;
    private long phaseTime = -1;
    private int phaseLength = 1000;


    public TankLogic(Communicator mCommunicator, TankActivity tankActivity) {
        this.mCommunicator = mCommunicator;
        mTankActivity = tankActivity;
        mCommunicator.mIncomingMessageObservable.addObserver(this);
    }

    @Override
    public void update(Observable observable, Object data) {
        decodeMessage();
    }

    public void targetPosition(int position) {
        targetInSight = position != TARGET_POSITION_NONE;
        targetDirection = position;
        think();
    }

    public void targetCenter(Point point) {
        targetCenter = point;
    }

    public void targetWidth(int width) {
        targetWidth = width;
    }

    public void targetHeight(int height) {
        targetHeight = height;
    }

    private void think() {
        // Stop robot with an object within 5cm
        if (distance != 0 && distance < 5) {
            deleteObstacle();
            Log.i(TAG, "Object at " + distance + "cm. Stopped.");
            mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
            isMovingForward = false;
            lastTargetCenter = null;
            turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
            return;
        }

        if (isAroundingObstacle) {
            //While arounding the obstacle he see the target
            if (targetInSight) {
                Log.i(TAG, "Target seen while arounding the obstacle");
                mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
                deleteObstacle();
                return;
            }
            //Obstacle on my way
           /* if (distance != 0 && distance < 30 && !targetInSight) {
                Log.i(TAG, "Another obstacle at " + distance + "cm. Starting arounding process.");
                resetObstacle();

            }
           */
            Log.i(TAG, "Phase: " + aroundingPhase);

            switch (aroundingPhase) {
                case 1:
                    if (phaseTime == -1) {
                        phaseTime = System.currentTimeMillis();
                    }
                    if (aroundingDirection == LEFT) {
                        mCommunicator.setOutgoing(MessageEncoderDecoder.turnLeft(MessageEncoderDecoder.DEFAULT_VELOCITY + 30, MessageEncoderDecoder.TURN_ON_SPOT));
                    } else {
                        mCommunicator.setOutgoing(MessageEncoderDecoder.turnRight(MessageEncoderDecoder.DEFAULT_VELOCITY + 25, MessageEncoderDecoder.TURN_ON_SPOT));
                    }
                    checkPhaseTimeElapsed(phaseLength);

                    break;
                case 2:
                    mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY, MessageEncoderDecoder.DEFAULT_VELOCITY));
                    checkPhaseTimeElapsed((int) (phaseLength / 1));
                    break;
                case 3:
                    if (aroundingDirection == LEFT) {
                        mCommunicator.setOutgoing(MessageEncoderDecoder.turnRight(MessageEncoderDecoder.DEFAULT_VELOCITY + 25, MessageEncoderDecoder.TURN_ON_SPOT));
                    } else {
                        mCommunicator.setOutgoing(MessageEncoderDecoder.turnLeft(MessageEncoderDecoder.DEFAULT_VELOCITY + 30, MessageEncoderDecoder.TURN_ON_SPOT));
                    }
                    checkPhaseTimeElapsed(phaseLength);
                    break;
                case 4:


            }

        } else {
            //Target Found
            if (distance != 0 && distance < 30 && targetInSight) {
                Log.i(TAG, "Target at " + distance + "cm. FOUND.");
                mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
                isMovingForward = false;
                lastTargetCenter = null;
                turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
                return;
            }
            //Obstacle on my way
            if (distance != 0 && distance < 30 && !targetInSight) {
                Log.i(TAG, "Obstacle at " + distance + "cm. Starting arounding orocess.");
                mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
                isMovingForward = false;
                lastTargetCenter = null;
                turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
                startAvoidingPhase(true);
                return;
            }
            if (!targetInSight) {
                Log.i(TAG, "None Object at in sight. Searching.");
                mCommunicator.setOutgoing(MessageEncoderDecoder.search());
                isMovingForward = false;
                lastTargetCenter = null;
                turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
                return;
            }
            //if target is thinner than 100px consider it 100px width
            targetWidth = targetWidth < 100 ? 100 : targetWidth;
            // Target in sight
            if (targetCenter.x < frameWidth / 2 - targetWidth / 2 || targetCenter.x > frameWidth / 2 + targetWidth / 2) {
                Log.i(TAG, "Target insight");
                //TODO add a minimum width of target
                //power up velocity if since last frame we didn't move, and wen we start moving maintain it since aim OK
                if (isNotTurning()) {
                    turnVelocity += velocityStep;
                    if (turnVelocity > 255 - velocityStep)
                        turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
                    //TODO send rover in emergency mode cause it's stuck
                }
                //target not aimed
                if (targetDirection == TARGET_POSITION_LEFT) {
                    if (isMovingForward) {
                        mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY, MessageEncoderDecoder.DEFAULT_VELOCITY + 20));
                    } else {
                        mCommunicator.setOutgoing(MessageEncoderDecoder.turnLeft(turnVelocity, MessageEncoderDecoder.TURN_NORMALLY));
                        isMovingForward = false;
                    }
                }
                if (targetDirection == TARGET_POSITION_RIGHT) {
                    if (isMovingForward) {
                        mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY + 20, MessageEncoderDecoder.DEFAULT_VELOCITY));
                    } else {
                        mCommunicator.setOutgoing(MessageEncoderDecoder.turnRight(turnVelocity, MessageEncoderDecoder.TURN_NORMALLY));
                        isMovingForward = false;
                    }
                }
            } else {// Target aimed
                turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
                mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY, MessageEncoderDecoder.DEFAULT_VELOCITY));
                isMovingForward = true;
            }
            lastTargetCenter = targetCenter;
        }
    }

    private void deleteObstacle() {
        phaseTime = aroundingPhase = -1;
        isAroundingObstacle = false;
    }

    private void resetObstacle() {
        if (aroundingPhase == 2) {
//            phaseTime += phaseLength / 1.2;
        } else {
            phaseTime += phaseLength;
        }
    }

    private void startAvoidingPhase(boolean newDirection) {
        mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
        isAroundingObstacle = true;
        aroundingPhase = 1;
        if (newDirection) {
            aroundingDirection = Math.random() >= 0.5 ? LEFT : RIGHT;
        }
    }

    private boolean isNotTurning() {
        return lastTargetCenter != null && Math.abs(targetCenter.x - lastTargetCenter.x) < 10;
    }

    private void decodeMessage() {
        incomingMessage = MessageEncoderDecoder.decodeIncomingMessage(mCommunicator.getIncoming());
        if (!incomingMessage.hasError()) {
            if (incomingMessage.isInfoMessage() && incomingMessage.hasDistance()) {
                distance = incomingMessage.getData();
            }
            if (incomingMessage.isTerminateCommand()) {
                mTankActivity.finish();

            }
            // Do things
            //mCommunicator.setOutgoing(incomingMessage.toString());
        }
        //mCommunicator.setOutgoing("Ricevuto da Arudino"+mCommunicator.getIncoming());
    }

    public void frameWidth(int width) {
        frameWidth = width;
    }

    public void frameHeight(int height) {
        frameHeight = height;
    }

    void checkPhaseTimeElapsed(long thresholdPhaseTime) {
        if (System.currentTimeMillis() - phaseTime > thresholdPhaseTime) {
            //se c'è ancora un ostacolo a meno di 30 centimetri
            if (distance != 0 && distance < 30 && aroundingPhase != 2) {
                phaseTime += phaseLength / 5;
            } else {
                aroundingPhase++;
                phaseTime = System.currentTimeMillis();
                if (aroundingPhase > 3) {
                    isAroundingObstacle = false;
                    aroundingPhase = -1;
                    phaseTime = -1;
                }
            }
        }
    }
}
