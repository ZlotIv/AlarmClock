package zlotnikov.alarmclock;

import android.app.AlarmManager;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;



// для теста



public class MainActivity extends AppCompatActivity {
    private Context context;
    private AlertDialog.Builder deleteDialogBuilder;
    private AlertDialog.Builder songDialogBuilder;
    private SQLiteDatabase db;
    private Cursor cursor;
    private CursorAdapter adapter;
    private final String[] songs = {"Звонок будильника", "Бетховен - Тишина", "Бетховен - 5 симфония", "Моцарт - Фантазия", "Деревенский будильник"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        ListView listView = (ListView) findViewById(R.id.alarmClock_list);
        ImageView newAlarmClock = (ImageButton) findViewById(R.id.new_alarmClock);
        ImageView chooseButton = (ImageButton) findViewById(R.id.choose_music);
        SQLiteOpenHelper openHelper = new AlarmClockDB(context);
        db = openHelper.getReadableDatabase();
        cursor = db.query("ALARMCLOCK", new String[]{"_id", "HOURS", "MINUTES", "DAYS", "MASSIVE"}, null, null, null, null, null);
        // формирование столбцов сопоставления
        String[] from = new String[]{"HOURS", "MINUTES", "DAYS"};
        int[] to = new int[]{R.id.hours, R.id.minutes, R.id.days};
        deleteDialogBuilder = new AlertDialog.Builder(MainActivity.this, R.style.myAlertDialog);
        songDialogBuilder = new AlertDialog.Builder(MainActivity.this, R.style.myAlertDialog);
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
            AlertDialog deleteDialog = deleteDialogBuilder.create();
            deleteDialog.getWindow().setBackgroundDrawableResource(R.drawable.alertdialogbackground);
            deleteDialog.show();
        }
        });
        // к настройкам
        newAlarmClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toAlarmSettingsIntent = new Intent(context, AlarmSettings.class);
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
    private void deleteAlarmClock(long id){
        // запрос на удаление данных из БД
        db.delete("ALARMCLOCK","_id = ?", new String[]{String.valueOf(id)});
        System.out.println("id в списке " + id);
        Toast toast = Toast.makeText(context, "Будильник удалён", Toast.LENGTH_SHORT);
        toast.show();
        Intent toReceiverIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int)id, toReceiverIntent, 0);
         AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
    private void chooseSong(){
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
        AlertDialog songDialog = songDialogBuilder.create();
        songDialog.getWindow().setBackgroundDrawableResource(R.drawable.alertdialogbackground);
        songDialog.show();

    }
}