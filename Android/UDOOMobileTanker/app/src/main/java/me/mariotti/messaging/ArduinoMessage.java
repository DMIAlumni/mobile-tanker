package me.mariotti.messaging;


import me.mariotti.exceptions.DeserializationException;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Message pattern sent by Arduino: (mMsgType,mMsg,mData)
 */
public class ArduinoMessage {
    // Message types
    private final static int INFO = 0;
    private final static int STATE = 1;

    // States
    private final static int IDLE = 100;
    private final static int SEARCHING = 101;
    private final static int HUNTING = 102;
    private final static int EMERGENCY = 103;

    // Infos
    private final static int DISTANCE = 203;
    private final static int TERMINATE = 999;

    private int mMsgType;
    private int mMsg;
    private int mData;
    private int mError;

    public ArduinoMessage(int msgType, int msg, int mData, int mError) {
        this.mMsgType = msgType;
        this.mMsg = msg;
        this.mData = mData;
        this.mError = mError;
    }

    public ArduinoMessage(String mIncomingMessage) throws DeserializationException {
        try {
            // Instance deserialization
            ArrayList<String> splittedMessage = new ArrayList<>(Arrays.asList(mIncomingMessage.split(",")));

            this.mMsgType = Integer.valueOf(splittedMessage.get(0));
            this.mMsg = Integer.valueOf(splittedMessage.get(1));
            this.mData = Integer.valueOf(splittedMessage.get(2));
            this.mError = 0;
        } catch (IndexOutOfBoundsException e) {
            throw new DeserializationException();
        }
    }

    private int getMsgType() {
        return mMsgType;
    }

    private int getMsg() {
        return mMsg;
    }

    public int getData() {
        return mData;
    }

    public int getError() {
        return mError;
    }

    public boolean isInfoMessage() {
        return mMsgType == INFO;
    }

    public boolean isStateMessage() {
        return mMsgType == STATE;
    }

    public boolean isTerminateCommand() {
        return mMsgType == INFO && mMsg == TERMINATE;
    }

    public boolean hasDistance() {
        return mMsg == DISTANCE;
    }

    public boolean isEmergency() {
        return mMsg == EMERGENCY;
    }

    public boolean isSearching() {
        return mMsg == SEARCHING;
    }

    public boolean isHunting() {
        return mMsg == HUNTING;
    }

    public boolean hasError() {
        return mError != 0;
    }
}
