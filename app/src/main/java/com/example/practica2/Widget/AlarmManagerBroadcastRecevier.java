package com.example.practica2.Widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.RemoteViews;

import com.example.practica2.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

public class AlarmManagerBroadcastRecevier  extends BroadcastReceiver {
    /*Clase que extiende de BroadcastReceiver para la actualizar
    el widget automáticamente.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.widget);
        ArrayList<String> lista = new ArrayList<String>();
        //Recorrer el fichero books.txt y añadir cada línea a un ArrayList
        try {
            Resources res = context.getResources();
            BufferedReader reader = new BufferedReader(new InputStreamReader(res.openRawResource(R.raw.books)));
            String line = reader.readLine();
            while(line != null){
                line = reader.readLine();
                lista.add(line);
            }

            //Obtener un número random
            Random rand = new Random();
            int upperbound = lista.size();
            int int_random = rand.nextInt(upperbound);
            //Obtener los datos de un libro aleatoriamente
            String frase= lista.get(int_random);
            String [] datos = frase.split(";");
            String categoria = datos[0];//Categoria
            String titulo = datos[1];//Título
            String autor = datos[2];//Autor

            //Asignar los datos a los elementos del widget
            remoteViews.setTextViewText(R.id.categoria, categoria);
            remoteViews.setTextViewText(R.id.titulo, titulo);
            remoteViews.setTextViewText(R.id.autor, autor);
            reader.close();
        } catch (Exception e) {
            // e.printStackTrace();
            //txtHelp.setText("Error: can't show help.");
        }
        ComponentName tipowidget = new ComponentName(context, Widget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(tipowidget, remoteViews);

    }
}
