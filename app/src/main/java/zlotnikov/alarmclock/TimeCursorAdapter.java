package zlotnikov.alarmclock;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

class TimeCursorAdapter extends SimpleCursorAdapter {

     TimeCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
         super(context, layout, c, from, to, flags);
    }

    // связывание данных с ячейкой списка
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        // получение часов, минут, дней
        int hours = cursor.getInt(cursor.getColumnIndex("HOURS"));
        int minutes = cursor.getInt(cursor.getColumnIndex("MINUTES"));
        String days = cursor.getString(cursor.getColumnIndex("DAYS"));
        // получение TextView часов, минут, дней
        TextView hourTV = (TextView) view.findViewById(R.id.hours);
        TextView minutesTV = (TextView) view.findViewById(R.id.minutes);
        TextView daysTV = (TextView) view.findViewById(R.id.days);
        if(hours >= 0 || hours <= 9){
            hourTV.setText(String.format("%02d", hours));
        }
        else {
            hourTV.setText(String.valueOf(hours));
        }
        if(minutes >= 0 || minutes <= 9){
            minutesTV.setText(String.format("%02d", minutes));
        }
        else {
            minutesTV.setText(String.valueOf(minutes));
        }
        daysTV.setText(days);
    }
}
