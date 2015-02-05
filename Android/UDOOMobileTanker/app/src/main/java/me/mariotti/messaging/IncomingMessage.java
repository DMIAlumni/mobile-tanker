package me.mariotti.messaging;

import me.mariotti.exceptions.DeserializationException;
import java.util.Observable;

/**
 * This is just an Observable wrapper for the incoming message
 */
public class IncomingMessage extends Observable {
    private static IncomingMessage mInstance = null;
    private static ArduinoMessage mMessage = null;

    private IncomingMessage() {
        // noop
    }

    public static synchronized IncomingMessage getInstance() {
        if (mInstance == null) {
            mInstance = new IncomingMessage();
        }
        return mInstance;
    }

    protected void setIncoming(String incoming) {
        try {
            mMessage = new ArduinoMessage(incoming);
            setChanged();
            notifyObservers();
        } catch (DeserializationException e) {
            mMessage = null;
            // log exception
        }
    }

    public ArduinoMessage getIncoming() {
        return mMessage;
    }
}
