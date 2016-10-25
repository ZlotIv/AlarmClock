package zlotnikov.alarmclock;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Context context;
    AlarmManager alarmManager;
    AlertDialog.Builder deleteDialogBuilder;
    AlertDialog.Builder songDialogBuilder;
    // переменные для того, чтобы сделать диалоги с тупыми углами
    Dialog deleteDialog;
    Dialog songDialog;
    // интент для удаления
    Intent toReceiverIntent;
    PendingIntent pendingIntent;
    SQLiteOpenHelper openHelper;
    SQLiteDatabase db;
    Cursor cursor;
    CursorAdapter adapter;
    public final String TAG = this.getClass().getSimpleName();
    // интент для переключения работы на другую активность
    Intent toAlarmSettingsIntent;
    ListView listView;
    ImageButton newAlarmClock;
    ImageButton chooseButton;
    Button negativeButton;
    Button positiveButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        listView = (ListView) findViewById(R.id.alarmClock_list);
        newAlarmClock = (ImageButton) findViewById(R.id.new_alarmClock);
        chooseButton = (ImageButton) findViewById(R.id.choose_music);
        openHelper = new AlarmClockDB(context);
        db = openHelper.getReadableDatabase();
        cursor = db.query("ALARMCLOCK", new String[]{"_id", "HOURS", "MINUTES", "DAYS", "MASSIVE"}, null, null, null, null, null);
        // формирование столбцов сопоставления
        String[] from = new String[]{"HOURS", "MINUTES", "DAYS"};
        int[] to = new int[]{R.id.hours, R.id.minutes, R.id.days};
        deleteDialogBuilder = new AlertDialog.Builder(MainActivity.this, R.style.myAlertDialog);
        songDialogBuilder = new AlertDialog.Builder(MainActivity.this, R.style.myAlertDialog);


        // НЕ ЗАБЫТЬ УДАЛИТЬ!!!



        //db.delete("ALARMCLOCK", null, null);
        while(cursor.moveToNext()){
            System.out.println(cursor.getInt(cursor.getColumnIndex("_id"))+ " " + cursor.getInt(cursor.getColumnIndex("HOURS")) + " " + cursor.getString(cursor.getColumnIndex("MASSIVE")));
        }



        adapter = new TimeCursorAdapter(context, R.layout.list_item_layout, cursor, from, to, 0);
        listView.setAdapter(adapter);
        // диалоговое окно для удаления будильника
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {@Override
        public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
            deleteDialogBuilder.setTitle("Удалить будильник?");
            deleteDialogBuilder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            deleteDialogBuilder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteAlarmClock(id);
                    // обновление курсора после удаления данных из БД
                    cursor = db.query("ALARMCLOCK", new String[]{"_id", "HOURS", "MINUTES", "DAYS"}, null, null, null, null, null);
                    adapter.changeCursor(cursor);

                    dialog.cancel();
                }
            });
            deleteDialog = deleteDialogBuilder.create();
            deleteDialog.getWindow().setBackgroundDrawableResource(R.drawable.alertdialogbackground);
            deleteDialog.show();
        }
        });
        // к настройкам
        newAlarmClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toAlarmSettingsIntent = new Intent(context, AlarmSettings.class);
                startActivity(toAlarmSettingsIntent);
            }
        });
        // выбрать мелодию
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseSong();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }
    public void deleteAlarmClock(long id){
        // запрос на удаление данных из БД
        db.delete("ALARMCLOCK","_id = ?", new String[]{String.valueOf(id)});
        System.out.println("id в списке " + id);
        Toast toast = Toast.makeText(context, "Будильник удалён", Toast.LENGTH_SHORT);
        toast.show();
        toReceiverIntent = new Intent(context, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(context, (int)id, toReceiverIntent, 0);
        alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        try {
            alarmManager.cancel(pendingIntent);
            Log.e(TAG, "Будильние удалён");
        }catch (Exception e){
            Log.e(TAG, "НЕ удалось удалить будильник");
        }
    }
    public void chooseSong(){
        final Intent toAlarmSoundService = new Intent(context, AlarmSoundService.class);
        // префенс для загрузки мелодии
        SharedPreferences loadSong = getSharedPreferences("song", MODE_PRIVATE);
        // утсновка "выбора" на установленной мелодии
        int song = 1;
        if(loadSong.contains("song")) {
            song = loadSong.getInt("song", 0);
            switch (song){
                case R.raw.alarmclock:
                    song = 0;
                    break;
                case R.raw.beethovensilence:
                    song = 1;
                    break;
                case R.raw.beethovensymphony5:
                    song = 2;
                    break;
                case R.raw.mozartfantasy:
                    song = 3;
                    break;
                case R.raw.ruralalarmclock:
                    song = 4;
                    break;
            }
        }
        // преференс для сохранения мелодии
        SharedPreferences saveSong = getSharedPreferences("song", MODE_PRIVATE);
        final SharedPreferences.Editor saveSongEditor = saveSong.edit();
        final String[] songs = {"Звонок будильника", "Бетховен - Тишина", "Бетховен - 5 симфония", "Моцарт - Фантазия", "Деревенский будильник"};
        songDialogBuilder.setTitle("Выберите мелодию для будильника");
        songDialogBuilder.setNegativeButton("Закрыть", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopService(toAlarmSoundService);
                dialog.cancel();
            }
        });
        songDialogBuilder.setPositiveButton("Выбрать", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveSongEditor.apply();
                stopService(toAlarmSoundService);
                dialog.cancel();
            }
        });
        songDialogBuilder.setSingleChoiceItems(songs, song, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                stopService(toAlarmSoundService);
                                saveSongEditor.putInt("song", R.raw.alarmclock);
                                toAlarmSoundService.putExtra("song", R.raw.alarmclock);
                                startService(toAlarmSoundService);
                                break;
                            case 1:
                                stopService(toAlarmSoundService);
                                saveSongEditor.putInt("song", R.raw.beethovensilence);
                                toAlarmSoundService.putExtra("song", R.raw.beethovensilence);
                                startService(toAlarmSoundService);
                                break;
                            case 2:
                                stopService(toAlarmSoundService);
                                saveSongEditor.putInt("song", R.raw.beethovensymphony5);
                                toAlarmSoundService.putExtra("song", R.raw.beethovensymphony5);
                                startService(toAlarmSoundService);
                                break;
                            case 3:
                                stopService(toAlarmSoundService);
                                saveSongEditor.putInt("song", R.raw.mozartfantasy);
                                toAlarmSoundService.putExtra("song", R.raw.mozartfantasy);
                                startService(toAlarmSoundService);
                                break;
                            case 4:
                                stopService(toAlarmSoundService);
                                saveSongEditor.putInt("song", R.raw.ruralalarmclock);
                                toAlarmSoundService.putExtra("song", R.raw.ruralalarmclock);
                                startService(toAlarmSoundService);
                                break;
                            default:
                                break;
                        }
                    }
                });
        songDialog = songDialogBuilder.create();
        songDialog.getWindow().setBackgroundDrawableResource(R.drawable.alertdialogbackground);
        songDialog.show();

    }
}