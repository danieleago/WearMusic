package it.unipi.wearmusic;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import static android.content.Context.VIBRATOR_SERVICE;

public class SensorFragment extends Fragment implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final float SHAKE_THRESHOLD = 1.2f;
    private static final float DIRECTION_THRESHOLD_MIN = 0.6f;
    private static final float DIRECTION_THRESHOLD_MAX = 1f;
    private static final int DIRECTION_TIME_MS =700;
    private static final String INCREASE_VOLUME = "increase volume";
    private static final String DECREASE_VOLUME = "decrease volume";
    private static final String PLAY = "play";
    private static final String NEXT = "next";
    private static final String PREVIOUS = "previous";
    private static final String TITLE = "title";

    private static final String TAG = "WearMusic";
    private static GoogleApiClient mGoogleApiClient;

    private View mView;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mSensorType;
    private long mShakeTime = 0;

    Vibrator vibrator;
    long[] vibrationPattern = {0, 100, 300, 100};
    //-1 - don't repeat
    final int indexInPatternToRepeat = -1;

    public static SensorFragment newInstance(int sensorType , GoogleApiClient mGAC) {
        SensorFragment f = new SensorFragment();
        mGoogleApiClient = mGAC;

        // Supply sensorType as an argument
        Bundle args = new Bundle();
        args.putInt("sensorType", sensorType);
        f.setArguments(args);

        return f;
    }

    ImageButton.OnClickListener listener = new ImageButton.OnClickListener() {

        @Override
        public void onClick(View v)
        {
            int id = v.getId();
            ImageView img;

            switch (id){

                case R.id.Next: mView.setBackgroundColor(Color.rgb(100, 100, 0));
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                    sendCommand(NEXT);
                    break;

                case R.id.Previous:
                    mView.setBackgroundColor(Color.rgb(100, 0, 0));
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                    sendCommand(PREVIOUS);
                    break;
                case R.id.Play:
                    mView.setBackgroundColor(Color.rgb(0, 100, 100));
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                    sendCommand(PLAY);
                    break;

                case R.id.Minus:
                    mView.setBackgroundColor(Color.rgb(0, 0, 100));
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                    sendCommand(DECREASE_VOLUME);
                    break;
                case R.id.Plus:
                    mView.setBackgroundColor(Color.rgb(0, 100, 0));
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                    sendCommand(INCREASE_VOLUME);
                    break;


            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG,"on create");
        vibrator = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);

        if(mGoogleApiClient!=null && !mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();


        Bundle args = getArguments();
        if(args != null) {
            mSensorType = args.getInt("sensorType");
        }
            mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(mSensorType);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG,"on create view");
            mView = inflater.inflate(R.layout.button, container, false);
            ImageButton bn = (ImageButton)mView.findViewById(R.id.Next);
            ImageButton bpl = (ImageButton)mView.findViewById(R.id.Play);
            ImageButton bpr = (ImageButton)mView.findViewById(R.id.Previous);
            ImageButton bminus = (ImageButton)mView.findViewById(R.id.Minus);
            ImageButton bplus = (ImageButton)mView.findViewById(R.id.Plus);
            bn.setOnClickListener(listener);
            bpl.setOnClickListener(listener);
            bpr.setOnClickListener(listener);
            bminus.setOnClickListener(listener);
            bplus.setOnClickListener(listener);


        Log.i(TAG,"On view sensor fragment");
        sendCommand(TITLE);
       return mView;
    }


    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG,"on resume");
        if(mGoogleApiClient!=null && !mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL*5);
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i(TAG,"onPause");
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE )
        {
            return;
        }

        if(getView().getLeft() == 0)
        detectShake(event);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mShakeTime) > DIRECTION_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            float gForce = (float)Math.sqrt(gX*gX + gY*gY + gZ*gZ);

            //UP
            if(gX > DIRECTION_THRESHOLD_MIN && gX < DIRECTION_THRESHOLD_MAX && gForce < SHAKE_THRESHOLD ) {
                
                mView.setBackgroundColor(Color.rgb(100, 0, 0));
                sendCommand(PREVIOUS);
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
            }

            //DOWN
            else if(gX < (- DIRECTION_THRESHOLD_MIN) && gX > (- DIRECTION_THRESHOLD_MAX) && gForce < SHAKE_THRESHOLD ) {
                mView.setBackgroundColor(Color.rgb(100, 100, 0));
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                sendCommand(NEXT);
            }

            //LEFT
            else if(gY < (- DIRECTION_THRESHOLD_MIN) && gY > (- DIRECTION_THRESHOLD_MAX) && gForce < SHAKE_THRESHOLD) {

                mView.setBackgroundColor(Color.rgb(0, 100, 0));
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                sendCommand(INCREASE_VOLUME);
            }

            //RIGHT
            else if(gY > DIRECTION_THRESHOLD_MIN && gY < DIRECTION_THRESHOLD_MAX && gForce < SHAKE_THRESHOLD ) {

                mView.setBackgroundColor(Color.rgb(0, 0, 100));
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                sendCommand(DECREASE_VOLUME);
            }
            else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }



    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void sendCommand(String msg) {


        Log.i(TAG, "SEND MESSAGE");
        new SenderThread("/wear_message", msg).start();

    }

    class SenderThread extends Thread {
        String path;
        String msg;
        SenderThread(String p, String m) {
            path = p;
            msg = m;
        }
        public void run() {

            NodeApi.GetConnectedNodesResult nodes =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result =
                        Wearable.MessageApi.sendMessage(mGoogleApiClient,
                                node.getId(),
                                path,
                                msg.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.i(TAG, "Message sent to " + node.getDisplayName());
                } else {
                    Log.i(TAG, "Failure sending to " + node.getDisplayName());
                }
            }
        }
    }
}
