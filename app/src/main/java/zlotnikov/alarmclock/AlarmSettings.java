package zlotnikov.alarmclock;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import java.util.Calendar;

public class AlarmSettings extends AppCompatActivity {
    private Context context;
    private Calendar calendar;
    private AlertDialog daysDialog;
    // массив дней недели для диалог. окна
    private final String[] daysArray = new String[]{"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
    private boolean[] checkDays = new boolean[]{true, true, true, true, true, false, false};
    private TimePicker timePicker;
    // буфер для скопления дней в виде "ПН ВТ .."
    private StringBuffer stringBuffer;
    // буфер для скопления дней в виде "10110"
    private StringBuffer intBuffer;
    private int timePickerHours;
    private int timePickerMinutes;
    // переменная для записи данных stringBuffer в БД
    private String stringDays = "";
    // переменная для записи данных intBuffer в БД
    private String intDays = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);
        context = getApplicationContext();
        Button chooseDays = (Button) findViewById(R.id.choose_days);
        timePicker = (TimePicker) findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        stringBuffer = new StringBuffer();
        intBuffer = new StringBuffer();
        AlertDialog.Builder daysDialogBuilder = new AlertDialog.Builder(this, R.style.myAlertDialog);

        daysDialogBuilder.setTitle(R.string.choosedaysTitle);
        // установка диалог. окна с множественным выбором
        daysDialogBuilder.setMultiChoiceItems(daysArray, checkDays, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                // обновление массива в соответствии с нажатием
                checkDays[which] = isChecked;

            }
        });
        daysDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        daysDialogBuilder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // проверка на существование true в массиве
                for(boolean b : checkDays){
                    if(b){
                        // проверка на установленный день
                        if (checkDays[0]) {stringBuffer.append(" ПН ");}
                        if (checkDays[1]) {stringBuffer.append(" ВТ ");}
                        if (checkDays[2]) {stringBuffer.append(" СР ");}
                        if (checkDays[3]) {stringBuffer.append(" ЧТ ");}
                        if (checkDays[4]) {stringBuffer.append(" ПТ ");}
                        if (checkDays[5]) {stringBuffer.append(" СБ ");}
                        if (checkDays[6]) {stringBuffer.append(" ВС ");}
                        // получение часов и минут для разных версий API
                        if (Build.VERSION.SDK_INT >= 23) {
                            timePickerHours = timePicker.getHour();
                            timePickerMinutes = timePicker.getMinute();
                        } else {
                            timePickerHours = timePicker.getCurrentHour();
                            timePickerMinutes = timePicker.getCurrentMinute();
                        }
                        // инициализация календаря
                        calendar = Calendar.getInstance();
                        // установка часов и минут в календарь
                        calendar.set(Calendar.HOUR_OF_DAY, timePickerHours);
                        calendar.set(Calendar.MINUTE, timePickerMinutes);
                        calendar.set(Calendar.SECOND, 0);
                        // сортировка массива и заполнение буфера
                        sortWeek(checkDays, intBuffer);
                        // проверить когда должен сработать
                        checkTime(calendar, checkDays);
                        // перевод буферов в String для записи в БД
                        stringDays = stringBuffer.toString();
                        intDays = intBuffer.toString();
                        // установка будильника
                        setAlarmClock();
                        // старт активности
                        Intent toMainIntent = new Intent(context, MainActivity.class);
                        startActivity(toMainIntent);
                        break;
                    }
                }
            }
        });
        daysDialog = daysDialogBuilder.create();
        daysDialog.getWindow().setBackgroundDrawableResource(R.drawable.alertdialogbackground);
        chooseDays.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                daysDialog.show();
            }
        });
    }

    public void setAlarmClock(){
        SQLiteOpenHelper openHelper = new AlarmClockDB(context);
        SQLiteDatabase db = openHelper.getWritableDatabase();
        Cursor cursor = db.query("ALARMCLOCK", new String[]{"_id"}, null, null, null, null, null);
        // запись в БД
        AlarmClockDB.insertAlarmClock(db, timePickerHours, timePickerMinutes, stringDays, intDays);
        db = openHelper.getReadableDatabase();
        Intent toReceiverIntent = new Intent(context, AlarmReceiver.class);
        // получения id у последней записи и отправление данных о будильнике
        if (cursor.moveToLast()) {
            int id = cursor.getInt(cursor.getColumnIndex("_id"));
            toReceiverIntent.putExtra("id", id);
            toReceiverIntent.putExtra("hours", timePickerHours);
            toReceiverIntent.putExtra("minutes", timePickerMinutes);
            toReceiverIntent.putExtra("checkDays", checkDays);
            // инициализация отложенного интента
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, toReceiverIntent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        }
        // тостер, оповещающий о создании будильника
        Toast toast = Toast.makeText(context, R.string.alarmClockConfirm, Toast.LENGTH_SHORT);
        toast.show();
        cursor.close();
        db.close();
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
                else {
                    // при других условиях поставить будильник
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
    // метод сортировки. Неделя начинается с воскресенья, а не понедельника.
    private void sortWeek(boolean[] mas, StringBuffer intBuffer){

        boolean temp = mas[mas.length - 1];
        for(int i = mas.length - 1; i > 0; i--){
            mas[i] = mas[i - 1];
        }
        mas[0] = temp;
        for(boolean b : mas){
            if(b){
                intBuffer.append("1");
            } else {
                intBuffer.append("0");
            }
        }
    }
}
