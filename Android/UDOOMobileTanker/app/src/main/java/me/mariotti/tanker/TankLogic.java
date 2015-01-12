package me.mariotti.tanker;

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
    private boolean isAvoidingObstacle=false;
    private int aroundingDirection;
    private int avoidingPhase=-1;


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
        if (isAvoidingObstacle){
            int timing=600;
            switch (avoidingPhase){
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:


            }
        }
        // Stop robot with an object within 5cm
        if (distance != 0 && distance < 5) {
            mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
            isMovingForward = false;
            lastTargetCenter = null;
            turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
            return;
        }
        //Target Found
        if (distance != 0 && distance < 30 && targetInSight) {
            mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
            isMovingForward = false;
            lastTargetCenter = null;
            turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
            return;
        }
        //Obstacle on my way
        if (distance != 0 && distance < 30 && !targetInSight) {
            mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
            isMovingForward = false;
            lastTargetCenter = null;
            turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
            startAvoidingPhase();
            return;
        }
        if (!targetInSight) {
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
                    mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY, MessageEncoderDecoder.DEFAULT_VELOCITY+20));
                } else {
                    mCommunicator.setOutgoing(MessageEncoderDecoder.turnLeft(turnVelocity, MessageEncoderDecoder.TURN_NORMALLY));
                    isMovingForward = false;
                }
            }
            if (targetDirection == TARGET_POSITION_RIGHT) {
                if (isMovingForward) {
                    mCommunicator.setOutgoing(MessageEncoderDecoder.moveForward(MessageEncoderDecoder.DEFAULT_VELOCITY+20, MessageEncoderDecoder.DEFAULT_VELOCITY));
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

    private void startAvoidingPhase() {
        isAvoidingObstacle=true;
        aroundingDirection=Math.random()>=0.5?LEFT:RIGHT;
        avoidingPhase=1;
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
            if (incomingMessage.isTerminateCommand()){
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
}
