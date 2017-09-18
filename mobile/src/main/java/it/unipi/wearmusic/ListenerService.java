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

public class ListenerService extends WearableListenerService{

    public static final String ACTION_MSG_RECEIVED = "it.unipi.wearmusic.Received";
    private static final String COMMAND_KEY = "command";
    private static final String TAG = "WearMusic";
    Intent intent;


    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(ACTION_MSG_RECEIVED);
    }

    private void sendCommand(String msg) {
        Log.i(TAG, "SendCommand");
        intent.putExtra(COMMAND_KEY,msg);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.i(TAG, "messaggio ricevuto");
        if (messageEvent.getPath().equals("/wear_message")) {
            String mess = new String(messageEvent.getData());

            Log.i(TAG, "ricevuto: " + mess);
            sendCommand(mess);

        }
    }
}