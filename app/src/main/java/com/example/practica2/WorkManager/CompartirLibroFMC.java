package com.example.practica2.WorkManager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.simple.JSONObject;

import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class CompartirLibroFMC extends Worker {
    public CompartirLibroFMC(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {

        //obtener tokens y mensaje
        String titulo = getInputData().getString("titulo");
        String autor = getInputData().getString("autor");
        String urlimagen = getInputData().getString("urlimagen");
        String user = getInputData().getString("user");
        String descripcion = getInputData().getString("descripcion");
        String[] tokens = getInputData().getStringArray("tokens");

        //Se genera un objeto HttpURLConnection con la configuración correspondiente
        String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/enviarNotificacion.php";
        HttpURLConnection urlConnection = null;
        Data resultados = null;
        try {
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type","application/json");

            JSONArray tokensJSON = new JSONArray();
            for(String token: tokens){
                tokensJSON.put(token);
            }

            JSONObject parametrosJSON = new JSONObject();
            parametrosJSON.put("titulo",titulo);
            parametrosJSON.put("autor",autor);
            parametrosJSON.put("urlimagen",urlimagen);
            parametrosJSON.put("user",user);
            parametrosJSON.put("descripcion",descripcion);
            parametrosJSON.put("tokens",tokensJSON);

            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(parametrosJSON.toString());
            out.close();

            int statusCode = urlConnection.getResponseCode();

            if (statusCode == 200) {
                return Result.success();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.failure();
    }

}
