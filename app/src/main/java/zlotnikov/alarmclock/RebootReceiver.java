package zlotnikov.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import java.util.Calendar;

public class RebootReceiver extends BroadcastReceiver {
    SQLiteDatabase db;
    SQLiteOpenHelper openHelper;
    Cursor cursor;
    AlarmManager alarmManager;
    Intent toReceiverIntent;
    PendingIntent pendingIntent;
    Calendar calendar;
    int id;
    int minutes;
    int hours;
    String days;
    boolean[] checkDays;
    @Override
    public void onReceive(Context context, Intent intent) {
        openHelper = new AlarmClockDB(context);
        db = openHelper.getReadableDatabase();
        cursor = db.query("ALARMCLOCK", new String[]{"_id", "HOURS", "MINUTES", "MASSIVE"}, null, null, null, null, null);
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        calendar = Calendar.getInstance();
        // установка всех будильников после перезагрузки
        while (cursor.moveToNext()){
            id = cursor.getInt(cursor.getColumnIndex("_id"));
            hours = cursor.getInt(cursor.getColumnIndex("HOURS"));
            minutes = cursor.getInt(cursor.getColumnIndex("MINUTES"));
            days = cursor.getString(cursor.getColumnIndex("MASSIVE"));
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            calendar.set(Calendar.MINUTE, minutes);
            calendar.set(Calendar.SECOND, 0);
            checkDays = new boolean[7];
            conversion(days, checkDays);
            checkTime(calendar, checkDays);
            toReceiverIntent = new Intent(context, AlarmReceiver.class);
            toReceiverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            toReceiverIntent.putExtra("id", id);
            toReceiverIntent.putExtra("hours", hours);
            toReceiverIntent.putExtra("minutes", minutes);
            toReceiverIntent.putExtra("checkDays", checkDays);
            pendingIntent = PendingIntent.getBroadcast(context, id, toReceiverIntent, 0);
            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }

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
    public void conversion(String days, boolean[] checkDays){
        for(int i = 0; i < days.toCharArray().length; i++){
            char p = days.toCharArray()[i];
            System.out.println(p);
            if(p == '1'){
                checkDays[i] = true;
            }
            else {
                checkDays[i] = false;
            }
        }
    }
}
