package it.unipi.wearmusic;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static android.media.AudioManager.ADJUST_LOWER;
import static android.media.AudioManager.ADJUST_RAISE;

public class MainActivity extends Activity implements MediaPlayerControl,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SeekBar.OnSeekBarChangeListener{


    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE =1 ;
    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private static final String TAG = "WearMusic";
    private static final String COMMAND_KEY = "command";
    private static final String TITLE_KEY = "title";
    private static final String TITLE_NEXT_KEY = "title next";
    private static final String TITLE_PREV_KEY = "title prev";
    private static final String STATUS_KEY = "status";
    private static final String PATH_INFO_SONG = "/InfoSong";
    private static final String PATH_INFO_STATUS = "/InfoStatus";

    private static GoogleApiClient mGoogleApiClient;
    private AudioManager managerAudio;
    private SeekBar seekBar;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();;
    //connect to the service
    private Intent intentTitle;
    private Intent intentCommand;
    private static final String INCREASE_VOLUME = "increase volume";
    private static final String DECREASE_VOLUME = "decrease volume";
    private static final String PLAY = "play";
    private static final String NEXT = "next";
    private static final String PREVIOUS = "previous";
    public static final String ACTION_UPDATE_TITLE = "it.unipi.wearmusic.Title";
    public static final String ACTION_MSG_RECEIVED = "it.unipi.wearmusic.Received";


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

        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }

        mGoogleApiClient.connect();
        Log.i(TAG,"call set parameters");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intentCommand = new Intent(this, ListenerService.class);
        intentTitle = new Intent(this, MusicService.class);
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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

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
        Log.i(TAG,"onDestroy");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            Log.i(TAG,"disconnect");
            mGoogleApiClient.disconnect();
        }
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        super.onPause();

    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.i(TAG,"resume");
        startService(intentTitle);
        startService(intentCommand);
        registerReceiver(broadcastReceiver, new IntentFilter(ListenerService.ACTION_MSG_RECEIVED));
        registerReceiver(broadcastReceiver, new IntentFilter(MusicService.ACTION_UPDATE_TITLE));
    }

    @Override
    protected void onStop() {
        Log.i(TAG,"on stop");
        super.onStop();
    }

    // ---------------------------- Override MediaPlayerControl methods --------------------------------------

    @Override
    public void pause() {
        musicSrv.setPause(true);
        updateStatusPlayer(musicSrv.isPause());
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

    // ------------------------------ Override ConnectionCallback methods ----------------------------------

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "connected");
        updateTitle(musicSrv.getSongTitle(), musicSrv.getSongTitlePrev(), musicSrv.getSongTitleNext());
        updateStatusPlayer(musicSrv.isPause());

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
                //Do something after 500ms
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
                //Do something after 500ms
                button.setImageResource(R.drawable.img_btn_previous);
            }
        }, 500);

        initProcessBar();

    }

    public void clickPlay(View view){

        // Verifico se c'è una canzone attiva
        if (isPlaying()){
            // Metto in pausa la canzone
            pause();
            stopProgressBar();

        } else {
            // Verifico se sono nella situazione di PAUSE o STOP
            if (musicSrv.isPause() == true) {
                // Riattivo la canzone in pausa
                start();
                // Updating progress bar
                updateProgressBar();

            }else{
                // Inizio dalla prima canzone
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

    /**
     * Update Info Page of the watch
     * @param title
     * @param titlePrevious
     * @param titleNext
     */
    private void updateTitle(String title,String titlePrevious,String titleNext) {

        Log.i(TAG,"updateTitle "+title);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(PATH_INFO_SONG);
        putDataMapReq.getDataMap().putString(TITLE_KEY, title);
        putDataMapReq.getDataMap().putString(TITLE_NEXT_KEY, titleNext);
        putDataMapReq.getDataMap().putString(TITLE_PREV_KEY, titlePrevious);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

    }

    /**
     * update button status of play/pause
     * @param status
     */
    private void updateStatusPlayer(boolean status) {

        Log.i(TAG,"updateStatusPlayer "+ status);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(PATH_INFO_STATUS);
        putDataMapReq.getDataMap().putBoolean(STATUS_KEY, status);
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

    private void updateUI(Intent intent) {
        if(intent.getAction()== ACTION_MSG_RECEIVED) {
            String command = intent.getStringExtra(COMMAND_KEY);
            Log.i(TAG, "info" + command);
            updateCommand(command);
        }
        if(intent.getAction()==ACTION_UPDATE_TITLE){
            Log.i(TAG, "action update title");
            String t = intent.getStringExtra(TITLE_KEY);
            String tn = intent.getStringExtra(TITLE_NEXT_KEY);
            String tp = intent.getStringExtra(TITLE_PREV_KEY);
            updateTitle(t,tp,tn);
        }
    }

    public void updateCommand(final String mess) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"update Command"+mess);
                if(mess.compareTo(NEXT)==0){
                    clickNext(null);
                }else if(mess.compareTo(PLAY)==0){
                    clickPlay(null);
                }else if(mess.compareTo(DECREASE_VOLUME)==0){
                    managerAudio.adjustVolume(ADJUST_LOWER,0);
                }else if(mess.compareTo(INCREASE_VOLUME)==0){
                    managerAudio.adjustVolume(ADJUST_RAISE,0);
                }else if(mess.compareTo(PREVIOUS)==0){
                    clickPrevious(null);
                }else if(mess.compareTo(TITLE_KEY)==0){
                    updateTitle(musicSrv.getSongTitle(),musicSrv.getSongTitlePrev(),musicSrv.getSongTitleNext());
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

        musicSrv.setPause(false);
        updateStatusPlayer(musicSrv.isPause());
        ImageButton button = (ImageButton) findViewById(R.id.Play);
        button.setImageResource(R.drawable.img_btn_pause);

        updateProgressBar();
    }

    public void updateProgressBar() {

        if (musicSrv.isPause()) {

            // resume from status pause
            musicSrv.setPause(false);
            updateStatusPlayer(musicSrv.isPause());
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

            seekBar.setProgress(progress);

            // Running this thread after 500 milliseconds
            mHandler.postDelayed(this, 500);
        }
    };

}