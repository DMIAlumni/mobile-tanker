package me.mariotti.tanker;

import me.mariotti.tanker.messaging.Communicator;
import me.mariotti.tanker.messaging.MessageEncoderDecoder;
import org.opencv.core.Point;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class TankLogic implements Observer {
    public final static int TARGET_POSITION_FRONT = 3;
    public final static int TARGET_POSITION_LEFT = 1;
    public final static int TARGET_POSITION_RIGHT = 2;
    public final static int TARGET_POSITION_NONE = 0;
    private final String TAG = "TankLogic";
    private Communicator mCommunicator;
    private String incomingMessageTemp;
    private HashMap<String, Integer> incomingMessage;
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


    public TankLogic(Communicator mCommunicator) {
        this.mCommunicator = mCommunicator;
        mCommunicator.mIncomingMessageObservable.addObserver(this);
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

        if (!targetInSight) {
            mCommunicator.setOutgoing(MessageEncoderDecoder.search());
            lastTargetCenter = null;
            return;
        }
        //If target is not correctly aimed
        if (targetCenter.x < frameWidth / 2 - targetWidth / 2 || targetCenter.x > frameWidth / 2 + targetWidth / 2) {
            if (isNotMoving()) {
                turnVelocity += velocityStep;
            }
            if (targetDirection == TARGET_POSITION_LEFT) {
                mCommunicator.setOutgoing(MessageEncoderDecoder.turnLeft(turnVelocity));
            }
            if (targetDirection == TARGET_POSITION_RIGHT) {
                mCommunicator.setOutgoing(MessageEncoderDecoder.turnRight(turnVelocity));
            }
        } else {
            turnVelocity = MessageEncoderDecoder.DEFAULT_VELOCITY;
            mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
        }
        lastTargetCenter = targetCenter;
    }

    private boolean isNotMoving() {
        return lastTargetCenter!=null && Math.abs(targetCenter.x-lastTargetCenter.x)<10;
    }

    @Override
    public void update(Observable observable, Object data) {
        decodeMessage();
    }

    private void decodeMessage() {
        incomingMessage = MessageEncoderDecoder.decodeIncomingMessage(mCommunicator.getIncoming());
        if (incomingMessage.get("ERROR") != -1) {
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
