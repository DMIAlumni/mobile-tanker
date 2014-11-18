package me.mariotti.tanker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by simone on 18/11/14.
 */
public class Movement {
    // Android --> Arduino codes
    private final static int CMD_NULL_VALUE = 0;
    private final static int CMD_MOVE = 1;
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


    // Android --> Arduino messages

    public static String move(int motorLeft, int motorRight) {
        return (CMD_MOVE + "," + motorLeft + "," + motorRight);
    }

    public static String stop() {
        return (CMD_STOP + "," + CMD_NULL_VALUE + "," + CMD_NULL_VALUE);
    }

    public static String stop(long stopTime) {
        return (CMD_STOP + "," + stopTime + "," + CMD_NULL_VALUE);
    }

    public static String turnLeft() {
        return (CMD_LEFT + "," + CMD_NULL_VALUE + "," + CMD_NULL_VALUE);
    }

    public static String turnRight() {
        return (CMD_RIGHT + "," + CMD_NULL_VALUE + "," + CMD_NULL_VALUE);
    }

    public static String shoot() {
        return (CMD_SHOOT + "," + CMD_NULL_VALUE + "," + CMD_NULL_VALUE);
    }

    public static String search() {
        return (CMD_SEARCH + "," + CMD_NULL_VALUE + "," + CMD_NULL_VALUE);
    }

    // Arduino --> Android messages
    public static Map<String, Integer> decodeIncomingMessage(String mIncomingMessage) {
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
        return parsedMessage;
    }
}