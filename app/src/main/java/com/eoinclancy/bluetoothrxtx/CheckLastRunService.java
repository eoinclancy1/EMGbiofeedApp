package com.eoinclancy.bluetoothrxtx;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Background Service that runs to keep track of inactivity
 * Should be reset in the onCreate method, gives a rough estimate of time
 */

public class CheckLastRunService extends Service {

    private final static String TAG = "CheckRecentPlay";
    private static Long MILLISECS_PER_DAY = 86400000L;
    private static Long MILLISECS_PER_MIN = 60000L;

      //private static long delay = MILLISECS_PER_MIN * 1;   // 1 minute (for testing)
      private static long delay = MILLISECS_PER_DAY * 1;   // 1 days - works out to be roughly 24hrs

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "Service started");                                                              // A means of accessing global data - other apps see changes as they occur
        SharedPreferences settings = getSharedPreferences(NotificationActivity.PREFS, MODE_PRIVATE); //Retreive and hold the contents of the preferences file .. .PREFS

        // Are notifications enabled?
        if (settings.getBoolean("enabled", true)) {
            // Is it time for a notification?
            if (settings.getLong("lastRun", Long.MAX_VALUE) < System.currentTimeMillis() - delay) //Check setting, with name lastRun, to check time since last active
                sendNotification();                                                               //Call method when current time exceeds set time

        } else {
            Log.i(TAG, "Notifications are disabled");
        }

        // Set an alarm for the next time this service should run:
        setAlarm();

        Log.v(TAG, "Service stopped");
        stopSelf();
    }

    public void setAlarm() {

        Intent serviceIntent = new Intent(this, CheckLastRunService.class);        //Intent set within this class
        PendingIntent pi = PendingIntent.getService(this, 131313, serviceIntent,
                PendingIntent.FLAG_CANCEL_CURRENT); //Description of the intent and the action to perform with it, this will start a service

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pi);
        //RTC_WAKEUP -- Wakes up the device to fire the pending intent at the specified time
            //Alternative would be RTC - this would improve battery consumption as it does not wake up the device
        //Fire alarm at the current time + the specified delay time (24/48 hours)
        //Then fire the above declared intent

        Log.v(TAG, "Alarm set");
    }


    //Creating the actual notification to be displayed on screen
    public void sendNotification() {

        //Note: before lollipop, setting .setSmallIcon was enough - on the 7" tablet the icon displays perfectly with just this
        //  lollipop onwards -> wihtout .setLargeIcon -> only a big white square is displayed
        //  the value set in .setLargeIcon replaces this big square

        Intent mainIntent = new Intent(this, NotificationActivity.class);
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.appicon);//Used to replace big white square
        @SuppressWarnings("deprecation")
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);            //Used to construct all elements of the notification
        notif.setAutoCancel(true)                                                           //Notification automatically disappears when user touches it
                .setContentIntent(PendingIntent.getActivity(this, 131314, mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT))                                 //Supply a PendingIntent to be sent when the notification is clicked
                .setContentTitle("Dont Forget to exercise!")                                //Title of the notification
                .setContentText("It has been 24 hours since you used the app!")             //Text displayed beneath the notification title
                .setDefaults(NotificationCompat.DEFAULT_ALL)                                //The default notification properties are inherited from system defaults
                .setLargeIcon(largeIcon)                                                    //Setting the app icon as the main motification image
                .setSmallIcon(R.drawable.small_run_icon_w)                                  //A default google running image is set as the icon that appears in notification bar at top of screen
                .setTicker("It has been 24 hours since you performed your exercise!")       //When notification appears, this is the text that runs accross top of screen
                .setWhen(System.currentTimeMillis())                                        //Timestamp of when the notification appeared
                .build();                                                                   //Combine all the above options and return a new notification object



        NotificationManager notificationManager
                = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(131315, notif.build());                                  //Informs the user of the background event

        Log.v(TAG, "Notification sent");
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
