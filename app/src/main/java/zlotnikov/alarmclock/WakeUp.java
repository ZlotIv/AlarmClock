package zlotnikov.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import java.util.Calendar;

public class WakeUp extends AppCompatActivity {
    // интент для получения данных
    Intent getIntent;
    // алярм для установки нового будильника
    AlarmManager alarmManager;
    // интент для установки будильника
    Intent toReceiverIntent;
    // отложенный интент для установки будильника
    PendingIntent pendingIntent;
    // кнопка STOP
    Button stopButton;
    // кнопка повтора
    Button repeatButton;
    // отложенный интент для повтора
    PendingIntent repeatPendingIntent;
    // интент для повтора
    Intent repeatIntent;
    // id повторного будильника
    int repeatId;
    Context context;
    // интент для сервиса
    Intent toAlarmSoundService;
    // календарь
    Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wake_up);
        // получение контекста
        context = getApplicationContext();
        stopButton = (Button) findViewById(R.id.stopButton);
        repeatButton = (Button) findViewById(R.id.repeatButton);
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
                finishApp();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(toAlarmSoundService);
                finishApp();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopService(toAlarmSoundService);
        finishApp();
    }

    // установка следующего будильника
    public void nextAlarmClock(){
        toReceiverIntent = new Intent(context, AlarmReceiver.class);
        toReceiverIntent.putExtra("id", getIntent.getExtras().getInt("id"));
        toReceiverIntent.putExtra("hours", getIntent.getExtras().getInt("hours"));
        toReceiverIntent.putExtra("minutes", getIntent.getExtras().getInt("minutes"));
        toReceiverIntent.putExtra("checkDays", getIntent.getExtras().getBooleanArray("checkDays"));
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getIntent.getExtras().getInt("hours"));
        calendar.set(Calendar.MINUTE, getIntent.getExtras().getInt("minutes"));
        calendar.set(Calendar.SECOND, 0);
        checkTime(calendar, getIntent.getExtras().getBooleanArray("checkDays"));
        pendingIntent = PendingIntent.getBroadcast(context, getIntent.getExtras().getInt("id"), toReceiverIntent, 0);
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
    // установка будильника через 5 минут
    public void repeatAlarmClock(){
        repeatIntent = new Intent(context, AlarmReceiver.class);
        repeatId = 0;
        repeatIntent.putExtra("id", repeatId);
        repeatPendingIntent = PendingIntent.getBroadcast(context, 0, repeatIntent, 0);
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5*60000, repeatPendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5*60000, repeatPendingIntent);
        }
    }

    // алгоритм установки будильника по дням недели
    public void checkTime(Calendar calendar, boolean[] mas){
        // получение сегодняшнего дня недели
        int today = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        // счетчик от одного будильника до другого
        int emptyDays = 0;
        // переменная следующей недели
        boolean nextWeek = false;
        for(int i = today; ; i++){
            // если i-й день содержит будильник
            if(mas[i]){
                // если i-й день сегодня
                if(i == today){
                    // и время будильника не наступило
                    if(calendar.getTimeInMillis() > System.currentTimeMillis()){
                        break;
                    }
                    // если будильник должен прозвенеть ровно через неделю
                    if(calendar.getTimeInMillis() <= System.currentTimeMillis() && nextWeek){
                        calendar.add(Calendar.DAY_OF_WEEK, 7);
                        break;
                    }
                    // иначе
                    else {
                        // если сегодня воскресенье
                        if (i == 6) {
                            // перенести цикл на следующую неделю, прибавить к счетчику день, новой неделе присвоить true
                            emptyDays++;
                            i = -1;
                            nextWeek = true;
                        }
                        // иначе
                        else {
                            // прибавить к счетчику день
                            emptyDays++;
                        }
                    }
                }
                // при других условиях поставить будильник
                else {
                    calendar.add(Calendar.DAY_OF_WEEK, emptyDays);
                    break;
                }
            }
            // иначе, если i-й день не содержит будильник
            else if(!mas[i]){
                // и если сегодня воскресенье
                if (i == 6) {
                    // перенести цикл на следующую неделю, прибавить к счетчику день, новой неделе присвоить true
                    emptyDays++;
                    i = -1;
                    nextWeek = true;
                }
                // иначе просто прибавить к счетчику день
                else {
                    emptyDays++;
                }
            }
        }
    }
    // полное завершение приложения
    public void finishApp(){
        if (Build.VERSION.SDK_INT >= 21)
        {
            finishAndRemoveTask();
        }
        else
        {
            if (Build.VERSION.SDK_INT >= 16)
            {
                finishAffinity();
            } else
            {
                finish();
            }
        }
        Process.killProcess(Process.myPid());
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // очистка флагов
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
