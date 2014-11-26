package me.mariotti.tanker.messaging;

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

    // Arduino --> Aandroid codes
    // Message types
    private final static int INFO = 0;
    private final static int STATE = 1;
    // States
    private final static int IDLE = 100;
    private final static int SEARCHING = 101;
    private final static int HUNTING = 102;
    private final static int EMERGENCY = 103;
    // Actions
    private final static int STOPPED = 150;
    private final static int MOVING = 151;
    // Infos
    private final static int SHOOTED = 201;
    private final static int RELOADED = 202;
    private final static int DISTANCE = 203;

    // Defaults
    public final static int DEFAULT_VELOCITY = 120;

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

    public static String stop(long stopTime) {
        return (CMD_STOP + "," + stopTime + "," + CMD_NULL_VALUE);
    }

    public static String turnLeft(int velocity) {
        return (CMD_LEFT + "," + velocity + "," + CMD_NULL_VALUE);
    }

    public static String turnLeft() {
        return turnLeft(DEFAULT_VELOCITY);
    }

    public static String turnRight(int velocity) {
        return (CMD_RIGHT + "," + velocity + "," + CMD_NULL_VALUE);
    }

    public static String turnRight() {
        return turnRight(DEFAULT_VELOCITY);
    }

    public static String shoot() {
        return (CMD_SHOOT + "," + CMD_NULL_VALUE + "," + CMD_NULL_VALUE);
    }

    public static String search() {
        return (CMD_SEARCH + "," + CMD_NULL_VALUE + "," + CMD_NULL_VALUE);
    }

    // Arduino --> Android messages
    public static HashMap<String, Integer> decodeIncomingMessage(String mIncomingMessage) {
        if (TankActivity.DEBUG) {
            Log.i(TAG, "mIncomingMessage: " + mIncomingMessage);
        }
        HashMap<String, Integer> parsedMessage = new HashMap<String, Integer>(3);
        ArrayList<String> splittedMessage = new ArrayList<String>(Arrays.asList(mIncomingMessage.split(",")));
        int messageType = Integer.valueOf(splittedMessage.get(0));

        switch (messageType) {
            case INFO: {
                parsedMessage.put("MSG_TYPE", INFO);
                parsedMessage.put("INFO_MSG", Integer.valueOf(splittedMessage.get(1)));
                if (parsedMessage.get("INFO_MSG") == DISTANCE) {
                    parsedMessage.put("DISTANCE", Integer.valueOf(splittedMessage.get(2)));
                }
                parsedMessage.put("ERROR", 0);
                break;
            }
            case STATE: {
                parsedMessage.put("MSG_TYPE", STATE);
                parsedMessage.put("STATE", Integer.valueOf(splittedMessage.get(1)));
                if (parsedMessage.get("STATE") == HUNTING || parsedMessage.get("STATE") == SEARCHING) {
                    parsedMessage.put("ACTION", Integer.valueOf(splittedMessage.get(2)));
                }
                parsedMessage.put("ERROR", 0);
                break;
            }
            default: {
                parsedMessage.put("ERROR", -1);
            }
        }
        if (TankActivity.DEBUG) {
            Log.i(TAG, "parsedMessage: " + parsedMessage.toString());
        }
        return parsedMessage;
    }
}