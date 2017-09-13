package it.unipi.wearmusic;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.hardware.Sensor;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;


public class SensorFragmentPagerAdapter extends FragmentGridPagerAdapter {

    private static final String TAG = "WearMusic";
    private int SENSOR = 1 ;
    private int TITLE = 0 ;
    GoogleApiClient mGACA;
    private int[] fragmentTypes = {
            TITLE,
            SENSOR
             };


    public SensorFragmentPagerAdapter(FragmentManager fm, GoogleApiClient mGAC ) {
        super(fm);
        mGACA = mGAC;

        }

    @Override
    public Fragment getFragment(int row, int column) {

        if(fragmentTypes[column] == SENSOR)
            return SensorFragment.newInstance(Sensor.TYPE_ACCELEROMETER , mGACA);
        else if (fragmentTypes[column] == TITLE)
            return TitleFragment.newInstance("Titolo");
        else{
            return null;
        }
    }

    @Override
    public int getRowCount() {
        return 1; // fix to 1 row
    }

    @Override
    public int getColumnCount(int row) {
        return fragmentTypes.length;
    }


}
