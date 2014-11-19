package me.mariotti.tanker;

import me.mariotti.tanker.messaging.Communicator;
import me.mariotti.tanker.messaging.MessageEncoderDecoder;

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


    public TankLogic(Communicator mCommunicator) {
        this.mCommunicator = mCommunicator;
        mCommunicator.mIncomingMessageObservable.addObserver(this);
    }

    public void targetPosition(int position) {
        targetInSight = position != TARGET_POSITION_NONE;
        targetDirection = position;
        think();
    }

    private void think() {
        switch (targetDirection) {
            case TARGET_POSITION_LEFT:
                mCommunicator.setOutgoing(MessageEncoderDecoder.turnLeft());
                break;
            case TARGET_POSITION_RIGHT:
                mCommunicator.setOutgoing(MessageEncoderDecoder.turnRight());
                break;
            case TARGET_POSITION_FRONT:
                mCommunicator.setOutgoing(MessageEncoderDecoder.stop());
                break;
            case TARGET_POSITION_NONE:
                mCommunicator.setOutgoing(MessageEncoderDecoder.search());
                break;
        }
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
}
