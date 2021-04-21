package com.example.practica2.ServicioMusica;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.practica2.R;

import java.io.IOException;

public class ServicioMusica extends Service {

    private final IBinder elBinder= new miBinder();
    private MediaPlayer mediaPlayer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager elmanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel canalservicio = new NotificationChannel("IdCanal",
                    "NombreCanal", NotificationManager.IMPORTANCE_DEFAULT);
            elmanager.createNotificationChannel(canalservicio);
            Notification.Builder builder = new Notification.Builder(this, "IdCanal")
                    .setContentTitle(getString(R.string.app_name))
                    .setAutoCancel(false);
            Notification notification = builder.build();
            startForeground(1, notification);
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.musica);
        mediaPlayer.start();
        mediaPlayer.setLooping(true);
        return START_NOT_STICKY;


    }

    @Override
    public void onDestroy(){
        if(mediaPlayer!=null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        Log.i("info","Servicio creado y enlazado");
        return elBinder;
    }

    public class miBinder extends Binder {
        public ServicioMusica obtenServicio(){
            return ServicioMusica.this;
        }
    }

    public void pararMusica(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        else{
            mediaPlayer.start();
        }

    }

    public void resume() throws IOException {
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

}
