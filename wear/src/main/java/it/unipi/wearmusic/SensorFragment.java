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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import android.view.View.OnClickListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import static android.content.ContentValues.TAG;
import static android.content.Context.VIBRATOR_SERVICE;

public class SensorFragment extends Fragment implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final float SHAKE_THRESHOLD = 1.2f;
    private static final float DIRECTION_THRESHOLD_MIN = 0.6f;
    private static final float DIRECTION_THRESHOLD_MAX = 1f;
    private static final int DIRECTION_TIME_MS =700;
    private static final float ROTATION_THRESHOLD = 5.0f;
    private static final int ROTATION_WAIT_TIME_MS = 700;

    private static final String COMMAND_KEY = "command";
    private static final String TAG = "HandheldActivity";
    private static GoogleApiClient mGoogleApiClient;

    private View mView;
    private TextView mTextTitle;
    private TextView mTextValues;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mSensorType;
    private long mShakeTime = 0;
    private boolean gesture = false;

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
    ImageButton.OnClickListener listener = new ImageButton.OnClickListener()
    {

        @Override
        public void onClick(View v)
        {
            int id = v.getId();
            ImageView img;

            switch (id){

                case R.id.Next:
                    sendCommand("avanti");
                    //img= (ImageView) mView.findViewById(R.id.Next);
                    //img.setImageResource(R.drawable.img_btn_next_pressed);
                    break;

                case R.id.Previous:
                    sendCommand("dietro");
                    //img= (ImageView) mView.findViewById(R.id.Previous);
                    //img.setImageResource(R.drawable.img_btn_previous_pressed);
                    break;
                case R.id.Play:
                    sendCommand("pause");
                    //img= (ImageView) mView.findViewById(R.id.Next);
                    //img.setImageResource(R.drawable.img_btn_next_pressed);
                    break;

                case R.id.Minus:
                    sendCommand("volumegiu");
                    //img= (ImageView) mView.findViewById(R.id.Previous);
                    //img.setImageResource(R.drawable.img_btn_previous_pressed);
                    break;
                case R.id.Plus:
                    sendCommand("volumesu");
                    //img= (ImageView) mView.findViewById(R.id.Next);
                    //img.setImageResource(R.drawable.img_btn_next_pressed);
                    break;


            }
            Log.i(TAG, "BUTTON TO IMPLEMENT");
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);


        if(mGoogleApiClient!=null && !mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();


        Bundle args = getArguments();
        if(args != null) {
            mSensorType = args.getInt("sensorType");
        }

        if(mSensorType!=-1) {
            mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(mSensorType);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(mSensorType!=-1) {

            mView = inflater.inflate(R.layout.sensor, container, false);
            gesture = true;
            mTextTitle = (TextView) mView.findViewById(R.id.text_title);
            mTextTitle.setText("scegli il comando");
            mTextValues = (TextView) mView.findViewById(R.id.text_values);
        }else{
            gesture = false;
            Log.i(TAG, " valore " + String.valueOf(mSensorType));
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

        }

        return mView;
    }




    @Override
    public void onResume() {
        super.onResume();
        if(mGoogleApiClient!=null && !mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        if(mSensorType!=-1)
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL*5);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mSensorType!=-1)
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }

        /*
        mTextValues.setText(
                "x = " + Float.toString(event.values[0]) + "\n" +
                "y = " + Float.toString(event.values[1]) + "\n" +
                "z = " + Float.toString(event.values[2]) + "\n"
        );
        */


        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && gesture) {
            detectShake(event);
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
            if(gX > DIRECTION_THRESHOLD_MIN && gX < DIRECTION_THRESHOLD_MAX && gForce < SHAKE_THRESHOLD ) {

                mView.setBackgroundColor(Color.rgb(100, 0, 0));
                mTextValues.setText("volumesu\n");
                sendCommand("volumesu");
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
            }

            //DOWN
            else if(gX < (- DIRECTION_THRESHOLD_MIN) && gX > (- DIRECTION_THRESHOLD_MAX) && gForce < SHAKE_THRESHOLD ) {
                mView.setBackgroundColor(Color.rgb(100, 100, 0));
                mTextValues.setText("volumegiu");
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                sendCommand("volumegiu");
            }

            //LEFT
            else if(gY < (- DIRECTION_THRESHOLD_MIN) && gY > (- DIRECTION_THRESHOLD_MAX) && gForce < SHAKE_THRESHOLD) {
                /*
                ImageView img= (ImageView) mView.findViewById(R.id.Next);
                if(img == null){
                    Log.d(TAG, "img null");
                }else{
                    Log.d(TAG, "img not null");

                    img.setImageResource(R.drawable.img_btn_next_pressed);
                }
                */
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
                mTextValues.setText("avanti");
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                sendCommand("avanti");
            }

            //RIGHT
            else if(gY > DIRECTION_THRESHOLD_MIN && gY < DIRECTION_THRESHOLD_MAX && gForce < SHAKE_THRESHOLD ) {
                /*
                ImageView img= (ImageView) mView.findViewById(R.);
                if(img == null){
                    Log.d(TAG, "img null");
                }else{

                    img.setImageResource(R.drawable.img_btn_previous_pressed);
                    Log.d(TAG, "img not null");
                }*/

                mView.setBackgroundColor(Color.rgb(0, 0, 100));
                mTextValues.setText("pause/play\n");
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                sendCommand("pause");
            }
            else {
                /*

                */
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
/*
        if(mGoogleApiClient!=null && !mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        if(mGoogleApiClient!=null){
            PutDataMapRequest putDataMapReq =
                    PutDataMapRequest.create("/Command");
                    putDataMapReq.getDataMap().putString(COMMAND_KEY, msg);
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        }*/

        Log.i(TAG, "SEND MESSAGE");
        new SenderThread("/wear_message", msg).start();

    }
/*
    @Override
    public void onClick(View view) {
        int id = view.getId();
        ImageView img;

        switch (id){

            case R.id.Next:
                sendCommand("avanti");
                //img= (ImageView) mView.findViewById(R.id.Next);
                //img.setImageResource(R.drawable.img_btn_next_pressed);
                break;

            case R.id.Previous:
                sendCommand("pause");
                //img= (ImageView) mView.findViewById(R.id.Previous);
                //img.setImageResource(R.drawable.img_btn_previous_pressed);
                break;

        }
        Log.i(TAG, "BUTTON TO IMPLEMENT");
    }
*/
    class SenderThread extends Thread {
        String path;
        String msg;
        SenderThread(String p, String m) {
            path = p;
            msg = m;
        }
        public void run() {

            Log.i(TAG, "Dio");
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
