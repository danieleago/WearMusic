package it.unipi.wearmusic.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import it.unipi.wearmusic.R;
import it.unipi.wearmusic.util.MusicService;
import it.unipi.wearmusic.util.Song;
import it.unipi.wearmusic.util.SongAdapter;
import it.unipi.wearmusic.util.Utilities;

import static android.media.AudioManager.ADJUST_LOWER;
import static android.media.AudioManager.ADJUST_RAISE;


public class MainActivity extends Activity implements MessageApi.MessageListener,MediaPlayerControl,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SeekBar.OnSeekBarChangeListener{


    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE =1 ;
    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private boolean pause = false;
    private static final String TAG = "WearMusic";
    private static final String COMMAND_KEY = "command";
    private static final String TITLE_KEY = "title";
    private static final String TITLE_NEXT_KEY = "title next";
    private static final String TITLE_PREV_KEY = "title prev";
    private static final String PATH = "/InfoSong";
    private static GoogleApiClient mGoogleApiClient;
    private AudioManager managerAudio;
    private SeekBar seekBar;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();;
    //connect to the service

    private static final String INCREASE_VOLUME = "increase volume";
    private static final String DECREASE_VOLUME = "decrease volume";
    private static final String PLAY = "play";
    private static final String NEXT = "next";
    private static final String PREVIOUS = "previous";


    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    // ----------------------- Override Activity methods --------------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        managerAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        requestPermission();
        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        getSongList();
        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_shuffle:
                musicSrv.setShuffle();
                if (musicSrv.isShuffle()){
                    item.setIcon(ContextCompat.getDrawable(this,R.drawable.img_btn_shuffle_pressed));
                } else {
                    item.setIcon(ContextCompat.getDrawable(this,R.drawable.img_btn_shuffle));
                }
                break;

            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {

        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        super.onPause();
        //paused=true;
        //Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        //mGoogleApiClient.disconnect();

    }

    @Override
    protected void onResume(){
        super.onResume();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        super.onStop();
    }

    // ---------------------------- Override MediaPlayerControl methods --------------------------------------

    @Override
    public void pause() {
        pause = true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null &&  musicBound  &&  musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    // ------------------------------- Override MessageListener method --------------------------------------

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(messageEvent.getPath().equals("/wear_message")) {
            String msg = new String(messageEvent.getData());
            updateCommand(msg);
        }
    }

    // ------------------------------ Override ConnectionCallback methods ----------------------------------

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "connected");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "connection failed");
    }

    // --------------------------- Override OnSeekBarChangeListener methods ------------------------------------

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mUpdateTimeTask);

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // remove message Handler from updating progress bar
        mHandler.removeCallbacks(mUpdateTimeTask);
        /*int totalDuration = getDuration();
        int currentPosition = Utilities.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
        */

    }
    // ------------------------------ onClick calls ----------------------------------------------------------

    public void songPicked(View view){


        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();

        initProcessBar();

        if (!isPlaying()){
            ((ImageButton)findViewById(R.id.Play)).setImageResource(R.drawable.img_btn_pause);
        }
    }

    public void clickNext(View view) {

        musicSrv.playNext();
        final ImageButton button = (ImageButton) findViewById(R.id.Next);
        button.setImageResource(R.drawable.img_btn_next_pressed);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                button.setImageResource(R.drawable.img_btn_next);
            }
        }, 500);
        initProcessBar();
    }

    public void clickPrevious(View view) {

        musicSrv.playPrev();
        final ImageButton button = (ImageButton) findViewById(R.id.Previous);
        button.setImageResource(R.drawable.img_btn_previous_pressed);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                button.setImageResource(R.drawable.img_btn_previous);
            }
        }, 500);

        initProcessBar();

    }

    public void clickPlay(View view){

        if (isPlaying()){

            pause();
            stopProgressBar();

        } else {
            if (pause == true) {
                start();
                // Updating progress bar
                updateProgressBar();

            }else{
                musicSrv.playSong();
                initProcessBar();
            }



        }

    }

    // ------------------------- Methods ------------------------------------------------------------------

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    public static void updateTitle(String title,String titlen,String titlep) {

        Log.i(TAG,"updateTitle"+title);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(PATH);
        putDataMapReq.getDataMap().putString(TITLE_KEY, title);
        putDataMapReq.getDataMap().putString(TITLE_NEXT_KEY, titlen);
        putDataMapReq.getDataMap().putString(TITLE_PREV_KEY, titlep);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

    }


    public void requestPermission(){

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constantconstant. The callback method gets the
                // result of the request.
            }
        }

    }

    public void updateCommand(final String mess) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mess.compareTo(NEXT)==0){
                    clickNext(null);
                }else if(mess.compareTo(PLAY)==0){
                    clickPlay(null);
                    /*if(isPlaying()) {
                        musicSrv.pausePlayer();
                        ((ImageButton)findViewById(R.id.Play)).setImageResource(R.drawable.img_btn_play);
                    } else {
                        musicSrv.playSong();
                        ((ImageButton)findViewById(R.id.Play)).setImageResource(R.drawable.img_btn_pause);
                    }*/
                }else if(mess.compareTo(DECREASE_VOLUME)==0){
                    managerAudio.adjustVolume(ADJUST_LOWER,0);
                }else if(mess.compareTo(INCREASE_VOLUME)==0){
                    managerAudio.adjustVolume(ADJUST_RAISE,0);
                }else if(mess.compareTo(PREVIOUS)==0){
                    clickPrevious(null);
                }

            }
        });
    }

    /**
     * Update timer on seekbar
     * */

    public void initProcessBar(){

        seekBar.setProgress(0);
        seekBar.setMax(100);
        updateProgressBar();
    }

    public void updateProgressBar() {

        if (pause) {
            pause = false;
            ImageButton button = (ImageButton) findViewById(R.id.Play);
            button.setImageResource(R.drawable.img_btn_pause);
        }
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    public void stopProgressBar() {
        mHandler.removeCallbacks(mUpdateTimeTask);
        ImageButton button = (ImageButton) findViewById(R.id.Play);
        button.setImageResource(R.drawable.img_btn_play);
    }

    /**
     * Background Runnable thread
     * */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = getDuration();
            long currentDuration = getCurrentPosition();

            // Displaying Total Duration time
            ((TextView)findViewById(R.id.totalTimeText)).setText(Utilities.milliSecondsToTimer(totalDuration));
            // Displaying time completed playing
            ((TextView)findViewById(R.id.currentTimeText)).setText(Utilities.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = (int)(Utilities.getProgressPercentage(currentDuration, totalDuration));
            //Log.d("Progress", ""+progress);
            seekBar.setProgress(progress);

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

}