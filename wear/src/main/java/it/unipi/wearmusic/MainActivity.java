package it.unipi.wearmusic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import it.unipi.wearmusic.util.ListenerService;


public class MainActivity extends Activity
        implements  View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
        {
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "WearMusic";
    private static final String PRESSURE_KEY = "command";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupConnection();

        Log.i(TAG," on create main");
        setContentView(R.layout.activity_main);
       WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override public void onLayoutInflated(WatchViewStub stub) {
                final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
                pager.setOnPageChangeListener(new GridViewPager.OnPageChangeListener() {


                    @Override
                    public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {

                        Log.i(TAG,"1valore :"+i+" altro: "+i1);
                    }

                    @Override
                    public void onPageSelected(int i, int i1) {
                        Log.i(TAG,"2valore :"+i+" altro: "+i1);
                    }

                    @Override
                    public void onPageScrollStateChanged(int arg0) {
                        Log.i(TAG,"2valore :");

                    }
                });

                pager.setAdapter(new SensorFragmentPagerAdapter(getFragmentManager(),mGoogleApiClient));


                DotsPageIndicator indicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
                indicator.setPager(pager);


            }
        });

    }



    private void setupConnection(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }


    @Override
    protected void onStart() {
        super.onStart();

        if(mGoogleApiClient!=null)
            mGoogleApiClient.connect();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View view) {

            }
        }