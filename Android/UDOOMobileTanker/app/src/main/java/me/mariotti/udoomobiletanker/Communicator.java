package me.mariotti.udoomobiletanker;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import me.palazzetti.adktoolkit.AdkManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;


/**
 * Created by simone on 30/10/14.
 */
public class Communicator extends AsyncTask<TankActivity, String, Void> {
    private final String TAG = "Communicator";
    //private TankActivity mActivity;
    private TextView mLogTextView;
    private String mIncoming, mOutgoing = "0", mLastIncome, mLastSent = "";
    private AdkManager mArduino;
    boolean mKeepAlive = true;
    IncomingMessage mIncomingMessageObservable;

    public Communicator(TankActivity mActivity) {
        //this.mActivity = mActivity;
        mArduino = mActivity.mArduino;
        mLogTextView = (TextView) mActivity.findViewById(R.id.LogTextView);
        mIncomingMessageObservable = new IncomingMessage();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mLogTextView.append(values[0] + '\n');
    }

    @Override
    protected Void doInBackground(TankActivity... params) {
        DateFormat mDateFormat = new SimpleDateFormat("HH:mm:ss.S");
        while (mKeepAlive) {
            try {
                String mSending = getOutgoing();
                if (!mLastSent.equals(mSending)) {
                    //get current date time with Date()
                    Date mDate = new Date();
                    publishProgress(mDateFormat.format(mDate) + " | Sending: " + mSending);
                    mLastSent = mSending;
                }
                mArduino.writeSerial(mSending);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
        return null;
    }

    //This is just an Observable wrapper for the incoming message
    class IncomingMessage extends Observable {
        private String message = "";

        public void setIncoming(String incoming) {
            message = incoming;
            triggerObservers();
        }
        public String getIncoming(){
            return message;
        }

        private void triggerObservers() {
            setChanged();
            notifyObservers();
        }

    }



    synchronized public String getIncoming() {
        return mIncomingMessageObservable.getIncoming();
    }

    synchronized private String getOutgoing() {
        return mOutgoing;
    }

    synchronized private void setIncoming(String incoming) {
        mIncomingMessageObservable.setIncoming(incoming);
    }

    synchronized public void setOutgoing(String outgoing) {
        this.mOutgoing = outgoing;
    }

    public void setKeepAlive(boolean mKeepAlive) {
        this.mKeepAlive = mKeepAlive;
    }
}