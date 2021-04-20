package com.example.practica2.Widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.RemoteViews;

import com.example.practica2.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WidgetConfigureActivity WidgetConfigureActivity}
 */
public class Widget extends AppWidgetProvider {

    private AlarmManager am;
    private PendingIntent pi;
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        //CharSequence widgetText = MiWidgetDemoConfigureActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        //views.setTextViewText(R.id.appwidget_text, widgetText);

        ArrayList<String> lista = new ArrayList<String>();
        try {
            Resources res = context.getResources();
            BufferedReader reader = new BufferedReader(new InputStreamReader(res.openRawResource(R.raw.books)));
            String line = reader.readLine();
            while(line != null){
                lista.add(line);
                line = reader.readLine();
            }

            Random rand = new Random(); //instance of random class
            int upperbound = lista.size();
            int int_random = rand.nextInt(upperbound);
            String frase= lista.get(int_random);
            String [] datos = frase.split(";");
            String categoria = datos[0];
            String titulo = datos[1];
            String autor = datos[2];

            views.setTextViewText(R.id.categoria, categoria);
            views.setTextViewText(R.id.titulo, titulo);
            views.setTextViewText(R.id.autor, autor);
            reader.close();
        } catch (Exception e) {

        }



        Intent intent = new Intent(context,Widget.class);
        intent.setAction("com.example.Practica2.ACTUALIZAR_WIDGET");
        intent.putExtra( AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                7768, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.elboton, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);//Actualiza el widget en concreto con los cambios
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            WidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        // Enter relevant functionality for when the first widget is created
        Intent intent = new Intent(context, AlarmManagerBroadcastRecevier.class);
        pi = PendingIntent.getBroadcast(context, 7475, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+ 1000 * 3, 60000 , pi);
    }

    @Override
    public void onDisabled(Context context) {
        am.cancel(pi);
    }

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("com.example.Practica2.ACTUALIZAR_WIDGET")) {
            int widgetId = intent.getIntExtra( AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateAppWidget(context, widgetManager, widgetId);
            }
        }
        super.onReceive(context,intent);
    }
}