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
    @Override
    public void onReceive(Context context, Intent intent) {
        SQLiteOpenHelper openHelper = new AlarmClockDB(context);
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor cursor = db.query("ALARMCLOCK", new String[]{"_id", "HOURS", "MINUTES", "MASSIVE"}, null, null, null, null, null);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        AlarmSettings alarmSettings = new AlarmSettings();
        // установка всех будильников после перезагрузки
        while (cursor.moveToNext()){
            int id = cursor.getInt(cursor.getColumnIndex("_id"));
            int hours = cursor.getInt(cursor.getColumnIndex("HOURS"));
            int minutes = cursor.getInt(cursor.getColumnIndex("MINUTES"));
            String days = cursor.getString(cursor.getColumnIndex("MASSIVE"));
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            calendar.set(Calendar.MINUTE, minutes);
            calendar.set(Calendar.SECOND, 0);

            boolean[] checkDays = new boolean[7];
            conversion(days, checkDays);

            alarmSettings.checkTime(calendar, checkDays);

            Intent toReceiverIntent = new Intent(context, AlarmReceiver.class);
            toReceiverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            toReceiverIntent.putExtra("id", id);
            toReceiverIntent.putExtra("hours", hours);
            toReceiverIntent.putExtra("minutes", minutes);
            toReceiverIntent.putExtra("checkDays", checkDays);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, toReceiverIntent, 0);
            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }

        }
        cursor.close();
    }
    public void conversion(String days, boolean[] checkDays){
        for(int i = 0; i < days.toCharArray().length; i++){
            char p = days.toCharArray()[i];
            if(p == '1'){
                checkDays[i] = true;
            }
            else {
                checkDays[i] = false;
            }
        }
    }
}
