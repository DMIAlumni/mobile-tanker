package me.mariotti.udoomobiletanker;

import android.os.AsyncTask;
import android.util.Log;
import me.palazzetti.adktoolkit.AdkManager;


/**
 * Created by simone on 30/10/14.
 */
public class Communicator extends AsyncTask<TankerActivity, String, Void> {
    private final String TAG = "Communicator";
    private TankerActivity mActivity;
    private String mIncoming, mOutocoming;
    private AdkManager mArduino;
    boolean mKeepAlive = true;

    public Communicator(TankerActivity mActivity) {
        this.mActivity = mActivity;
        mArduino = mActivity.mArduino;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected Void doInBackground(TankerActivity... params) {
        while (mKeepAlive) {
            try {
                mArduino.writeSerial(getOutocoming());
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