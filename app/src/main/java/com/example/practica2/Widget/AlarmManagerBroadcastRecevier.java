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
    @Override
    public void onReceive(Context context, Intent intent) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.widget);
        ArrayList<String> lista = new ArrayList<String>();
        try {
            Resources res = context.getResources();
            BufferedReader reader = new BufferedReader(new InputStreamReader(res.openRawResource(R.raw.books)));
            String line = reader.readLine();
            while(line != null){
                line = reader.readLine();
                lista.add(line);
            }

            Random rand = new Random(); //instance of random class
            int upperbound = lista.size();
            int int_random = rand.nextInt(upperbound);
            String frase= lista.get(int_random);
            String [] datos = frase.split(";");
            String categoria = datos[0];
            String titulo = datos[1];
            String autor = datos[2];

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
