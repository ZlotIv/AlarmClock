package zlotnikov.alarmclock;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AlarmClockDB extends SQLiteOpenHelper {
    // переменные для названия ии версии БД
    private static final String DB_NAME = "alarmClockDataBase";
    private static final int DB_VERSION = 1;

    public AlarmClockDB(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        // переопределение конструктора
        super(context, DB_NAME, null, DB_VERSION);
    }
    // созданный вручную упрощенный конструктор
    public AlarmClockDB(Context context){
        super(context, DB_NAME, null, DB_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // создание таблицы
        db.execSQL("CREATE TABLE ALARMCLOCK ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "HOURS INTEGER, "
                + "MINUTES INTEGER, "
                + "DAYS TEXT, "
                + "MASSIVE TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    // метод-шаблон для заполнения БД
    public static void insertAlarmClock(SQLiteDatabase db, int hours, int minutes, String days, String massive){
        ContentValues contentValues = new ContentValues();
        contentValues.put("HOURS", hours);
        contentValues.put("MINUTES", minutes);
        contentValues.put("DAYS", days);
        contentValues.put("MASSIVE", massive);
        db.insert("ALARMCLOCK", null, contentValues);
    }
}