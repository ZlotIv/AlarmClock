package zlotnikov.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import java.util.Calendar;

public class WakeUp extends AppCompatActivity {
    private Intent getIntent;
    private AlarmManager alarmManager;
    private Context context;
    private Intent toAlarmSoundService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wake_up);
        // получение контекста
        context = getApplicationContext();
        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setText(R.string.stop);
        Button repeatButton = (Button) findViewById(R.id.repeatButton);
        stopButton.setText(R.string.repeatAlarmClock);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        toAlarmSoundService = new Intent(context, AlarmSoundService.class);
        getIntent = getIntent();
        startService(toAlarmSoundService);
        if(getIntent.getExtras().getInt("id") != 0) {nextAlarmClock();}
        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(toAlarmSoundService);
                repeatAlarmClock();
                finish();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(toAlarmSoundService);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopService(toAlarmSoundService);
        finish();
    }

    // установка следующего будильника
    public void nextAlarmClock(){
        Intent toReceiverIntent = new Intent(context, AlarmReceiver.class);
        toReceiverIntent.putExtra("id", getIntent.getExtras().getInt("id"));
        toReceiverIntent.putExtra("hours", getIntent.getExtras().getInt("hours"));
        toReceiverIntent.putExtra("minutes", getIntent.getExtras().getInt("minutes"));
        toReceiverIntent.putExtra("checkDays", getIntent.getExtras().getBooleanArray("checkDays"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getIntent.getExtras().getInt("hours"));
        calendar.set(Calendar.MINUTE, getIntent.getExtras().getInt("minutes"));
        calendar.set(Calendar.SECOND, 0);
        AlarmSettings alarmSettings = new AlarmSettings();
        alarmSettings.checkTime(calendar, getIntent.getExtras().getBooleanArray("checkDays"));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, getIntent.getExtras().getInt("id"), toReceiverIntent, 0);
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
    // установка будильника через 5 минут
    public void repeatAlarmClock(){
        Intent repeatIntent = new Intent(context, AlarmReceiver.class);
        int repeatId = 0;
        repeatIntent.putExtra("id", repeatId);
        PendingIntent repeatPendingIntent = PendingIntent.getBroadcast(context, 0, repeatIntent, 0);
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5*60000, repeatPendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5*60000, repeatPendingIntent);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
