package it.unipi.wearmusic;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import it.unipi.wearmusic.MainActivity;

import static android.media.AudioManager.ADJUST_LOWER;
import static android.media.AudioManager.ADJUST_RAISE;

/**
 * Created by Franc on 03/09/2017.
 */

public class ListenerService extends WearableListenerService{

    private static MusicService musicSrv;
    private static final String INCREASE_VOLUME = "increase volume";
    private static final String DECREASE_VOLUME = "decrease volume";
    private static final String PLAY = "play";
    private static final String NEXT = "next";
    private static final String PREVIOUS = "previous";
    public static final String ACTION_MSG_RECEIVED = "it.unipi.wearmusic.Received";
    private final Handler handler = new Handler();
    private static final String TAG = "WearMusic";
    Intent intent;


    @Override
    public void onCreate() {
        super.onCreate();

        intent = new Intent(ACTION_MSG_RECEIVED);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 1000); // 1 second

    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            //DisplayLoggingInfo();
            handler.postDelayed(this, 10000); // 10 seconds
        }
    };

    private void DisplayLoggingInfo(String msg) {
        Log.d(TAG, "entered DisplayLoggingInfo");

        intent.putExtra("command",msg);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
    }

    private static AudioManager managerAudio;
    //private final IBinder Bind = new ListenerBinder();

    public class ListenerBinder extends Binder {
        public ListenerService getService() {
            return ListenerService.this;
        }
    }

    /*
    public static void setParameters(MusicService ms,AudioManager AM) {

        musicSrv = ms;
        managerAudio = AM;

        Log.i(TAG," costruttore 2");
        if (managerAudio==null)
            Log.i(TAG," null ma");
        if (musicSrv==null)
            Log.i(TAG," null musicSrv");
    }
    */

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.i(TAG, "messaggio ricevuto");
        if (messageEvent.getPath().equals("/wear_message")) {
            String mess = new String(messageEvent.getData());

            Log.i(TAG, "ricevuto: " + mess);
            DisplayLoggingInfo(mess);

            /*
            if(mess.compareTo(NEXT)==0){
                musicSrv.playNext();
            }else if(mess.compareTo(PLAY)==0){
                if(musicSrv.isPng())
                    musicSrv.pausePlayer();
                else{
                    if(musicSrv.pause==true){
                        musicSrv.go();
                    }else{
                        musicSrv.playSong();
                    }

                }
            }else if(mess.compareTo(DECREASE_VOLUME)==0){
                managerAudio.adjustVolume(ADJUST_LOWER,0);
            }else if(mess.compareTo(INCREASE_VOLUME)==0){
                managerAudio.adjustVolume(ADJUST_RAISE,0);
            }else if(mess.compareTo(PREVIOUS)==0){
                musicSrv.playPrev();
            }*/
        }
    }
}