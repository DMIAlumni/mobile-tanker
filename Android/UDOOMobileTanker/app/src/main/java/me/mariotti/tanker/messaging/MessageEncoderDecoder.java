package me.mariotti.tanker.messaging;

import android.text.TextUtils;
import android.util.Log;
import me.mariotti.tanker.TankActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MessageEncoderDecoder {
    private final static String TAG = "MessageEncoderDecoder";
    // Android --> Arduino codes
    private final static int CMD_NULL_VALUE = 0;
    private final static int CMD_MOVE_FORWARD = 1;
    private final static int CMD_MOVE_BACKWARD = 6;
    private final static int CMD_STOP = 2;
    private final static int CMD_LEFT = 3;
    private final static int CMD_RIGHT = 4;
    private final static int CMD_SHOOT = 5;
    private final static int CMD_SEARCH = 10;

    //modes
    public final static int TURN_ON_SPOT = 1;
    public final static int TURN_NORMALLY = 2;

    // Arduino --> Aandroid codes
    // Message types
    protected final static int INFO = 0;
    protected final static int STATE = 1;
    // States
    private final static int IDLE = 100;
    public final static int SEARCHING = 101;
    public final static int HUNTING = 102;
    public final static int EMERGENCY = 103;
    // Actions
    private final static int STOPPED = 150;
    private final static int MOVING = 151;
    // Infos
    public final static int DISTANCE = 203;
    public final static int TERMINATE = 999;

    // Defaults
    public final static int DEFAULT_VELOCITY = 135;

    // Android --> Arduino messages

    public static String moveForward(int motorLeft, int motorRight) {
        return (CMD_MOVE_FORWARD + "," + motorLeft + "," + motorRight);
    }

    public static String moveForward() {
        return moveForward(DEFAULT_VELOCITY, DEFAULT_VELOCITY);
    }

    public static String moveBackward(int motorLeft, int motorRight) {
        return (CMD_MOVE_BACKWARD + "," + motorLeft + "," + motorRight);
    }

    public static String moveBackward() {
        return moveBackward(DEFAULT_VELOCITY, DEFAULT_VELOCITY);
    }

    public static String stop() {
        return (CMD_STOP + "," + CMD_NULL_VALUE + "," + CMD_NULL_VALUE);
    }

    public static String turnLeft(int velocity, int mode) {
        return (CMD_LEFT + "," + velocity + "," + mode);
    }

    public static String turnLeft(int mode) {
        return turnLeft(DEFAULT_VELOCITY, mode);
    }

    public static String turnRight(int velocity, int mode) {
        return (CMD_RIGHT + "," + velocity + "," + mode);
    }

    public static String turnRight(int mode) {
        return turnRight(DEFAULT_VELOCITY, mode);
    }

    public static String search() {
        return (CMD_SEARCH + "," + CMD_NULL_VALUE + "," + CMD_NULL_VALUE);
    }

    public static DecodedMessage decodeIncomingMessage(String mIncomingMessage) {
        if (!TextUtils.isEmpty(mIncomingMessage)) {
            ArrayList<String> mSplittedMessage = new ArrayList<>(Arrays.asList(mIncomingMessage.split(",")));
            return new DecodedMessage(mSplittedMessage);
        }
        return new DecodedMessage(-1, -1, -1, -1);
    }
}