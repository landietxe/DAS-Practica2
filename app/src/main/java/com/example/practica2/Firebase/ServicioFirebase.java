package com.example.practica2.Firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.example.practica2.Actividades.LibroCompartido;
import com.example.practica2.Actividades.LoginActivity;
import com.example.practica2.Libro;
import com.example.practica2.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Locale;

public class ServicioFirebase extends FirebaseMessagingService {

    /*Clase que implementa un servicio que extienda de FirebaseMessagingService para poder recibir mensajes
    y enviar notificaciones.*/

    private String titulo;
    private String autor;
    private String urlimagen;
    private String user;
    private String descripcion;
    public ServicioFirebase() {}

    public void onMessageReceived(RemoteMessage remoteMessage) {


        //Obtener preferencias de idioma para actualizar los elementos del layout según el idioma
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idioma = prefs.getString("idioma","es");

        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration configuration = getBaseContext().getResources().getConfiguration();
        configuration.setLocale(nuevaloc);
        configuration.setLayoutDirection(nuevaloc);

        Context context = getBaseContext().createConfigurationContext(configuration);
        getBaseContext().getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());


        //El mensaje viene con datos
        if (remoteMessage.getData().size() > 0) {
            titulo = remoteMessage.getData().get("titulo");
            autor = remoteMessage.getData().get("autor");
            urlimagen = remoteMessage.getData().get("urlimagen");
            user = remoteMessage.getData().get("user");
            descripcion = remoteMessage.getData().get("descripcion");
        }
        //El mensaje es una notificación
        if (remoteMessage.getNotification() != null) {
            NotificationManager elManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(this, "IdCanal");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel elCanal = new NotificationChannel("IdCanal", "Notificación",
                        NotificationManager.IMPORTANCE_DEFAULT);

                elCanal.setDescription("Notificación");
                elCanal.enableLights(true);
                elCanal.setLightColor(Color.RED);
                elCanal.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                elCanal.enableVibration(false);
                elManager.createNotificationChannel(elCanal);
            }


            if(remoteMessage.getNotification().getClickAction().equals("MENSAJE")){

                //Asignar los datos que se van a enviar a la siguiente actividad si el usuario pulsa en la notificación
                Intent i = new Intent(this, LibroCompartido.class);
                i.putExtra("titulo",titulo);
                i.putExtra("autor",autor);
                i.putExtra("urlimagen",urlimagen);
                i.putExtra("user",user);
                i.putExtra("descripcion",descripcion);

                PendingIntent intentEnNot = PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT);

                //Mostrar notificación cuando la aplicación esté en primer plano
                String tituloNotificacion = getString(R.string.notificacionLibroCompartido);
                String mensaje =getString(R.string.notificacionLibroCompartido2);
                elBuilder.setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setContentTitle(tituloNotificacion)
                        .setContentText(mensaje)
                        .setSubText("FMC")
                        .setVibrate(new long[]{0, 1000, 500, 1000})
                        .setAutoCancel(true)
                        .setContentIntent(intentEnNot); //Abir la actividad al darle a la notificación cuando está en primer plano
            }
            elManager.notify(1, elBuilder.build());
        }
    }


}
