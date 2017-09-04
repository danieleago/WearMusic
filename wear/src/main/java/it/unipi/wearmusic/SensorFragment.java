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
import android.support.wearable.view.WatchViewStub;
import android.util.FloatMath;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import static android.content.Context.VIBRATOR_SERVICE;

public class SensorFragment extends Fragment implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final float SHAKE_THRESHOLD = 1.2f;
    private static final float DIRECTION_THRESHOLD = 0.9f;
    private static final int DIRECTION_TIME_MS = 500;
    private static final float ROTATION_THRESHOLD = 5.0f;
    private static final int ROTATION_WAIT_TIME_MS = 700;

    private static final String COMMAND_KEY = "command";

    private static GoogleApiClient mGoogleApiClient;

    private View mView;
    private TextView mTextTitle;
    private TextView mTextValues;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mSensorType;
    private long mShakeTime = 0;
    private long mRotationTime = 0;

    public static SensorFragment newInstance(int sensorType , GoogleApiClient mGAC ) {
        SensorFragment f = new SensorFragment();
        mGoogleApiClient = mGAC;
        // Supply sensorType as an argument
        Bundle args = new Bundle();
        args.putInt("sensorType", sensorType);
        f.setArguments(args);

        return f;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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

        mView = inflater.inflate(R.layout.sensor, container, false);

        mTextTitle = (TextView) mView.findViewById(R.id.text_title);
        mTextTitle.setText("PD");
        mTextValues = (TextView) mView.findViewById(R.id.text_values);

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mGoogleApiClient!=null && !mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }

        mTextValues.setText(
                "x = " + Float.toString(event.values[0]) + "\n" +
                "y = " + Float.toString(event.values[1]) + "\n" +
                "z = " + Float.toString(event.values[2]) + "\n"
        );

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event);
        }
        else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            detectRotation(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();
        Vibrator vibrator = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);
        long[] vibrationPattern = {0, 100, 300, 100};


        //-1 - don't repeat
        final int indexInPatternToRepeat = -1;

        if((now - mShakeTime) > DIRECTION_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            float gForce = (float)Math.sqrt(gX*gX + gY*gY + gZ*gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color

            //UP
            if(gX > DIRECTION_THRESHOLD && gForce < SHAKE_THRESHOLD ) {
                mView.setBackgroundColor(Color.rgb(100, 0, 0));
                mTextValues.append("UP\n");
                sendCommand("play");
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
            }
            //DOWN
            else if(gX < (- DIRECTION_THRESHOLD) && gForce < SHAKE_THRESHOLD ) {
                mView.setBackgroundColor(Color.rgb(100, 100, 0));
                mTextValues.append("DOWN\n");
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                sendCommand("pause");
            }
            //LEFT
            else if(gY < (- DIRECTION_THRESHOLD) && gForce < SHAKE_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
                mTextValues.append("LEFT\n");
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                sendCommand("avanti");
            }
            //RIGHT
            else if(gY > DIRECTION_THRESHOLD && gForce < SHAKE_THRESHOLD ) {
                mView.setBackgroundColor(Color.rgb(0, 0, 100));
                mTextValues.append("RIGHT\n");
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                sendCommand("dietro");
            }
            else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }

    private void detectRotation(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mRotationTime) > ROTATION_WAIT_TIME_MS) {
            mRotationTime = now;

            // Change background color if rate of rotation around any
            // axis and in any direction exceeds threshold;
            // otherwise, reset the color
            if(Math.abs(event.values[0]) > ROTATION_THRESHOLD ||
               Math.abs(event.values[1]) > ROTATION_THRESHOLD ||
               Math.abs(event.values[2]) > ROTATION_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
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

    private void sendCommand(String mess) {

        if(mGoogleApiClient!=null && !mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        if(mGoogleApiClient!=null){
            PutDataMapRequest putDataMapReq =
                    PutDataMapRequest.create("/Command");
                    putDataMapReq.getDataMap().putString(COMMAND_KEY, mess);
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        }

    }
}
