package me.mariotti.tanker.messaging;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import me.mariotti.tanker.R;
import me.mariotti.tanker.TankActivity;
import me.palazzetti.adktoolkit.AdkManager;

import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;


/**
 * Created by simone on 30/10/14.
 */
public class Communicator extends AsyncTask<TankActivity, String, Void> {
    private final String TAG = "Communicator";
    private final TankActivity mActivity;
    private final int DELAY = 50;
    private TextView mLogTextView;
    private String mOutgoing = "0", mLastSent = "", mLastReceived = "";
    private AdkManager mArduino;
    boolean mKeepAlive = true;
    public IncomingMessage mIncomingMessageObservable;

    public Communicator(TankActivity mActivity) {
        mArduino = mActivity.mArduino;
        this.mActivity = mActivity;
        mLogTextView = (TextView) mActivity.findViewById(R.id.LogTextView);
        mIncomingMessageObservable = new IncomingMessage();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        int lenght = mLogTextView.getText().length();
        if (lenght < 10000) {
            mLogTextView.append(values[0] + '\n');
        } else {
            mLogTextView.setText(mLogTextView.getText().subSequence(lenght - 5000, lenght));
        }
    }

    @Override
    protected Void doInBackground(TankActivity... params) {
        DateFormat mDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        while (mKeepAlive) {
            try {
                // Sending phase
                String mSending = getOutgoing();
                if (!mLastSent.equals(mSending)) {
                    Date mDate = new Date();
                    publishProgress(mDateFormat.format(mDate) + " | Sending: " + mSending);
                    mLastSent = mSending;
                }
                //sending the last command until it changes
                mArduino.writeSerial(mSending);
                // this.wait(5);
                // Receiving phase
                //FileInputStream a= new FileInputStream("");

                String mReceiving = mArduino.readString();
                if (!mReceiving.equals("") && !mReceiving.equals(mLastReceived)) {
                    setIncoming(mReceiving);
                    mLastReceived = mReceiving;
                    Date mDate = new Date();
                    publishProgress(mDateFormat.format(mDate) + " | Receiving: " + mReceiving);
                }
                Thread.sleep(DELAY);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
        return null;
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