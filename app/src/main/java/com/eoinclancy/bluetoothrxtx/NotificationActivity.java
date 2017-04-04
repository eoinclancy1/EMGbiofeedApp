package com.eoinclancy.bluetoothrxtx;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/* Main activity for enabling/disabling the inactivity notifications
* Could be used in real app as an option
* Notification should be on by default*/

public class NotificationActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    public final static String PREFS = "PrefsFile";

    private SharedPreferences settings = null;
    private SharedPreferences.Editor editor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Save time of run:
        settings = getSharedPreferences(PREFS, MODE_PRIVATE);
        editor = settings.edit();

        // First time running app?
        if (!settings.contains("lastRun"))
            enableNotification(null);
        else
            recordRunTime();            //http://stackoverflow.com/questions/22709751/how-to-send-notification-if-user-inactive-for-3-days

        Log.v(TAG, "Starting CheckRecentRun service...");
        startService(new Intent(this,  CheckLastRunService.class));

    }

    //Store the the current time for checking later
    public void recordRunTime() {
        editor.putLong("lastRun", System.currentTimeMillis());
        editor.commit();
    }

    //Setup for notification to be provided to user
    public void enableNotification(View v) {
        editor.putLong("lastRun", System.currentTimeMillis());
        editor.putBoolean("enabled", true);
        editor.commit();
        Toast.makeText(NotificationActivity.this, "Reminder notifications enabled", Toast.LENGTH_SHORT).show();
        Log.v(TAG, "Notifications enabled");
    }

    //Setup to cancel the user notification - sets timer to false
    public void disableNotification(View v) {
        editor.putBoolean("enabled", false);
        editor.commit();
        Toast.makeText(NotificationActivity.this, "Reminder notifications disabled", Toast.LENGTH_SHORT).show();
        Log.v(TAG, "Notifications disabled");
    }
}
