package it.unipi.wearmusic.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import it.unipi.wearmusic.R;
/**
 * Created by Franc on 03/09/2017.
 */

public class ListenerService extends WearableListenerService {
    public static final String ACTION_MSG_RECEIVED = "it.unipi.iet.maps.MSG_RECEIVED";
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(messageEvent.getPath().equals("/my_message")) {
            String message = new String(messageEvent.getData());
            Intent messageIntent = new Intent();
            messageIntent.setAction(ACTION_MSG_RECEIVED);
            messageIntent.putExtra("message", message);
            PendingIntent pi = PendingIntent.getActivity(this, 0, messageIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder nb = new Notification.Builder(this);
            nb.setContentTitle("Title");
            nb.setContentText("Some text");
            nb.setContentIntent(pi);
            //nb.setSmallIcon(R.drawable.ic_star);
            Notification n = nb.build();
            NotificationManager nm =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(1, n);
        } else {
            super.onMessageReceived(messageEvent);
        }
    }
}