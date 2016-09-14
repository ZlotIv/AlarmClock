package zlotnikov.alarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    // интент для запуска активности
    Intent toWakeUpIntent;
    @Override
    public void onReceive(Context context, Intent intent) {
        toWakeUpIntent = new Intent(context, WakeUp.class);
        toWakeUpIntent.putExtra("id", intent.getExtras().getInt("id"));
        toWakeUpIntent.putExtra("hours", intent.getExtras().getInt("hours"));
        toWakeUpIntent.putExtra("minutes", intent.getExtras().getInt("minutes"));
        toWakeUpIntent.putExtra("checkDays", intent.getExtras().getBooleanArray("checkDays"));
        // установка флага для интента
        toWakeUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.getApplicationContext().startActivity(toWakeUpIntent);
        Log.e("Лог", "во втором ресивере");
    }
}
