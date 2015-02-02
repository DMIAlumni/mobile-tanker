package me.mariotti.tanker.messaging;


import java.util.ArrayList;

//Message pattern: mMsgType,mMsg,mData
public class DecodedMessage {
    private int mMsgType;
    public int mMsg;
    private int mData;
    private int mError;

    public DecodedMessage(int msgType, int msg, int mData, int mError) {
        this.mMsgType = msgType;
        this.mMsg = msg;
        this.mData = mData;
        this.mError = mError;
    }

    public DecodedMessage(ArrayList<String> splittedMessage) {
        this.mMsgType = Integer.valueOf(splittedMessage.get(0));
        this.mMsg = Integer.valueOf(splittedMessage.get(1));
        this.mData = Integer.valueOf(splittedMessage.get(2));
        this.mError = 0;
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

    public int getmError() {
        return mError;
    }

    public boolean isInfoMessage() {
        return mMsgType == MessageEncoderDecoder.INFO;
    }

    public boolean isStateMessage() {
        return mMsgType == MessageEncoderDecoder.STATE;
    }

    public boolean isTerminateCommand() {
        return mMsgType == MessageEncoderDecoder.INFO && mMsg == MessageEncoderDecoder.TERMINATE;
    }

    public boolean hasDistance() {
        return mMsg == MessageEncoderDecoder.DISTANCE;
    }

    public boolean EmergencyMode() {
        return mMsg == MessageEncoderDecoder.EMERGENCY;
    }

    public boolean SearchingMode() {
        return mMsg == MessageEncoderDecoder.SEARCHING;
    }

    public boolean HuntingMode() {
        return mMsg == MessageEncoderDecoder.HUNTING;
    }

    public boolean hasError() {
        return mError != 0;
    }
}
