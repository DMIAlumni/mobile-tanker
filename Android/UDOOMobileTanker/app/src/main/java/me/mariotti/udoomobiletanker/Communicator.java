package me.mariotti.udoomobiletanker;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import me.palazzetti.adktoolkit.AdkManager;

import java.io.InputStream;

/**
 * Created by simone on 30/10/14.
 */
public class Communicator extends AsyncTask<Void, Void, Void> {
    private OpenCVActivity mActivity;
    private String mIncoming, mOutocoming;
    private AdkManager mArduino;
    boolean mKeepAlive = true;

    public Communicator(OpenCVActivity mActivity) {
        this.mActivity = mActivity;
        mArduino = mActivity.arduino;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected Void doInBackground(Void... params) {
        while (mKeepAlive) {
            try {
                mArduino.writeSerial("1");
            } catch (Exception ex) {
                Log.e("TEST", ex.getMessage());
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
}
