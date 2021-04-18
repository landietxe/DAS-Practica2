package com.example.practica2.WorkManager;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GuardarImagenLibro  extends Worker {
    public GuardarImagenLibro(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {
        int statusCode=0;
        String user_id = getInputData().getString("user_id");
        String isbn = getInputData().getString("isbn");
        String nombreImagen = getInputData().getString("nombreImagen");
        boolean imagenCambiado = getInputData().getBoolean("imagenCambiado",false);

        if(!imagenCambiado) {//Es la primera vez que este usuario cambia la portada al libro, llamamos al php guardarUsuarioLibroImagen
            String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/guardarUsuarioLibroImagen.php";
            HttpURLConnection urlConnection = null;
            try {
                URL destino = new URL(direccion);
                urlConnection = (HttpURLConnection) destino.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);

                //Generar parámetros
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("user_id", user_id)
                        .appendQueryParameter("isbn", isbn)
                        .appendQueryParameter("nombreImagen", nombreImagen);
                String parametros = builder.build().getEncodedQuery();

                //Hay que configurar el objeto HttpURLConnection para indicar que se envían parámetros
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                //Para incluir los parámetros en la llamada se usa un objeto PrintWriter
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                statusCode = urlConnection.getResponseCode();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {//Anteriormente se ha cambiado la foto del libro, por lo que solamente hay que actualizar el nombre llamando al php "actualizarUsuarioLibroImagen"
            String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/actualizarUsuarioLibroImagen.php";
            HttpURLConnection urlConnection = null;
            try {
                URL destino = new URL(direccion);
                urlConnection = (HttpURLConnection) destino.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);

                //Generar parámetros
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("user_id", user_id)
                        .appendQueryParameter("isbn", isbn)
                        .appendQueryParameter("nombreImagen", nombreImagen);
                String parametros = builder.build().getEncodedQuery();

                //Hay que configurar el objeto HttpURLConnection para indicar que se envían parámetros
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                //Para incluir los parámetros en la llamada se usa un objeto PrintWriter
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                statusCode = urlConnection.getResponseCode();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(statusCode == 200){
            return Result.success();
        }
        return Result.failure();
    }
}
