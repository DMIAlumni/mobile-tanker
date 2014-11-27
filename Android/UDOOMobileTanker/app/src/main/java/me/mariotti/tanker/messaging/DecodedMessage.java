package me.mariotti.tanker.messaging;


import java.util.ArrayList;

//Message pattern: msgType,msg,data
public class DecodedMessage {
    private int msgType;
    private int msg;
    private int data;
    private int error;

    public DecodedMessage(int msgType, int msg, int data, int error) {
        this.msgType = msgType;
        this.msg = msg;
        this.data = data;
        this.error = error;
    }

    public DecodedMessage(ArrayList<String> splittedMessage) {
        this.msgType=Integer.valueOf(splittedMessage.get(0));
        this.msg=Integer.valueOf(splittedMessage.get(1));
        this.data=Integer.valueOf(splittedMessage.get(2));
        this.error=0;
    }

    private int getMsgType() {
        return msgType;
    }

    private int getMsg() {
        return msg;
    }

    public int getData() {
        return data;
    }

    public int getError() {
        return error;
    }

    public boolean isInfoMessage() {
        return msgType == MessageEncoderDecoder.INFO;
    }

    public boolean isStateMessage() {
        return msgType == MessageEncoderDecoder.STATE;
    }

    public boolean hasDistance() {
        return msg == MessageEncoderDecoder.DISTANCE;
    }

    public boolean EmergencyMode() {
        return msg == MessageEncoderDecoder.EMERGENCY;
    }

    public boolean SearchingMode() {
        return msg == MessageEncoderDecoder.SEARCHING;
    }

    public boolean HuntingMode() {
        return msg == MessageEncoderDecoder.HUNTING;
    }
    public boolean hasError(){
        return error!=0;
    }
}
