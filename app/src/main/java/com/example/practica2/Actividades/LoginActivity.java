package com.example.practica2.Actividades;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.practica2.R;
import com.example.practica2.BD.miBD;
import com.example.practica2.WorkManager.GuardarTokenFMC;
import com.example.practica2.WorkManager.InsertarUsuario;
import com.example.practica2.WorkManager.ObtenerUsuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*Actividad que permite al usuario iniciar sesión con un nombre y una contraseña
 o abrir otra actividad para registrarse.*/
public class LoginActivity extends AppCompatActivity {
    private EditText usuario;
    private EditText contraseña;
    private miBD gestorDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Obtener preferencias de idioma para actualizar los elementos del layout según el idioma
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idioma = prefs.getString("idioma","es");

        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration configuration = getBaseContext().getResources().getConfiguration();
        configuration.setLocale(nuevaloc);
        configuration.setLayoutDirection(nuevaloc);

        Context context = getBaseContext().createConfigurationContext(configuration);
        getBaseContext().getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());

        //Establecer la vista "activity_login.xml"
        setContentView(R.layout.activity_login);

        //Obtener referencias a los elementos del layout
        usuario = (EditText)findViewById(R.id.editTextUsuario);
        contraseña = (EditText) findViewById(R.id.editTextTextPassword);

        //Obtener la base de datos de la aplicación
        gestorDB = new miBD(this, "Libreria", null, 1);

        //Obtener token del dispositivo
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        String token = task.getResult().getToken();
                        guardarToken(token);
                    }
                });
    }


    public void guardarToken(String token){
        /*Método que se ejecuta al abrir la aplicación. Obtiene el token del dispositivo
        y mediante la clase "GuardarTokenFMC" guarda el token en una base de datos remota en caso de que
        no se haya añadido anteriormente. Si se añade el nuevo token, se muestra un Toast.
        */

        //Se crea un objeto Data para enviar a la tarea el token del dispositivo actual.
        Data datos = new Data.Builder()
                .putString("token",token)
                .build();
        //Ejecuta la tarea de la clase "GuardarTokenFMC"
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(GuardarTokenFMC.class).setInputData(datos).build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if(workInfo != null && workInfo.getState().isFinished()) {
                            //Si se ha añadido el token, mostrar un Toast.
                            if ("SUCCEEDED".equals(workInfo.getState().name())) {
                                String mensaje = getString(R.string.token);
                                Toast toast = Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                                toast.show();
                            }
                        }
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);
    }


    public void onClickLogin(View v) {
        /*Método que se ejecuta cuando el usuario pulsa en el botón de Login.
        Por un lado, se obtienen los datos que el usuario ha introducido y se obtiene  el identificador del usuario
        mediante la clase "ObtenerUsuario". En caso de que el usuario no exista, el identificador será null.
        En caso de existir, se escribe en el fichero externo "usuario_actual.txt" el nombre del usuario y
        su identificador y a continuación se abre la actividad "MainActivityBiblioteca".
        Si el usuario no se ha encontrado en la base de datos se mostrará un Toast.*/

        String user = usuario.getText().toString();
        String password = contraseña.getText().toString();
        if(user != null && password != null) {
            //Se crea un objeto Data para enviar a la tarea el nombre y la contraseña del usuario.
            Data datos = new Data.Builder()
                    .putString("username", user)
                    .putString("password", password)
                    .build();
            //Ejecuta la tarea de la clase "ObtenerUsuario"
            OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ObtenerUsuario.class).setInputData(datos).build();
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                    .observe(this, new Observer<WorkInfo>() {
                        @Override
                        public void onChanged(WorkInfo workInfo) {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                //Obtener los datos del resultado
                                if ("SUCCEEDED".equals(workInfo.getState().name())) {
                                    String result = workInfo.getOutputData().getString("resultados");
                                    JSONParser parser = new JSONParser();
                                    JSONObject json = null;
                                    try {
                                        json = (JSONObject) parser.parse(result);
                                        //Obtiene el identificador de la respuesta json
                                        String id = (String) json.get("user_id");
                                        //Comprueba que el identificador no sea null ( contraseña correcta)
                                        if (id != null) {
                                            login(id, user);
                                        } else {
                                            //Si la contraseña no es correcta mostrar un Toast.
                                            String mensaje = getString(R.string.ususuarioContraseña);
                                            Toast toast = Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT);
                                            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                                            toast.show();
                                        }
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    //Si no se ha podido completar la petición y el usuario no existe, mostrar un Toast.
                                    String mensaje = getString(R.string.usuarioNoExiste);
                                    Toast toast = Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT);
                                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                                    toast.show();
                                }
                            }
                        }
                    });
            WorkManager.getInstance(this).enqueue(otwr);
        }
    }
    public void onClickRegistrar(View v){
        /*Método que se ejecuta cuando el usuario pulsa en el botón de registrarse.Este método
    abrirá la actividad "RegisterActivity" para que el usuario se pueda registrar.*/

        Intent intent = new Intent(this,RegisterActivity.class);
        startActivity(intent);
    }
    public void login (String id,String user){
        /*Método que se ejecuta cuando el usuario ha iniciado sesión con un nombre y contraseña válidos. El método escribe en un
        fichero externo el identificador y el nombre de usuario y a continuación abre la actividad "MainActivityBiblioteca".
         */

        try {
            //Escribir en fichero externo el identificador y nombre del usuario
            OutputStreamWriter fichero = new OutputStreamWriter(openFileOutput("usuario_actual.txt", Context.MODE_PRIVATE));
            fichero.write("id:"+id+"\n"+"Usuario:"+user);
            fichero.close();
        } catch (IOException e){

        }
        //Abrir la actividad MainActivityBiblioteca
        Intent intent = new Intent(this,MainActivityBiblioteca.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }
}