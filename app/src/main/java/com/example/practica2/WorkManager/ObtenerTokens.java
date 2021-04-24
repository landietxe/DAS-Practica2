package com.example.practica2.WorkManager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
/*Clase que define una tarea para obtener los tokens de los dispositivos guardados en la base de datos remota.
 */
public class ObtenerTokens  extends Worker {
    public ObtenerTokens(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {

        /*Método principal en el que se ejecuta la tarea. Este método envía una petición al servicio web "obtenerTokens.php".
        Este servicio ejecutará la sentencia contra la base de datos y devolverá el resultado. El resultado será una lista con los tokens
        guardados en la base de datos remota.
         */

        Data resultados = null;
        String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/obtenerTokens.php";
        HttpURLConnection urlConnection = null;
        try {
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            //Ejecuta la llamada al servicio web
            int statusCode = urlConnection.getResponseCode();
            //Mirar el código de vuelta y procesar los datos
            if (statusCode == 200) {
                BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line, result = "";
                while ((line = bufferedReader.readLine()) != null) {
                    result += line;

                }
                resultados = new Data.Builder().putString("resultados",result).build();
                inputStream.close();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success(resultados);
    }
}
