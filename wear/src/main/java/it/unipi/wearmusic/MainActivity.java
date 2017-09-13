package it.unipi.wearmusic;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;


public class MainActivity extends Activity  implements DataApi.DataListener, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "WearMusic";
    private static final String TITLE_KEY = "title";
    private static final String TITLE_NEXT_KEY = "title next";
    private static final String TITLE_PREV_KEY = "title prev";
    private static final String PATH = "/InfoSong";

/*
     @Override
     public boolean onTouchEvent(MotionEvent motionEvent){
         super.onTouchEvent(motionEvent);
         //if(motionEvent.)
        Log.i(TAG,"on touch");

         return true;
     }

     @Override
     public boolean  onKeyUp(int i,KeyEvent ke){

         Log.i(TAG,"on key");
         return true;
     }
     */
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
                pager.setAdapter(new FragmentPagerAdapter(getFragmentManager(),mGoogleApiClient));

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

        Wearable.DataApi.addListener(mGoogleApiClient, (DataApi.DataListener) this);
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
    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.i(TAG,"onDataChanged title");
        for(DataEvent event : dataEventBuffer) {
            if(event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if(item.getUri().getPath().compareTo(PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateTitle(dataMap.getString(TITLE_KEY),dataMap.getString(TITLE_NEXT_KEY),dataMap.getString(TITLE_PREV_KEY));
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    private void updateTitle(final String title,final String titlen,final String titlep) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.text_title);
                if(tv!=null)
                tv.setText("song current: " + title);
                TextView tvp = (TextView) findViewById(R.id.text_previous_title);
                if(tvp!=null)
                tvp.setText("song prev: " + titlep);
                TextView tvn = (TextView) findViewById(R.id.text_next_title);
                if(tvn!=null)
                tvn.setText("song next: " + titlen);
            }
        });}


}