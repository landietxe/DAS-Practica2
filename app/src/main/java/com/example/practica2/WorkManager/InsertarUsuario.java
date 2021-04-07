package com.example.practica2.WorkManager;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
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

public class InsertarUsuario  extends Worker {

    public InsertarUsuario(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {

        String username = getInputData().getString("username");
        String password = getInputData().getString("password");

        if (!this.usuarioExiste(username)) { //Si no existe un usuario con el nombre introducido, se crea uno nuevo
            System.out.println("EL USUARIO NO EXISTE");
            String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/insertarUsuario.php";
            HttpURLConnection urlConnection = null;

            try {
                URL destino = new URL(direccion);
                urlConnection = (HttpURLConnection) destino.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);

                //Generar parámetros
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("username", username)
                        .appendQueryParameter("password", password);
                String parametros = builder.build().getEncodedQuery();

                //Hay que configurar el objeto HttpURLConnection para indicar que se envían parámetros
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                //Para incluir los parámetros en la llamada se usa un objeto PrintWriter
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();

                int statusCode = urlConnection.getResponseCode();
                System.out.println("STATUS CODE: " + statusCode);
                if(statusCode == 200){
                    return Result.success();
                }
                else{
                    return Result.failure();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("EL USUARIO EXISTE");
        }
        return Result.failure();
    }

    public boolean usuarioExiste(String username){
        //Se genera un objeto HttpURLConnection con la configuración correspondiente
        String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/usuarioExiste.php";
        HttpURLConnection urlConnection = null;
        try {
            URL destino = new URL(direccion);
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            //Generar parámetros
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("username", username);
            String parametros = builder.build().getEncodedQuery();

            //Hay que configurar el objeto HttpURLConnection para indicar que se envían parámetros
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            //Para incluir los parámetros en la llamada se usa un objeto PrintWriter
            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(parametros);
            out.close();

            int statusCode = urlConnection.getResponseCode();

            if (statusCode == 200) {
                BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line, result = "";
                while ((line = bufferedReader.readLine()) != null) {
                    result += line;

                }
                inputStream.close();


                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(result);

                String id = (String) json.get("user_id");
                if(id != null){
                   return true;
               }
            }
        }
        catch (Exception e) {
        e.printStackTrace();
        }
        return false;
    }
}
