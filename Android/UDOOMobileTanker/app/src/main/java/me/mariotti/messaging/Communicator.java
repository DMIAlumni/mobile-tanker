package me.mariotti.messaging;


import me.palazzetti.adktoolkit.AdkManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Communicator {
    private final String TAG = "Communicator";
    private final int READING_POLLING_TIME = 50; // should be equal the delay set on Arduino

    private AdkManager mArduino;
    private ScheduledExecutorService mScheduler;
    private String mOutgoing = MessageEncoder.stop();
    private String mLastReceived = "";

    public Communicator(AdkManager adkManager) {
        this.mArduino = adkManager;
    }

    public void start() {
        CommunicationThread thread = new CommunicationThread();
        mScheduler = Executors.newSingleThreadScheduledExecutor();
        mScheduler.scheduleAtFixedRate(thread, 0, READING_POLLING_TIME, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        mScheduler.shutdown();
    }

    synchronized public void setOutgoing(String outgoing) {
        this.mOutgoing = outgoing;
    }

    private class CommunicationThread implements Runnable {

        @Override
        public void run() {
            // Sending phase
            mArduino.write(mOutgoing);

            // Receiving phase
            String mReceiving = mArduino.read().getString();
            if (!mReceiving.equals("") && !mReceiving.equals(mLastReceived)) {
                IncomingMessage.getInstance().setIncoming(mReceiving);
                mLastReceived = mReceiving;
            }
        }
    }
}
