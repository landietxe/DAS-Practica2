package com.example.practica2.WorkManager;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class ObtenerImagenLibro extends Worker {
    /*Clase que define una tarea para obtener el nombre de la imagen que un usuario ha sacado a un libro.*/

    public ObtenerImagenLibro(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @NonNull
    @Override
    public ListenableWorker.Result doWork() {

        /*Método principal en el que se ejecuta la tarea. Este método envía una petición al servicio web
        "obtenerUsuarioLibroImagen.php" para obtener el nombre de la imagen que un usuario ha sacado
        a su libro. Este servicio ejecutará la sentencia contra la base de datos y
        devolverá el  nombre de la imagen como resultado.
         */

        String user_id = getInputData().getString("user_id");
        String isbn = getInputData().getString("isbn");

        Data resultados = null;
        String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/obtenerUsuarioLibroImagen.php";
        HttpURLConnection urlConnection = null;

        try {
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            //Generar parámetros
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("user_id", user_id)
                    .appendQueryParameter("isbn", isbn);
            String parametros = builder.build().getEncodedQuery();

            //Hay que configurar el objeto HttpURLConnection para indicar que se envían parámetros
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            //Para incluir los parámetros en la llamada se usa un objeto PrintWriter
            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(parametros);
            out.close();

            //Ejecuta la llamada al servicio web
            int statusCode = urlConnection.getResponseCode();
            //Mirar el código de vuelta y procesar los datos
            if(statusCode == 200){
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
