package it.unipi.wearmusic;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener{

    private static final String TITLE_KEY = "title";
    private static final String TITLE_NEXT_KEY = "title next";
    private static final String TITLE_PREV_KEY = "title prev";
    public static final String ACTION_UPDATE_TITLE = "it.unipi.wearmusic.Title";
    private static final String TAG = "WearMusic";
    Intent intentTitle;

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songPosn;
    private final IBinder musicBind = new MusicBinder();
    private String songTitle = "";
    private String songTitleNext = "";
    private String songTitlePrev = "";
    private static final int NOTIFY_ID = 1;
    private boolean shuffle = false;
    private Random rand;
    private boolean pause = true;
    private boolean stop = true;


    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }
    public String getSongTitle() {
        return songTitle;
    }

    public String getSongTitleNext() {
        return songTitleNext;
    }

    public String getSongTitlePrev() {
        return songTitlePrev;
    }

    public boolean isPause() {
        return pause;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    @Override
    public void onCreate() {
        //create the service
        super.onCreate();

        intentTitle = new Intent(ACTION_UPDATE_TITLE);
        //initialize position
        songPosn = 0;
        //create player
        player = new MediaPlayer();
        initMusicPlayer();
        rand=new Random();

    }

    public boolean isShuffle(){
        return shuffle;
    }

    public void setShuffle(){
        if(shuffle) shuffle=false;
        else shuffle=true;
    }

    public void setList(ArrayList<Song> theSongs) {
        songs = theSongs;
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }
    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();

        Song playSong = songs.get(songPosn);
        songTitle=playSong.getTitle();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing: ").setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    public void setSong(int songIndex) {
        songPosn = songIndex;
    }

    public void playSong() {
        Log.i(TAG, "song position: " + songPosn);
        int temp;
        player.reset();
        //get song
        Song playSong = songs.get(songPosn);
        temp = songPosn-1;

        if (temp < 0)
            temp = songs.size() - 1;
        Song songPrev = songs.get(temp);

        temp = songPosn+1;
        if (temp >= songs.size())
            temp=0;
        Song songNext = songs.get(temp);
        //get id


        songTitle = playSong.getTitle();
        songTitleNext = songNext.getTitle();
        songTitlePrev = songPrev.getTitle();

        // send info to MainActivity through Intent
        sendTitle();

        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try {
            player.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
    }

    public int getPosn() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPng() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int posn) {
        player.seekTo(posn);
    }

    public void go() {
        player.start();
    }

    public void playPrev() {
        songPosn--;
        if (songPosn < 0) songPosn = songs.size() - 1;
        playSong();
    }

    public void playNext(){
        if(shuffle){
            int newSong = songPosn;
            while(newSong==songPosn){
                newSong=rand.nextInt(songs.size());
            }
            songPosn=newSong;
        }
        else{
            songPosn++;
            if(songPosn>=songs.size()) songPosn=0;
        }
        playSong();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition()>0){
            mp.reset();
            playNext();
        }
    }
    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    private void sendTitle() {
        if(intentTitle==null)
            Log.i(TAG, "intentTitle null");

        if (shuffle){

            intentTitle.putExtra(TITLE_KEY, songTitle);
            intentTitle.putExtra(TITLE_PREV_KEY, "");
            intentTitle.putExtra(TITLE_NEXT_KEY, "");
        } else {

            intentTitle.putExtra(TITLE_KEY, songTitle);
            intentTitle.putExtra(TITLE_PREV_KEY, songTitlePrev);
            intentTitle.putExtra(TITLE_NEXT_KEY, songTitleNext);
        }

        sendBroadcast(intentTitle);
    }


}