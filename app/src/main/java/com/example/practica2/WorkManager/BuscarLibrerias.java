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

//https://wiki.openstreetmap.org/wiki/Map_features
//https://towardsdatascience.com/loading-data-from-openstreetmap-with-python-and-the-overpass-api-513882a27fd0
public class BuscarLibrerias  extends Worker {
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

            //[out:json];node[shop=books](42.90607549704063,-3.5251062060323615,43.80539710295936,-2.288254193967638);out;
            //Generar parámetros
            String data="[out:json];node[shop=books]"+bbox+";out;";
            System.out.println("DATA" + data);

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("data", data);
            String parametros = builder.build().getEncodedQuery();

            Log.i("etiqueta","GENERANDO PARAMETROS: " + parametros);

            //Hay que configurar el objeto HttpURLConnection para indicar que se envían parámetros
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            System.out.println(urlConnection.toString());

            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(parametros);
            out.close();

            int statusCode = urlConnection.getResponseCode();
            System.out.println(statusCode);
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
