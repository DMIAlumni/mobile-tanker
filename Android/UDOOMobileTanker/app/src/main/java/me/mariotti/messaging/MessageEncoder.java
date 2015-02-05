package me.mariotti.messaging;

/**
 * Encodes messages sent to Arduino with the following pattern:
 * (command,param1,param2)
 */
public class MessageEncoder {
    // Command types
    private final static int CMD_NULL_VALUE = 0;
    private final static int CMD_MOVE_FORWARD = 1;
    private final static int CMD_MOVE_BACKWARD = 6;
    private final static int CMD_STOP = 2;
    private final static int CMD_LEFT = 3;
    private final static int CMD_RIGHT = 4;
    private final static int CMD_SEARCH = 10;

    // Modes
    public final static int TURN_ON_SPOT = 1;
    public final static int TURN_NORMALLY = 2;

    // Defaults
    public final static int DEFAULT_VELOCITY = 135;

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
}
