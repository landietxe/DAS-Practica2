package com.example.practica2.WorkManager;

import android.content.Context;
import android.net.Uri;

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
/*Clase que define una tarea para comprobar si el usuario y la contraseña introducidos son correctos y existen en la base de datos remota.
 */
public class ObtenerUsuario extends Worker {
    public ObtenerUsuario(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        /*Método principal en el que se ejecuta la tarea. Por un lado se comprueba si un usuario existe en la base de datos
        remota enviando una petición al servicio web "usuarioExiste.php". Este servicio ejecutará la sentencia contra la base de datos y
        devolverá el resultado. El resultado será null si el usuario no existe y en caso contrario, será el identificador del usuario.

        En caso de que el usuario exista, se envía otra petición al servicio web "obtenerUsuario.php" para comprobar que la contraseña
        introducida es correcta.
         */

        String username = getInputData().getString("username");
        String password = getInputData().getString("password");

        //Si el usuario existe, comprobamos que la contraseña introducida es correcta
        if (this.usuarioExiste(username)) {
            String direccion = "http://ec2-54-167-31-169.compute-1.amazonaws.com/lechevarria008/WEB/Practica2/obtenerUsuario.php";
            HttpURLConnection urlConnection = null;
            Data resultados = null;

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
