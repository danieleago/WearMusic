package it.unipi.wearmusic;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.hardware.Sensor;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.WatchViewStub;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;

public class SensorFragmentPagerAdapter extends FragmentGridPagerAdapter {
    GoogleApiClient mGACA;
    private int[] sensorTypes = {
            Sensor.TYPE_ACCELEROMETER,
             -1 //Sensor.TYPE_GYROSCOPE //
    };


    public SensorFragmentPagerAdapter(FragmentManager fm, GoogleApiClient mGAC ) {
        super(fm);
        mGACA = mGAC;
    }

    @Override
    public Fragment getFragment(int row, int column) {
        return SensorFragment.newInstance(sensorTypes[column] , mGACA);
    }

    @Override
    public int getRowCount() {
        return 1; // fix to 1 row
    }

    @Override
    public int getColumnCount(int row) {
        return sensorTypes.length;
    }
}
