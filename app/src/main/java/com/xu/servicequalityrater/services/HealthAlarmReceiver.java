package com.xu.servicequalityrater.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.xu.servicequalityrater.CameraActivity;
import com.xu.servicequalityrater.EmotionRecyclerActivity;
import com.xu.servicequalityrater.R;

import java.util.Calendar;

//TODO 03/04WORKS! REPEATS EVERY MINUTE BUT ONBOOTCOMPLETED NO LONGER EFFECTIVE DUE TO GOOGLE POLICIES
//TODO 03/04-WORKS x2! STARTS EmotionRecyclerActivity FROM LOCK SCREEN USING ACTION_USER_PRESENT. FUCKING A. NEeds to ensure its in battery saver protected app though
//TODO 04/04 Service seems to be half-working, saw a toast once, suggesting that the service itself doesnt work - Testing service separately in CameraActivity
// TODO 07/05 WORKS! what else do we have except User Present?
//TODO  Consult CameraActivity For programatical method to disable manifest-registered receiver
//todo 1007 DISABLED EVERY 10 SECONDS, WILL ONLY TAKE ONCE NOW

/** REMEMBER HUAWEI HAS PROTECTED APP RESTRICTIONS
 *
 *https://developer.android.com/reference/android/app/AlarmManager.html
 * AlarmManager allows for triggering with SYSTEM ALARMS
 * Note the AlarmManager has  Interval_DAY function, meaning possibility of triggerring remotely
 * Switch this code to work automatically with alarmManager
 *
 * MOD HEAlthPictureService2 to only use rear camera in this case
 * Or we just launch CameraActiivtyInstead.

 ONBOOTCOMPLETED http://stackoverflow.com/questions/17226410/android-boot-completed-not-received-when-application-is-closed/19856367#19856367
 */

//TODO 07082017 Look and Add in Time elapsed check to make sure we only trigger if an hour passed
    //https://stackoverflow.com/questions/25620908/what-are-the-ways-to-check-24-hours-are-completed
public class HealthAlarmReceiver  extends BroadcastReceiver {
    //TODO Action.USER_PRESENT is registered in manifest, User_initialize, Screen_ON
    // Consider User_Background
    //All we have here is trigger code
    //According to mark, should work as long as no one Force-Stops
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Long lastConfigCheck;
    SharedPreferences sharedPreferences;
    int type;
    int seconds;

    //Trialhandler
    long trialstart;
    SharedPreferences checkDate;
    SharedPreferences.Editor edit;
    boolean trialvalid;


    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        checkDate = context.getSharedPreferences("checkdate", Context.MODE_PRIVATE);
        edit = checkDate.edit();

        //0208 temp disabled for SGH staff demo
        trialvalid=true;
        //trialvalid = checkTrialValidity();

        Intent i = new Intent(context, HealthPictureService2.class);

        PendingIntent pending = PendingIntent.getService(context,
                0,
                i,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar cal = Calendar.getInstance();

        //27082017 New code to fetch user-set limits for detection
        sharedPreferences = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        type = sharedPreferences.getInt("type", 0);



        seconds = setDetectionLimiter(0);

        //TODO FOLLOWING CODE CHECKS IF AN HOUR HAS PASSED. STEP BY STEP INSTRUCTION
        prefs = context.getSharedPreferences("timecheck", Context.MODE_PRIVATE);
        lastConfigCheck = prefs.getLong("LAST_CONFIG_CHECK", 0);
        editor = prefs.edit();

        if (trialvalid == true) {
            //If trial actually valid, check scan period

            if (lastConfigCheck == 0) {
                lastConfigCheck = Long.valueOf(System.currentTimeMillis());
                editor.putLong("LAST_CONFIG_CHECK", lastConfigCheck);
                editor.apply();

            } else {
                Long todayTime = System.currentTimeMillis();
                Long timeElapsed = todayTime - Long.valueOf(lastConfigCheck);
                //check if timeElapsed is 30 minutes. If so, call the method. TODO NEEDS TESTING,
                //TODo 27082017 Eventually we will have to actually use timeElapsed with Settings, but for testing, 1 minute
                //Set 10 minute limit
                if (timeElapsed > 1000) {
                    //DELAY WORKS!;
                    service.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pending);

                    lastConfigCheck = 0L;
                    editor.putLong("LAST_CONFIG_CHECK", lastConfigCheck);
                    editor.apply();
                }else{
                    //Toast.makeText(context, "TimeElapsed has not been reached", Toast.LENGTH_LONG).show();
                }


            }

        } else {
            //Problem is in cameraactivity with client, not here
            Toast.makeText(context, "Your Veritas subscription has expired!", Toast.LENGTH_LONG).show();
        }

    }

        /*cal.add(Calendar.MINUTE, Integer.valueOf(interval));
        service.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                (Integer.valueOf(interval) * 60000), pending);
        */
        //long interval = 1000 * 60 * 1; // 1 minute in milliseconds


        /*long interval = 1000*5; //5 seconds in milliseconds

        //TODO 1007- IMPLEMENTED. launch it once, to save API costs. Anyhow for testing this is fine
        service.setRepeating(
                AlarmManager.RTC_WAKEUP,//type of alarm. This one will wake up the device when it goes off, but there are others, check the docs
                cal.getTimeInMillis(),
                interval,
                pending
        );
        */




   public int setDetectionLimiter(int length){
       int result =0;
        if (length ==0){
            //10 minute limit
           result  = 600000 ;
        }else if(length ==1){
            //30 minute limit
            result =1800000;
        }else if(length ==2){
            //1 hour limit
            result =3600000;
        }else if (length ==3){
            //4 hour  limit
            result = 14400000;

        }else if(length ==4){
            //12 hour limit
            result = 43200000;
        }else if(length ==5){
            //24 hour limit
            result = 86400000;
        }
        return result;
    }

    public boolean checkTrialValidity(){
        boolean result = true;
        trialstart = checkDate.getLong("trialstart",0L);

        if (trialstart==0L){
            //First time using app, create and save time in sharedpreference
//https://stackoverflow.com/questions/17201848/comparing-current-time-in-milliseconds-to-time-saved-in-shared-preferences
            long currentDateTime = System.currentTimeMillis();

            edit.putLong("trialstart",currentDateTime);
            //End is incorrect



            edit.commit();

        }else{
            // One month long twomin = 2592000000l;
            // Three months second value
            long twomin= 7776000000l;
            //Otherwise. fetch both values and check if subscription valid by compare to pre-set time interval
            if (System.currentTimeMillis() - trialstart>twomin) {

                result = false;
            }else {

                result = true;
            }

        }

        return result;
    }



}
    /*

    EDIT : if you are taking about device screen on/off then you need to register
    <action android:name="android.intent.action.USER_PRESENT" />
    and <action android:namhttps://coderwall.com/p/qfoxfg/schedule-a-service-using-alarmmanager
e="android.intent.action.SCREEN_ON" />
    for starting your service when user is present or screen is on
     */

/* BEST GUIDE:https://coderwall.com/p/qfoxfg/schedule-a-service-using-alarmmanager
  https://androidresearch.wordpress.com/2012/07/02/scheduling-an-application-to-start-later/
   http://android-er.blogspot.co.uk/2011/05/using-alarmmanager-to-start-scheduled.html
   For onDestroy/Restarting service http://stackoverflow.com/questions/27376043/using-alarmmanager-in-a-service-to-restart-the-same-service-after-a-period-of-ti
  Using boot completed --  http://stacktips.com/tutorials/android/repeat-alarm-example-in-android

  //To launch an activity in roundabout way, use this alarmreceiver to launch another using getBroadCast
  //which then calls http://stackoverflow.com/questions/3849868/startactivity-from-broadcastreceiver
}
*/
