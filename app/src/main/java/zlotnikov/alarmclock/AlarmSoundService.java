package zlotnikov.alarmclock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;


public class AlarmSoundService extends Service {
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;
    private final long mills[] = {1000, 1000, 1000, 1000};
    private int volume = 1;
    private Vibrator vibrator;
    private ChangeVolumeTask changeVolumeTask = new ChangeVolumeTask();
    private int song = R.raw.beethovensilence;
    // для связывания компонентов со службой
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // запуск запускаемой службы
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        SharedPreferences preference = getSharedPreferences("song", MODE_PRIVATE);
        if(intent.getExtras() != null){
            listenSong(intent.getExtras().getInt("song"));
        }
        else if(preference.contains("song")) {
            song = preference.getInt("song", 0);
            alarmClock();
        }
        else{
            alarmClock();
        }
        // сервис  не будет запускаться после смерти
        return START_NOT_STICKY;
    }
    // моментальное отключение звука и вибрации при остановки сервиса
    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        vibrator.cancel();
    }
    class ChangeVolumeTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {
            while (volume < 14) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                volume += 1;
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
    private void alarmClock(){
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(mills, 0);
        // получение аудио файла
        mediaPlayer = MediaPlayer.create(this, song);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        // установка громкости лев/прав.
        // установка повтора
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
        changeVolumeTask.execute();
    }
    private void listenSong(int song){
        mediaPlayer = MediaPlayer.create(this, song);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }
}
