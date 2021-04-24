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
    /*Servicio utilizado para reproducir o parar música.*/

    private final IBinder elBinder= new miBinder();
    private MediaPlayer mediaPlayer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        //Para evitar que el servicio se pare si el SDK del target >=26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Lanzar notificación indicando que se está reproduciendo música
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
        //Objeto MediaPlayer para la reproducción de la música
        mediaPlayer = MediaPlayer.create(this, R.raw.musica);
        mediaPlayer.start();
        mediaPlayer.setLooping(true);
        //Si el servicio se detiene, no se reinicia
        return START_NOT_STICKY;

    }

    @Override
    public void onDestroy(){
        /* Método ejecutado cuando se hace stopService. El método para la música y libera el objeto MediaPlayer */
        if(mediaPlayer!=null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        /* Método ejecutado al vincular una actividad al servicio*/
        Log.i("info","Servicio creado y enlazado");
        return elBinder;
    }

    public class miBinder extends Binder {
        /*Clase que extiende de la clase Binder y que tiene un método que devuelve la instancia del
        servicio para poder acceder a los métodos del servicio desde la actividad.
         */
        public ServicioMusica obtenServicio(){
            return ServicioMusica.this;
        }
    }

    public void pararMusica(){
        /*Método que se ejecuta cuando la música se está reproduciendo y llaman por teléfono.
        Cuando empiece la llamada se parará la música.
         */
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        else{
            mediaPlayer.start();
        }

    }

    public void resume() throws IOException {
        /*Método que se ejecuta cuando termina una llamada si se estaba reproduciendo la música y se habia parado.
        El método vuelve a reproducir la música.
         */
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

}
