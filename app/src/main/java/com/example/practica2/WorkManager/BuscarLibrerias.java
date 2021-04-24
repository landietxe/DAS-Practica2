package com.example.practica2.WorkManager;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLOutput;

public class BuscarLibrerias  extends Worker {
    /*Clase que define una tarea para obtener las librerías que se encuentren dentro de una área especificada.
        Basado en el ejemplo de la página "Loading Data from OpenStreetMap with Python and the Overpass API"
        //https://towardsdatascience.com/loading-data-from-openstreetmap-with-python-and-the-overpass-api-513882a27fd0
    * */
    public BuscarLibrerias(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {
        Data resultados = null;
        String direccion = "https://overpass-api.de/api/interpreter";
        HttpURLConnection urlConnection = null;
        String bbox= getInputData().getString("bbox");
        System.out.println("BOUNDING BOX" + bbox);
        try {
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(30000);

            //Generar parámetros: (obtener respuesta en json y los "nodos" que sean tiendas de libros)
            String data="[out:json];node[shop=books]"+bbox+";out;";
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("data", data);
            String parametros = builder.build().getEncodedQuery();

            Log.i("etiqueta","GENERANDO PARAMETROS: " + parametros);

            //Hay que configurar el objeto HttpURLConnection para indicar que se envían parámetros
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            //Para incluir los parámetros en la llamada se usa un objeto PrintWriter
            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(parametros);
            out.close();

            //Ejecuta la llamada al servicio web
            int statusCode = urlConnection.getResponseCode();

            //Mirar el código de vuelta y procesar los datos
            if (statusCode == 200) {
                System.out.println("CODIGO 200");
                System.out.println(urlConnection.getResponseMessage());
                BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line, result = "";
                while ((line = bufferedReader.readLine()) != null) {
                    result += line;
                }
                resultados = new Data.Builder().putString("resultados",result).build();
                inputStream.close();
                return Result.success(resultados);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.failure();
    }
}
