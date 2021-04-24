package com.example.practica2.Actividades;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.practica2.R;
import com.squareup.picasso.Picasso;

import java.util.Locale;

public class LibroCompartido extends AppCompatActivity {

    /* Actividad que muestra la información de un libro compartido por un usuario. Esta actividad
    se abrirá cuando se pulse en la notificación que se crea cuando un usuario comparte uno de sus
    libros.*/


    private ImageView imagen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        //Establecer la vista "activity_libro_compartido"
        setContentView(R.layout.activity_libro_compartido);


        //Obtener los datos enviados por la actividad anterior
        if(getIntent().getExtras() != null){
            String titulo = getIntent().getExtras().getString("titulo");
            String autor = getIntent().getExtras().getString("autor");
            String urlimagen = getIntent().getExtras().getString("urlimagen");
            String user = getIntent().getExtras().getString("user");
            String descripcion = getIntent().getExtras().getString("descripcion");

            //Asignar a los elementos del layout los datos obtenidos
            TextView tvtitulo = findViewById(R.id.tvtitulo);
            tvtitulo.setText(titulo);
            TextView tvautor = findViewById(R.id.tvautor);
            tvautor.setText(autor);
            TextView tvuser = findViewById(R.id.tvusuario);
            tvuser.setText(user);
            TextView tvdescripcion = findViewById(R.id.tvdescripcion);
            tvdescripcion.setText(descripcion);

            //Cargar la imagen original del libro
            imagen = (ImageView) findViewById(R.id.imageView);
            if(urlimagen.equals("")){
                this.imagen.setImageResource(R.drawable.no_cover);
            }
            else{
                //Cargar la imagen
                Glide.with(getApplicationContext()).load(urlimagen.replace("http","https")).into(this.imagen);
            }
        }
    }
}