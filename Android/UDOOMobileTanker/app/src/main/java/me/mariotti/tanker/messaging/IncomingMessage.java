package me.mariotti.tanker.messaging;

import java.util.Observable;

//This is just an Observable wrapper for the incoming message
public class IncomingMessage extends Observable {
    private String message = "";

    protected void setIncoming(String incoming) {
        message = incoming;
        triggerObservers();
    }

    public String getIncoming() {
        return message;
    }

    private void triggerObservers() {
        setChanged();
        notifyObservers();
    }

}