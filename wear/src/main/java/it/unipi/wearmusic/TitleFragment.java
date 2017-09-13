package it.unipi.wearmusic;

import android.app.Fragment;
import android.content.Context;
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
import android.view.View.OnClickListener;
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

public class TitleFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener  {


    private static TextView mTextTitle;
    private static TextView mTextTitleNext;
    private static TextView mTextTitlePrev;
    private static final String TITLE_KEY = "title";
    private static final String PATH = "/InfoSong";
    private View mView;

    private static GoogleApiClient mGoogleApiClient;

    private static final String TAG = "WearMusic";

    public static TitleFragment newInstance(GoogleApiClient mGAC) {
        TitleFragment f = new TitleFragment();
        mGoogleApiClient = mGAC;
        return f;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Wearable.DataApi.addListener(mGoogleApiClient, (DataApi.DataListener) this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.title, container, false);
        mTextTitle = (TextView) mView.findViewById(R.id.text_title);
        mTextTitleNext = (TextView) mView.findViewById(R.id.text_next_title);
        mTextTitlePrev = (TextView) mView.findViewById(R.id.text_previous_title);


        return  mView;
    }




    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

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
/*
    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.i(TAG,"onDataChanged title");
        for(DataEvent event : dataEventBuffer) {
            if(event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if(item.getUri().getPath().compareTo(PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    //TitleFragment.updateTitle(dataMap.getString(TITLE_KEY),"next","prev");
                    //TitleFragment fragment_obj = (TitleFragment)getFragmentManager().findFragmentById(R.id.title_view);
                    //TitleFragment.
                    //updateTitle(dataMap.getString(TITLE_KEY),"next","prev");
                    try {

                        ((TextView) mView.findViewById(R.id.text_title)).setText(dataMap.getString(TITLE_KEY));
                        ((TextView) mView.findViewById(R.id.text_next_title)).setText("next");
                        ((TextView) mView.findViewById(R.id.text_previous_title)).setText("prev");

                    }catch (RuntimeException re){

                    }finally {


                    }
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }
*/

}
