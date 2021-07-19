package protec.pl.protecabasvol2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;


public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    public static String database;
    public static String password;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        ComponentName componentName = new ComponentName(this, FirebaseMessagingService.class);
        this.getPackageManager().setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        if(remoteMessage.getData().size() > 0){
            Log.d("TEST ETST", "TEST");
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            showNotification(title, body);
        }

//        if (remoteMessage.getNotification() != null){
//            String title = remoteMessage.getNotification().getTitle();
//            String body = remoteMessage.getNotification().getBody();
//            showNotification(title, body);
//        }
        super.onMessageReceived(remoteMessage);
    }

    public static String getDatabase() {
        return database;
    }

    public static String getPassword() {
        return password;
    }

    public static void setDBandPassword(String databaseLocal, String passwordLocal){
        database = databaseLocal;
        password = passwordLocal;
    }

    // Method to display the notifications
    public void showNotification(String title, String message) {
        Intent intent; String channel_id = "notification_channel";
        if(getPassword() == null) {
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }else {
            intent = new Intent(this, Menu.class);
            intent.putExtra("password", getPassword());
            intent.putExtra("database", getDatabase());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }


        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                .setSmallIcon(R.drawable.protec_logo_szare)
                .setAutoCancel(true)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);
        builder = builder.setContentTitle(title).setContentText(message).setSmallIcon(R.drawable.protec_logo_szare);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channel_id, "web_app", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        notificationManager.notify(0, builder.build());

    }
}
