package me.mariotti.tanker.messaging;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import me.mariotti.tanker.R;
import me.mariotti.tanker.TankActivity;
import me.palazzetti.adktoolkit.AdkManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Communicator extends AsyncTask<TankActivity, String, Void> {
    private final String TAG = "Communicator";
    private final int DELAY = 50;
    private TextView mLogTextView;
    private String mOutgoing = MessageEncoderDecoder.stop(),
                   mLastSent = "",
                   mLastReceived = "";
    private AdkManager mArduino;
    private boolean mKeepAlive = true;
    public IncomingMessage mIncomingMessageObservable;

    public Communicator(TankActivity mActivity) {
        mArduino = mActivity.mArduino;
        mLogTextView = (TextView) mActivity.findViewById(R.id.LogTextView);
        mIncomingMessageObservable = new IncomingMessage();
        execute();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        if (TankActivity.DEBUG) {
            int length = mLogTextView.getText().length();
            if (length < 10000) {
                mLogTextView.append(values[0] + '\n');
            } else {
                mLogTextView.setText(mLogTextView.getText().subSequence(length - 5000, length));
            }
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

                // Receiving phase
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