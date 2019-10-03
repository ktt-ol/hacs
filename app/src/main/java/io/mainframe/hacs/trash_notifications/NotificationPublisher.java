package io.mainframe.hacs.trash_notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import org.pmw.tinylog.Logger;

import io.mainframe.hacs.R;
import io.mainframe.hacs.main.NetworkStatus;

public class NotificationPublisher extends BroadcastReceiver {

    public static final String EXTRA_MSG = "eventMsg";

    private static final String CHANNEL_ID = "trashNotiChannel";

    public void onReceive(Context context, Intent intent) {
        final NetworkStatus status = new NetworkStatus(context, PreferenceManager.getDefaultSharedPreferences(context));
        if (!status.isInMainframeWifi()) {
            Logger.info("Skip trash notification, because I'm not in the mainfame wifi.");
            return;
        }

        Notification.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            this.createNotificationChannel(context);
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
            builder.setPriority(Notification.PRIORITY_DEFAULT);
        }
        builder.setContentTitle("Müll rausbringen");
        final String msg = intent.getStringExtra(EXTRA_MSG);
        builder.setContentText(msg);
        builder.setSmallIcon(R.drawable.ic_trash);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Logger.info("Show notification: " + msg);
        notificationManager.notify(1, builder.build());

//        new TrashCalendar(context).setNextAlarm();
    }


    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        final NotificationChannel chan = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (chan != null) {
            return;
        }
        CharSequence name = "Hacs Müll Benachrichtigung";
        String description = "Du wirst einen Tag zuvor über die Müllabfuhr benachrichtig. Aber nur, wenn du gerade im Hackspace bist.";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }
}
