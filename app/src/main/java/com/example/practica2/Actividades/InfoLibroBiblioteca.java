package com.example.practica2.Actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.practica2.Dialogos.DialogoConfirmarBorrar;
import com.example.practica2.Dialogos.DialogoImagen;
import com.example.practica2.R;
import com.example.practica2.BD.miBD;
import com.example.practica2.WorkManager.CompartirLibroFMC;
import com.example.practica2.WorkManager.GuardarImagenLibro;
import com.example.practica2.WorkManager.ObtenerImagenLibro;
import com.example.practica2.WorkManager.ObtenerTokens;
import com.example.practica2.WorkManager.ObtenerUsuario;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
/*Actividad que muestra la información de un libro seleccionado desde el recyclerview de la clase "MainAcitvityBiblioteca" que
 contiene los libros añadidos por el usuario. La actividad permite borrar el libro de la biblioteca del usuario o ver su previsualización.
 */
public class InfoLibroBiblioteca extends AppCompatActivity implements DialogoConfirmarBorrar.ListenerdelDialogo,DialogoImagen.ListenerdelDialogo {

    //Elementos del layout
    private TextView tvTitulo;
    private TextView tvAutor;
    private TextView tvEditorial;
    private TextView tvDescripcion;
    private ImageView imagen;
    private Bitmap bitmapImagen;

    //Base de datos
    private miBD gestorDB;

    //Información del libro
    private String ISBN;
    private String titulo;
    private String autor;
    private String editorial;
    private String descripcion;
    private String urlImagen;
    private String preview;
    private String user_id;
    private String user;

    private Uri uriimagen = null;
    private String imageName;
    private File fichImg = null;
    private boolean imagenCambiado;
    private boolean imagenSubiendo=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagenCambiado=false;

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

        //Establecer la vista "activity_info_libro_biblioteca.xml"
        setContentView(R.layout.activity_info_libro_biblioteca);

        //Obtener la base de datos de la aplicación
        gestorDB = new miBD(this, "Libreria", null, 1);

        //Obtener referencias a los elementos del layout
        tvTitulo = (TextView) findViewById(R.id.info_libro_titulo);
        tvAutor = (TextView) findViewById(R.id.info_libro_autor);
        tvEditorial = (TextView) findViewById(R.id.info_libro_editorial);
        tvDescripcion = (TextView) findViewById(R.id.info_libro_descripcion);
        imagen = (ImageView) findViewById(R.id.info_libro_imagen);


        try {//Obtener nombre  y identificador del Usuario
            BufferedReader ficherointerno = new BufferedReader(new InputStreamReader(openFileInput("usuario_actual.txt")));
            // ficherointerno.readLine();
            String linea = ficherointerno.readLine();
            this.user_id= linea.split(":")[1];
            linea = ficherointerno.readLine();
            this.user= linea.split(":")[1];
            ficherointerno.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //Obtener información pasada desde la actividad anterior
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String isbn = extras.getString("isbn");
            String titulo = extras.getString("titulo");
            String autor = extras.getString("autor");
            String editorial = extras.getString("editorial");
            String descripcion = extras.getString("descripcion");
            String urlimagen = extras.getString("imagen");
            String preview = extras.getString("previewlink");

            this.ISBN = isbn;
            this.titulo = titulo;
            this.autor = autor;
            this.editorial = editorial;
            this.descripcion = descripcion;
            this.urlImagen = urlimagen;
            this.preview = preview;

            this.tvTitulo.setText(titulo);
            this.tvAutor.setText(autor);
            this.tvEditorial.setText(editorial);
            this.tvDescripcion.setText(descripcion);


            Data datos = new Data.Builder()
                    .putString("user_id", user_id)
                    .putString("isbn", ISBN)
                    .build();
            OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ObtenerImagenLibro.class).setInputData(datos).build();
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
            .observe(this, new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        //Obtener los datos del resultado de la conexión asincrona ejecuta desde la clase conexionBDWebService
                        String result = workInfo.getOutputData().getString("resultados");
                        JSONParser parser = new JSONParser();
                        JSONObject json = null;
                        try {
                            json = (JSONObject) parser.parse(result);
                            String nombreImagen = (String) json.get("nombreImagen");
                            if(nombreImagen!=null){
                                imagenCambiado=true;
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();
                                StorageReference pathReference = storageRef.child(nombreImagen);
                                pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Glide.with(getApplicationContext()).load(uri).into(imagen);
                                    }
                                });
                            }
                        } catch (ParseException parseException) {
                            parseException.printStackTrace();
                        }
                        if(!imagenCambiado) { //Si la imagen no ha sido cambiada por el usuario, se carga la original
                            if (urlimagen.equals("")) {
                                imagen.setImageResource(R.drawable.no_cover);
                            } else {
                                //Cargar la imagen original
                                Glide.with(getApplicationContext()).load(urlimagen.replace("http","https")).into(imagen);
                            }
                        }
                    }
                }

            });
            WorkManager.getInstance(this).enqueue(otwr);

        }

    }

    public void onClickBorrar(View v){
        /*Método que se ejecuta cuando el usuario pulsa el botón de borrar el libro de su biblioteca.
        Se abrirá un diálogo de la clase "DialogoConfirmarBorrar" para que el usuario confirme si quiere borrar el libro.*/
        DialogFragment dialogoalerta= new DialogoConfirmarBorrar();
        dialogoalerta.show(getSupportFragmentManager(), "etiqueta");
    }

    @Override
    public void alpulsarSI() {
        /*Método que se ejecuta cuando el usuario pulsa el botoón "Sí" en el dialogo de borrar el libro
        de su biblioteca. Por un lado, se lee del fichero "usuario_actual.txt" cual es el identificador del usuario actual. Con ese identificador,
        se llama al método "borrarUsuarioLibro" de la base de datos para quitar el libro al usuario. Después se abre una notificación
        indicando que el libro ha sido borrado. Por último, se vuelve a abrir la actividad "MainActivityBiblioteca".*/

        //Quitar libro a usuario
        gestorDB.borrarUsuarioLibro(this.ISBN,this.user_id);

        //Crear notificación indicando que se ha eliminado el libro de la biblioteca
        NotificationManager elManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(this, "IdCanal");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel elCanal = new NotificationChannel("IdCanal", "Notificación libro",
                    NotificationManager.IMPORTANCE_DEFAULT);

            elCanal.setDescription("Notificación libro eliminado");
            elCanal.enableLights(true);
            elCanal.setLightColor(Color.RED);
            elCanal.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            elCanal.enableVibration(false);

            elManager.createNotificationChannel(elCanal);
        }

        String contentTitle = getString(R.string.notificacionBorrar1);
        String contentText =  getString(R.string.notificacionBorrar2);
        String subText = getString(R.string.notificacionBorrar1);

        elBuilder.setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setAutoCancel(true);

        elManager.notify(1, elBuilder.build());

        //Abrir actividad "MainActivityBiblioteca"
        Intent newIntent = new Intent(this, MainActivityBiblioteca.class);
        startActivity(newIntent);
        finish();


    }

    @Override
    public void alpulsarNO() {
        /* Método que se ejecuta cuando el usuario pulsa el bóton "No" en el diálogo de borrar el libro
        de su biblioteca. Se abrirá un Toast indicando que el libro no se ha borrado.*/
        String mensaje = getString(R.string.toastNoBorrado);
        Toast toast = Toast.makeText(this, mensaje, Toast.LENGTH_SHORT);
        toast.setGravity( Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();

    }
    @Override
    public void onBackPressed() {
        /*Método que se ejecuta cuando el usuario pulsa el bóton del móvil para volver hacia atras.
          El método abrirá la actividad anterior a la actual, en este caso, MainActivityBiblioteca y finalizará la
          actividad actual.*/
        if (!imagenSubiendo) {
            Context context = getApplicationContext();
            Intent newIntent = new Intent(context, MainActivityBiblioteca.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newIntent);
            finish();
        }
    }

    public void onClickPreview(View v){
        /*Método que se ejecuta cuando el usuario pulsa el botón de previsualizar el libro.
        Este método abre un intent implícito que muestra en el navegador una previsualización del libro.*/
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(this.preview));
        startActivity(i);
    }

    public void onClickCompartir(View v){
        String titulo = this.titulo;
        String autor = this.autor;
        String urlimagen = this.urlImagen;
        String user=this.user;
        String descripcion =this.descripcion;

        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ObtenerTokens.class).build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            try {
                                //Obtener los datos del resultado de la conexión asincrona ejecuta desde la clase conexionBDWebService
                                JSONArray jsonArray = new JSONArray(workInfo.getOutputData().getString("resultados"));
                                List<String> lista = new ArrayList<String>();
                                for(int i = 0; i < jsonArray.length(); i++)
                                {
                                    String token = jsonArray.getString(i);
                                    lista.add(token);
                                }
                                enviarNotificacion(lista.toArray(new String[0]),titulo,autor,urlimagen,user,descripcion);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);
    }
    public void enviarNotificacion(String[] lista,String titulo,String autor,String urlimagen,String user,String descripcion){
        Data datos = new Data.Builder()
                .putString("titulo",titulo)
                .putString("autor",autor)
                .putString("urlimagen",urlimagen)
                .putString("user",user)
                .putString("descripcion",descripcion)
                .putStringArray("tokens",lista)
                .build();

        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(CompartirLibroFMC.class).
                setInputData(datos)
                .build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if(workInfo != null && workInfo.getState().isFinished()){
                            Toast.makeText(getApplicationContext(),"Libro compartido",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);
    }
    public void onClickCambiarFoto(View v){
        if(comprobarPermisos()) {
            String titulo = getString(R.string.cambiarPortada);
            String texto = getString(R.string.cambiarPortada2);
            DialogFragment dialogoImagen = new DialogoImagen(titulo, texto);
            dialogoImagen.show(getSupportFragmentManager(), "etiqueta");
        }

    }


    @Override
    public void alpulsarObtenerDeGaleria() {
        Intent elIntentGal = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(elIntentGal, 10);


    }

    @Override
    public void alpulsarSacarFoto() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombrefich = "IMG_" + timeStamp + "_";
        File directorio=this.getFilesDir();

        try {
            fichImg = File.createTempFile(nombrefich, ".jpg",directorio);
            uriimagen = FileProvider.getUriForFile(this, "com.example.practica2.provider", fichImg);
        } catch (IOException e) {
        }
        Intent elIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        elIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriimagen);
        startActivityForResult(elIntent, 11);

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 10 && resultCode == RESULT_OK) { //Foto de galeria
            uriimagen = data.getData();
            imageName=uriimagen.toString().split("%2F")[uriimagen.toString().split("%2F").length-1];
            guardarImagen();
        }
        if (requestCode == 11 && resultCode == RESULT_OK) { //Foto tomada con teléfono
            imageName=uriimagen.toString().split("/")[uriimagen.toString().split("/").length-1];
            guardarImagen();
        }


    }

    public void guardarImagen(){
        try {
            bitmapImagen = reescalarImagen();
            imagen.setRotation(90);
            imagen.setImageBitmap(bitmapImagen);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Subir imagen a Firebase
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference spaceRef = storageRef.child(imageName);

        imagenSubiendo=true;
        String texto = getString(R.string.guardandoImagen);
        Toast.makeText(getApplicationContext(),texto,Toast.LENGTH_SHORT).show();
        UploadTask uploadTask = spaceRef.putFile(uriimagen);
        try {
            Thread.sleep(6000); // Delay para que se suba la foto a Firebase y se pueda cargar correctamente

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imagenSubiendo=false;
                guardarEnBD();
            }
        });

    }
    public void guardarEnBD(){
        //Subir nombre de la imagen a la BD remota
        Data datos = new Data.Builder()
                .putString("user_id",user_id)
                .putString("isbn",ISBN)
                .putString("nombreImagen",imageName)
                .putBoolean("imagenCambiado",imagenCambiado)
                .build();

        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(GuardarImagenLibro.class).
                setInputData(datos)
                .build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if(workInfo != null && workInfo.getState().isFinished()){
                            String texto = getString(R.string.imagenGuardada);
                            Toast.makeText(getApplicationContext(),texto,Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);



        //Borrar fichero temporal
        boolean deleted = false;
        if(fichImg!=null) {
            try {
                deleted = fichImg.delete();
            } catch (SecurityException e) {
            }
            if (!deleted) {
                fichImg.deleteOnExit();
            }
        }
    }
    public  Bitmap reescalarImagen() throws IOException {

        Bitmap bitmapFoto = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uriimagen);
        int anchoDestino = imagen.getWidth();
        int altoDestino = imagen.getHeight();
        int anchoImagen = bitmapFoto.getWidth();
        int altoImagen = bitmapFoto.getHeight();
        float ratioImagen = (float) anchoImagen / (float) altoImagen;
        float ratioDestino = (float) anchoDestino / (float) altoDestino;
        int anchoFinal = anchoDestino;
        int altoFinal = altoDestino;
        if (ratioDestino > ratioImagen) {
            anchoFinal = (int) ((float) altoDestino * ratioImagen);
        } else {
            altoFinal = (int) ((float) anchoDestino / ratioImagen);
        }
        Bitmap bitmapredimensionado = Bitmap.createScaledBitmap(bitmapFoto, anchoFinal, altoFinal, true);

        return bitmapredimensionado;
    }



    public boolean comprobarPermisos(){

        String [] permisos = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO

            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR
            }
            //PEDIR Permisos
            ActivityCompat.requestPermissions(this, permisos,
                    0);

        } else {
            //EL PERMISO ESTÁ CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 0:{
                // Si la petición se cancela, granResults estará vacío
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // PERMISO CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
                    String titulo = getString(R.string.cambiarPortada);
                    String texto = getString(R.string.cambiarPortada2);
                    DialogFragment dialogoImagen = new DialogoImagen(titulo, texto);
                    dialogoImagen.show(getSupportFragmentManager(), "etiqueta");
                }
                else {// PERMISO DENEGADO, DESHABILITAR LA FUNCIONALIDAD O EJECUTAR ALTERNATIVA
                }
                return;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //Método que guarda la uri de la imagen
        super.onSaveInstanceState(outState);
        outState.putParcelable("file_uri", uriimagen);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Método que recupera la uri de la imagen
        uriimagen = savedInstanceState.getParcelable("file_uri");
    }
}