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

public class GuardarTokenFMC extends Worker {
    /*Clase que define una tarea para insertar un nuevo token de un dispositivo en la base de datos remota*/

    public GuardarTokenFMC(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {

        /*Método principal en el que se ejecuta la tarea. Por un lado se comprueba si el token del dispositivo actual ya
        se encuentra en la base de datos remota enviando una petición al servicio web "obtenerTokens.php". Este servicio ejecutará la sentencia contra la
         base de datos y devolverá el resultado. En el resultado se comprobará si ya se encuentra el token actual o no.

        En caso de que el token no se encuentre en el resultado, se envía otra petición al servicio web "guardarToken.php" para guardar el token actual
        en la base de datos remota.
         */

        //obtener TOKEN
        String valor = getInputData().getString("token");
        Data resultados = null;
        //Si el token del dispositivo no está guardado, se guarda en la base de datos remota.
        if (!tokenExiste(valor)) {
            //Se genera un objeto HttpURLConnection con la configuración correspondiente
            String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/guardarToken.php";
            HttpURLConnection urlConnection = null;
            try {
                URL destino = new URL(direccion);
                urlConnection = (HttpURLConnection) destino.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);


                //Generar parámetros
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("parametro", valor);
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
                if (statusCode == 200) {
                    return Result.success();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Result.failure();
    }

    /*
    Método que comprueba si el token del dispositivo actual ya está guardado o no. En caso de que no lo esté,
    devuelve false. En caso contrario, devuelve true.
     */
    public boolean tokenExiste(String token) {
        /*Devuelve true si el token actual ya se encuentra en la base de datos remota, y false
        en caso contrario.*/

        //Se genera un objeto HttpURLConnection con la configuración correspondiente
        String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/obtenerTokens.php";
        HttpURLConnection urlConnection = null;
        Data resultados = null;
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
                inputStream.close();
                return result.contains(token);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
