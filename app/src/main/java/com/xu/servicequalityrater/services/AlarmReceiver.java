package com.xu.servicequalityrater.services;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.xu.servicequalityrater.CameraActivity;

/**
 *
 *  Differs from AlarmReceiver because it should be launched by onBoot AUTOMATICALLY
 * SOURCE: http://stackoverflow.com/questions/11168869/starting-background-service-when-android-turns-on
 * Support: https://examples.javacodegeeks.com/android/core/activity/android-start-service-boot-example/
 * Support :https://thinkandroid.wordpress.com/2010/01/24/handling-screen-off-and-screen-on-intents/
 *
 * 1. Add to Manifest
 *  <receiver android:name=".BootBroadcastReceiver" >
 *<intent-filter>
 * <action android:name="android.intent.action.BOOT_COMPLETED" />
 *</intent-filter>
 *</receiver>
 *and permission
 * <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED">
 * </uses-permission>
 *
 * BroadcastReceiver that works with Android's AlarmManager to automatically
 * Launch the CameraActivity. Also passes boolean autostart, which needs to be fetched
 * and launch camera automatically
 *
 * For wakelock/VERITAS, BroadCastReceiver needs to be registered in manifest itself
 * For detailed instructions consult following link
 * /http://stackoverflow.com/questions/11168869/starting-background-service-when-android-turns-on
 *
 *
 */
//TODO while HealthAlarmReceiver responds to User, AlarmReceiver should respond to Either phone clock Alarms?
    //Or just start Everyday at some point.
    //http://stackoverflow.com/questions/21461191/alarmmanager-fires-alarms-at-wrong-time
    /*TODO Instead lets just schedule next alarm at some random time, and check if time falls within/without opening hours
    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION,  android.intent.action.PHONE_STATE
    */
public class AlarmReceiver extends BroadcastReceiver {

    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(ACTION)) {
            //Service
            Intent serviceIntent = new Intent(context, CameraOneService.class);
            context.startService(serviceIntent);


        /*
        // http://stackoverflow.com/questions/26454120/starting-activity-from-alarm-broadcast-receiver-even-when-user-clicked-home-butt

        //PowerManager wakelock just forces the system to turn on screen, nautrally
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "TRAININGCOUNDOWN");
        wl.acquire();



        /* OLD CODE to start CameraActivity, now we are using a service instead
        //TODO do we really need to start a new activity, or can we simply use CameraActivity.this?
        Intent scheduledIntent = new Intent(context, CameraActivity.class);
        scheduledIntent.addFlags(Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_SINGLE_TOP );
        scheduledIntent.putExtra("autostart",autostart);
        context.startActivity(scheduledIntent);


        wl.release();
        */
        }
    }
}
