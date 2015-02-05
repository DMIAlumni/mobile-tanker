package me.mariotti.exceptions;


public class DeserializationException extends Exception {

    public DeserializationException() {
        super("Unable to deserialize given message");
    }

    public DeserializationException(String message) {
        super(message);
    }
}
