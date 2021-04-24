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
 contiene los libros añadidos por el usuario. La actividad permite borrar el libro de la biblioteca del usuario,ver su previsualización,
 compartirlo a los demás usuarios de la aplicación o cambiarle la foto de la portada.
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


            //Se crea un objeto Data para enviar a la tarea el identificador del usuario y el ISBN del libro.
            Data datos = new Data.Builder()
                    .putString("user_id", user_id)
                    .putString("isbn", ISBN)
                    .build();
            //Ejecuta la tarea de la clase "ObtenerImagenLibro" para obtener el nombre de la imagen cuando el usuario haya cambiado la portada del libro.
            OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ObtenerImagenLibro.class).setInputData(datos).build();
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
            .observe(this, new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        //Obtener los datos del resultado de la tarea.
                        String result = workInfo.getOutputData().getString("resultados");
                        JSONParser parser = new JSONParser();
                        JSONObject json = null;
                        try {
                            json = (JSONObject) parser.parse(result);
                            //Obtener el nombre de la imagen
                            String nombreImagen = (String) json.get("nombreImagen");
                            //Si el nombre de la imagen no es null, significa que el usuario ha cambiado la imagen del libro.
                            if(nombreImagen!=null){
                                imagenCambiado=true;
                                //Se obtiene del almacenamiento de Firebase la dirección de la imagen
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();
                                StorageReference pathReference = storageRef.child(nombreImagen);
                                pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        //Se carga la imagen al imageView de la actividad.
                                        Glide.with(getApplicationContext()).load(uri).into(imagen);
                                    }
                                });
                            }
                        } catch (ParseException parseException) {
                            parseException.printStackTrace();
                        }

                        if(!imagenCambiado) {
                            //Si la imagen no ha sido cambiada por el usuario, se carga la original
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

    public void onClickPreview(View v){
        /*Método que se ejecuta cuando el usuario pulsa el botón de previsualizar el libro.
        Este método abre un intent implícito que muestra en el navegador una previsualización del libro.*/
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(this.preview));
        startActivity(i);
    }

    public void onClickCompartir(View v){

        /* Método que se ejecuta cuando el usuario pulsa el botón de compartir el libro.
        Este método obtiene los tokens de los dispositivos registrados en la aplicación y llama al
        método "enviarNotificación" para enviar una notificación a todos los tokens compartiendo el libro.
        ha compartido.*/

        String titulo = this.titulo;
        String autor = this.autor;
        String urlimagen = this.urlImagen;
        String user=this.user;
        String descripcion =this.descripcion;

        //Ejecuta la tarea de la clase "ObtenerTokens" para obtener los tokens de los dispositivos.
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ObtenerTokens.class).build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            try {
                                //Obtener los datos del resultado de la conexión asincrona
                                JSONArray jsonArray = new JSONArray(workInfo.getOutputData().getString("resultados"));
                                List<String> lista = new ArrayList<String>();
                                //Recorrer los resultados y obtener los tokens
                                for(int i = 0; i < jsonArray.length(); i++)
                                {
                                    String token = jsonArray.getString(i);
                                    lista.add(token);
                                }
                                //Llamar al método "enviarNotificación" pasándole como parametro la lista de tokens y los datos del libro
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
        /*Este método envía mediante una tarea de Firebase una notificación a todos los tokens mostrando
        los datos del libro que el usuario ha compartido.*/

        //Se crea un objeto Data para enviar a la tarea la lista de tokens y los datos del libro.
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
        //Ejecuta la tarea de la clase "CompartirLibroFMC" para envíar la notificación a todos los tokens.
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if(workInfo != null && workInfo.getState().isFinished()){
                            //Se muestra un Toast indicando que el libro ha sido compartido.
                            Toast.makeText(getApplicationContext(),"Libro compartido",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);
    }
    public void onClickCambiarFoto(View v){
        /* Método que se ejecuta cuando un usuario pulsa en el botón para cambiar la imagen de portada de un libro.
        Por un lado, se le piden al usuario los permisos CAMERA y WRITE_EXTERNAL_STORAGE. En caso de que el usuario acepte
        los permisos, se crea un diálogo preguntando de donde desea obtener la nueva foto del libro; cargándolo de una foto
        existente de la galería o sacando una nueva foto.*/
        if(comprobarPermisos()) {
            String titulo = getString(R.string.cambiarPortada);
            String texto = getString(R.string.cambiarPortada2);
            DialogFragment dialogoImagen = new DialogoImagen(titulo, texto);
            dialogoImagen.show(getSupportFragmentManager(), "etiqueta");
        }

    }


    @Override
    public void alpulsarObtenerDeGaleria() {
        /*Método que se ejecuta cuando el usuario pulsa el bóton "Galería" en el diálogo de como obtener
        la nueva foto  del libro. Se creará un nuevo Intent para que el usuario seleccione la imagen desde su galería.*/
        Intent elIntentGal = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(elIntentGal, 10);
    }

    @Override
    public void alpulsarSacarFoto() {
        /*Método que se ejecuta cuando el usuario pulsa el bóton "Foto" en el diálogo de como obtener
        la nueva foto  del libro. Se creará un nuevo Intent para que el usuario pueda sacar una nueva foto
        utilizando la cámara del dispositivo*/

        //Indicamos un nombre único para el fichero
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombrefich = "IMG_" + timeStamp + "_";
        File directorio=this.getFilesDir();

        //Para gestionar la imagen en tamaño completo es necesario almacenarla primero, por lo que se asigna donde se va a guardar.
        try {
            fichImg = File.createTempFile(nombrefich, ".jpg",directorio);
            uriimagen = FileProvider.getUriForFile(this, "com.example.practica2.provider", fichImg);
        } catch (IOException e) {
        }

        //Se crea el nuevo Intent para abrir la cámara del dispositivo
        Intent elIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        elIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriimagen);
        startActivityForResult(elIntent, 11);

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*Método que se ejecuta una vez terminadas las acciones de obtener la foto de la galería o sacando una nueva foto.
        En ambos casos, se obtiene el nombre de la imagen y se llama al método "guardarImagen".*/
        if (requestCode == 10 && resultCode == RESULT_OK) { //Foto de galeria
            uriimagen = data.getData();
            imageName=new File(uriimagen.getPath()).getName();
            guardarImagen();
        }
        if (requestCode == 11 && resultCode == RESULT_OK) { //Foto tomada con teléfono
            imageName=new File(uriimagen.getPath()).getName();
            guardarImagen();
        }


    }

    public void guardarImagen(){
        /*En este método por un lado se llama al método "reescalarImagen" para escalar la imagen
         al tamaño que se va a mostrar, pero manteniendo su aspecto. Después, se sube la imagen a Firebase
         y en caso de que se suba correctamente, se llama al método "guardarEnBD" para guardar la referencia
         de la iamgen en la base de datos Remota.
         */


        //Escalar la imagen
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
                //Si la imagen se ha subido correctamente, llamamos al método "guardarEnBD"
                guardarEnBD();
            }
        });

    }
    public void guardarEnBD(){
        /*Método para guardar la referencia de una imagen de Firebase en la base de datos remota.
        Una vez guardada la imagen, se borra el fichero temporal que se creó (en caso de que la imagen haya sido
        sacada con la cámara)*/

        //Se crea un objeto Data para enviar a la tarea el nombre de la imagen, el isbn del libro, el identificador del usuario
        //y una variable que indica si es la primera vez que se cambia la foto o no.
        Data datos = new Data.Builder()
                .putString("user_id",user_id)
                .putString("isbn",ISBN)
                .putString("nombreImagen",imageName)
                .putBoolean("imagenCambiado",imagenCambiado)
                .build();

        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(GuardarImagenLibro.class).
                setInputData(datos)
                .build();
        //Ejecuta la tarea de la clase "GuardarImagenLibro" para guardar el nombre de la imagen
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

        /*Método que escala la imagen al tamaño que se van a mostrar, pero
        manteniendo su aspecto*/

        //bitmapFoto tiene cargada la imagen en tamaño original
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

        //bitmapredimensionado contiene la imagen escalada al tamaño del destino
        Bitmap bitmapredimensionado = Bitmap.createScaledBitmap(bitmapFoto, anchoFinal, altoFinal, true);

        return bitmapredimensionado;
    }


    public boolean comprobarPermisos(){

        /*Método que comprueba si el usuario ha concedido a la aplicación los permisos de
        CAMERA y WRITE_EXTERNAL_STORAGE. En caso de que no los haya concedido, se le piden. Si el usuario
        no quiere dar los permisos, el método devuelve false por lo que no se ejecutará la funcionalidad. En caso contrario
        devuelve true.*/

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
        /*
        Método que se ejecuta cuando el usuario concede los permisos. En este caso, ejecuta la misma funcionalidad
        del método "onClickCambiarFoto"
         */
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
        //Método que guarda la uri de la imagen cuando se rota el dispositivo
        super.onSaveInstanceState(outState);
        outState.putParcelable("file_uri", uriimagen);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Método que recupera la uri de la imagen cuando se rota el dispositivo
        uriimagen = savedInstanceState.getParcelable("file_uri");
    }


    @Override
    public void onBackPressed() {
        /*Método que se ejecuta cuando el usuario pulsa el bóton del móvil para volver hacia atras.
          El método abrirá la actividad anterior a la actual, en este caso, MainActivityBiblioteca y finalizará la
          actividad actual.*/
        Context context = getApplicationContext();
        Intent newIntent = new Intent(context, MainActivityBiblioteca.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newIntent);
        finish();
    }


}