package me.mariotti.udoomobiletanker;

import java.util.Observable;
import java.util.Observer;

/**
 * Created by simone on 31/10/14.
 */
public class TankLogic implements Observer {
    public final static int TARGET_POSITION_FRONT = 3;
    public final static int TARGET_POSITION_LEFT = 1;
    public final static int TARGET_POSITION_RIGHT = 2;
    public final static int TARGET_POSITION_NONE = 0;
    private final String CMD_LEFT="TURN LEFT";
    private final String CMD_RIGHT="TURN RIGHT";
    private final String CMD_SEARCH="NO TARGET IN SIGHT, SEARCH IT!";
    private final String CMD_FORWARD="GO AHEAD";
    private final String CMD_BACKWARD="GO BACK";
    private final String TAG = "TankLogic";
    private Communicator mCommunicator;
    private String incomingMessage;
    private Boolean targetInSight = false;
    private int targetDirection;


    public TankLogic(Communicator mCommunicator) {
        this.mCommunicator = mCommunicator;
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

    private void think() {
        switch (targetDirection){
            case TARGET_POSITION_LEFT:
                mCommunicator.setOutgoing(CMD_LEFT);
                break;
            case TARGET_POSITION_RIGHT:
                mCommunicator.setOutgoing(CMD_RIGHT);
                break;
            case TARGET_POSITION_FRONT:
                mCommunicator.setOutgoing(CMD_FORWARD);
                break;
            case TARGET_POSITION_NONE:
                mCommunicator.setOutgoing(CMD_SEARCH);
                break;
        }
    }

    private void decodeMessage() {

    }
}
