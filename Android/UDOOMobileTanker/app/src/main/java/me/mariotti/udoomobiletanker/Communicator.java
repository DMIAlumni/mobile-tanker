package me.mariotti.udoomobiletanker;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import me.palazzetti.adktoolkit.AdkManager;


/**
 * Created by simone on 30/10/14.
 */
public class Communicator extends AsyncTask<TankActivity, String, Void> {
    private final String TAG = "Communicator";
    private TankActivity mActivity;
    private TextView mLogTextView;
    private String mIncoming, mOutocoming = "0", mLastIncome, mLastOutcome = "";
    private AdkManager mArduino;
    boolean mKeepAlive = true;

    public Communicator(TankActivity mActivity) {
        this.mActivity = mActivity;
        mArduino = mActivity.mArduino;
        mLogTextView = (TextView) mActivity.findViewById(R.id.LogTextView);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        mLogTextView.append(values[0] + '\n');
    }

    @Override
    protected Void doInBackground(TankActivity... params) {
        //int d = 0;
        while (mKeepAlive) {
            /*if (d < 100) {
                d++;
                setOutocoming(String.valueOf(d));
            }*/
            try {
                String mCurrentOutcome = getOutocoming();
                if (!mLastOutcome.equals(mCurrentOutcome)) {
                    publishProgress("Sending: " + mCurrentOutcome);
                    mLastOutcome = mCurrentOutcome;
                }
                mArduino.writeSerial(mCurrentOutcome);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
        return null;
    }

    synchronized public String getIncoming() {
        return mIncoming;
    }

    synchronized private String getOutocoming() {
        return mOutocoming;
    }

    synchronized private void setIncoming(String incoming) {
        this.mIncoming = incoming;
    }

    synchronized public void setOutocoming(String outocoming) {
        this.mOutocoming = outocoming;
    }

    public void setmKeepAlive(boolean mKeepAlive) {
        this.mKeepAlive = mKeepAlive;
    }
}