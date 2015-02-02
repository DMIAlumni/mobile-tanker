package me.mariotti.messaging;

import java.util.Observable;

//This is just an Observable wrapper for the incoming message
public class IncomingMessage extends Observable {
    private String mMessage = "";

    protected void setIncoming(String mIncoming) {
        mMessage = mIncoming;
        triggerObservers();
    }

    public String getIncoming() {
        return mMessage;
    }

    private void triggerObservers() {
        setChanged();
        notifyObservers();
    }
}