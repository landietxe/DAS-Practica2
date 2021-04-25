package com.example.practica2.Actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;


import com.example.practica2.Dialogos.DialogoConfirmarBorrar;
import com.example.practica2.Dialogos.DialogoImagen;
import com.example.practica2.Fragments.fragmentBiblioteca;
import com.example.practica2.Fragments.fragmentInfoLibroBibliotecaLand;
import com.example.practica2.R;
import com.example.practica2.BD.miBD;
import com.example.practica2.ServicioMusica.ServicioMusica;
import com.example.practica2.WorkManager.GuardarImagenLibro;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*Actividad que muestra los libros que el usuario tenga añadidos en su biblioteca. Esta actividad está compuesta de fragments
* para tener un comportamiento diferente según la orientación en la que se encuentre el móvil*/
public class MainActivityBiblioteca extends AppCompatActivity implements fragmentBiblioteca.listenerDelFragment,fragmentInfoLibroBibliotecaLand.listener2,
        DialogoConfirmarBorrar.ListenerdelDialogo,DialogoImagen.ListenerdelDialogo{

    private String ISBN;
    private miBD gestorDB;
    private String user_id;
    private String ordenLibros;
    private Toolbar toolbar;
    private String nombreUsuario="";

    //Imagen libro
    private ImageView imageView;
    private int numClicks=0;
    private String isbnClick="";
    private Uri uriimagen = null;
    private String imageName;
    private File fichImg = null;
    private boolean imagenCambiado=false;
    private Bitmap bitmapImagen;
    private boolean imagenSubiendo=false;

    //Menú desplegable
    DrawerLayout elmenudesplegable;

    //Servicio Música
    private ServicioMusica elservicio;
    private boolean reproduciendo=false;
    private static boolean mBound;
    private ServiceConnection laconexion= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            elservicio= ((ServicioMusica.miBinder)service).obtenServicio();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            elservicio=null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagenCambiado=false;

        /*Obtener preferencias de idioma para actualizar los elementos del layout según el idioma y el orden
        en el que se quiere ordenar los libros*/
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idioma = prefs.getString("idioma","es"); //Idioma
        String orden = prefs.getString("orden","title"); //Orden
        this.ordenLibros=orden;

        Locale nuevaloc = new Locale(idioma);
        Locale.setDefault(nuevaloc);
        Configuration configuration = getBaseContext().getResources().getConfiguration();
        configuration.setLocale(nuevaloc);
        configuration.setLayoutDirection(nuevaloc);

        Context context = getBaseContext().createConfigurationContext(configuration);
        getBaseContext().getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());

        //Establecer la vista "activity_main_biblioteca.xml"
        setContentView(R.layout.activity_main_biblioteca);

        //Obtener nombre del Usuario
        try {
            BufferedReader ficherointerno = new BufferedReader(new InputStreamReader(openFileInput("usuario_actual.txt")));
            ficherointerno.readLine();
            String linea = ficherointerno.readLine();
            this.nombreUsuario= linea.split(":")[1];
            ficherointerno.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Añadir toolbar al layout
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(this.nombreUsuario);
        setSupportActionBar(toolbar);

        //Añdir menu desplegable al layout
        elmenudesplegable = findViewById(R.id.drawer);
        NavigationView elnavigation = findViewById(R.id.elnavigationview);
        elnavigation.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
           @Override
           public boolean onNavigationItemSelected(@NonNull MenuItem item) {
               switch (item.getItemId()){
                   case R.id.mapa:
                       /*Botón de buscar librerias del menú desplegable. El método comprueba
                       que la aplicación dispone del permiso ACESS_FINE_LOCATION, y de ser así, abre la actividad
                       "GoogleMaps"*/

                       if(pedirPermisoLocalizacion()){
                           Intent intent = new Intent(getApplicationContext(), GoogleMaps.class);
                           startActivity(intent);
                       }
                       break;
                   case R.id.logout:
                        /*Botón de cerrar sesión del menú desplegable. El método finaliza la actividad actual
                         y abre la actividad "LoginActivity*/
                       Context context = getApplicationContext();
                       Intent newIntent = new Intent(context, LoginActivity.class);
                       newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                       context.startActivity(newIntent);
                       finish();
                       break;
                   case R.id.play:
                       /*Botón de reproducir la música del menú desplegable. Si la aplicación contiene el permiso READ_PHONE_STATE,
                       se comprueba que no haya un servicio ya enlazado anteriormente. De ser así, se enlaza el servicio de la clase
                       "ServicioMusica". Después, si no se esta reproduciendo la música, se lanza el servicio en segundo plano y se reproduce
                       la música.
                        */
                       if (permisoEstadoTelefono()) {
                           if (!mBound) {
                               Intent myService = new Intent(getApplicationContext(), ServicioMusica.class);
                               bindService(myService, laconexion, Context.BIND_AUTO_CREATE);
                               mBound = true;
                           }
                           if (!reproduciendo) {
                               Intent myService = new Intent(getApplicationContext(), ServicioMusica.class);
                               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                   startForegroundService(myService);
                               } else {
                                   startService(myService);
                               }
                               bindService(myService, laconexion, Context.BIND_AUTO_CREATE);
                               reproduciendo = true;
                           }
                       }
                       break;
                   case R.id.stop:
                       /*Botón de parar la música del menú desplegable. Si el servicio está en marcha, se quita el enlace con el servicio
                       y se para la música.*/
                       Intent intent = new Intent(getApplicationContext(), ServicioMusica.class);
                       stopService(intent);
                       if (isMyServiceRunning(ServicioMusica.class)) {
                           unbindService(laconexion);
                           System.out.println("Servicio detenido");
                       }
                       reproduciendo = false;
                       break;

               }
               elmenudesplegable.closeDrawers();
               return false;
           }
       });
        getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_dialog_info);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Obtener referencia a los elementos del menú desplegable
        Menu menu = elnavigation.getMenu();
        // Indicar cual es el nombre del usuario actual
        MenuItem nav_user = menu.findItem(R.id.user);
        nav_user.setTitle(nav_user.getTitle().toString() +": " + nombreUsuario);


        //Obtener la base de datos de la aplicación
        gestorDB = new miBD(this, "Libreria", null, 1);

    }

    @Override
    public void seleccionarElemento(String isbn, String title, String autores, String editorial, String descripcion, String thumbnail, String previewLink, ImageView imageview, boolean cambiado) {
        /*Método que se ejecuta cuando el usuario selecciona uno de sus libros. Por un lado se comprueba la orientación en la que
        se encuentra el móvil. Si el móvil está en vertical, se abrirá la actividad "InfoLibroBiblioteca" pasandole los datos del libro.
        Si el móvil está en horizontal, ya existe otro fragment en el layout, por lo que se hace cast a su clase y se llama al método
        "actualizar" para visualizar los datos. También se gestiona cuando el usuario hace click 2 veces en un mismo libro para poder cambiar su imagen.*/
        this.ISBN=isbn;
        this.imageView=imageview;
        this.imagenCambiado=cambiado;
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE){ //Pantalla en horizontal, usamos el otro fragment
            if(this.ISBN.equals(isbnClick)){
                //Ha vuelto a click-ar por segunda vez en la misma imagen, abrimos diálogo para cambiar la imagen.
                if(comprobarPermisos()) {
                    //Si la aplicación tiene los permisos CAMERA y WRITE_EXTERNAL_STORAGE se abre el diálogo de seleccionar una nueva imagen
                    String titulo = getString(R.string.cambiarPortada);
                    String texto = getString(R.string.cambiarPortada2);
                    DialogFragment dialogoImagen = new DialogoImagen(titulo, texto);
                    dialogoImagen.show(getSupportFragmentManager(), "etiqueta");
                }
            }
            else{
                isbnClick=this.ISBN;
                fragmentInfoLibroBibliotecaLand elotro=(fragmentInfoLibroBibliotecaLand) getSupportFragmentManager().findFragmentById(R.id.fragment4);
                elotro.actualizar(isbn,title,autores,editorial,descripcion,previewLink,thumbnail);
            }
        }
        else{ //Pantalla en vertical, abrimos nueva actividad
            Intent i= new Intent(this,InfoLibroBiblioteca.class);
            i.putExtra("isbn",isbn);
            i.putExtra("titulo", title);
            i.putExtra("autor", autores);
            i.putExtra("editorial", editorial);
            i.putExtra("descripcion", descripcion);
            i.putExtra("imagen", thumbnail);
            i.putExtra("previewlink",previewLink);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Método que se ejecuta cuando se pulsa alguno de los botones de la Toolbar.
        int id=item.getItemId();
        switch (id){
            case R.id.opcion1:{  //Botón buscar, abrirá la actividad "MainActivity"
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("id",this.user_id);
                startActivity(intent);
                finish();
                return true;
            }
            case R.id.opcion2:{//Boton Ajustes,abrirá la actividad "PreferenciasActivity"
                Intent intent = new Intent(this, PreferenciasActivity.class);
                startActivity(intent);
                return true;
            }
            //Botón de información, abre el menu desplegable.
            case android.R.id.home:
                elmenudesplegable.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Asignar el fichero xml con la definición del menú a la Toolbar
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }


    @Override
    public void seleccionarElemento() {
        /*Método implementado del listener de fragmentInfoBibliotecaLand"
        El método se ejecutará cuando el usuario pulse el botón de borrar el libro cuando tenga el móvil en orientación
        horizontal y abrirá un diálogo de la clase "DialogoConfirmarBorrar"*/
        DialogFragment dialogoalerta= new DialogoConfirmarBorrar();
        dialogoalerta.show(getSupportFragmentManager(), "etiqueta");
    }

    @Override
    public void alpulsarSI() {
        /*Método que se ejecuta cuando el usuario pulsa el botoón "Sí" en el dialogo de borrar el libro
        de su biblioteca. Por un lado, se lee del fichero "usuario_actual.txt" cual es el identificador del usuario actual. Con ese identificador,
        se llama al método "borrarUsuarioLibro" de la base de datos para quitar el libro al usuario. Después se abre una notificación
        indicando que el libro ha sido borrado. Por último, se recarga la actividad.*/

        //Obtener identificador del usuario actual
        try {
            BufferedReader ficherointerno = new BufferedReader(new InputStreamReader(openFileInput("usuario_actual.txt")));
            String linea = ficherointerno.readLine();
            this.user_id= linea.split(":")[1]; //id:num
            ficherointerno.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        //Recargar la actividad
        Intent intent = getIntent();
        finish();
        startActivity(intent);


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
    public void onBackPressed(){
        /*Método que se ejecuta cuando el usuario pulsa el bóton del móvil para volver hacia atras. Si el menu desplegable está abierto,
        lo cierra. En caso contrario, el método abrirá la actividad anterior a la actual, en este caso, "LoginActivity" y finalizará la
          actividad actual.*/

        //Si el menu desplegable está abierto, lo cierra
        if (elmenudesplegable.isDrawerOpen(GravityCompat.START)) {
            elmenudesplegable.closeDrawer(GravityCompat.START);
        } else {
            Context context = getApplicationContext();
            Intent newIntent = new Intent(context, LoginActivity.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newIntent);
            finish();
        }
    }

    public boolean pedirPermisoLocalizacion(){

        /*Método que comprueba si la aplicación tiene el permiso "ACESS_FINE_LOCATION"
        En caso de que el usuario no lo haya concedido, se le pide el permiso. Si el usuario
        no quiere dar el permiso, el método devuelve false por lo que no se ejecutará la funcionalidad. En caso contrario
        devuelve true.*/

        //PEDIR PERMISOS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO

            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR
            }
            //PEDIR EL PERMISO
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);

        } else {//EL PERMISO ESTÁ CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
            return true;
        }
        return false;
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

        /*Método que se ejecuta una vez terminadas las acciones de obtener la foto de la galería o sacando una nueva foto.
        En ambos casos, se obtiene el nombre de la imagen y se llama al método "guardarImagen".*/
        if (requestCode == 10 && resultCode == RESULT_OK) { //Foto de galeria
            uriimagen = data.getData();
            imageName= new File(uriimagen.getPath()).getName();
            guardarImagen();
        }
        if (requestCode == 11 && resultCode == RESULT_OK) { //Foto tomada con teléfono
            imageName = new File(uriimagen.getPath()).getName();
            guardarImagen();
        }
    }

    public void guardarImagen() {
        /*En este método se sube la imagen a Firebase y en caso de que se suba correctamente,
         se llama al método "guardarEnBD" para guardar la referencia
         de la iamgen en la base de datos Remota.
         */

        //Obtener identificador del usuario actual
        try {
            BufferedReader ficherointerno = new BufferedReader(new InputStreamReader(openFileInput("usuario_actual.txt")));
            String linea = ficherointerno.readLine();
            this.user_id = linea.split(":")[1]; //id:num
            ficherointerno.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //Subir imagen a Firebase
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference spaceRef = storageRef.child(imageName);
        UploadTask uploadTask = spaceRef.putFile(uriimagen);
        String texto = getString(R.string.guardandoImagen);
        Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_SHORT).show();
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imagenSubiendo = false;
                System.out.println("Imagen subida a firebase" + imagenCambiado);
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

        //Recargar la actividad
        Intent intent = getIntent();
        finish();
        startActivity(intent);
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
                    2);

        } else {
            //EL PERMISO ESTÁ CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
            return true;
        }
        return false;
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Método que guarda la uri de la imagen y el estado de la música cuando se rota el dispositivo
        outState.putBoolean("reproduciendo",reproduciendo);
        outState.putParcelable("file_uri", uriimagen);
        outState.putBoolean("imagenCambiado",imagenCambiado);
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //Método que recupera la uri de la imagen y el estado de la música cuando se rota el dispositivo
        this.reproduciendo=savedInstanceState.getBoolean("reproduciendo");
        uriimagen = savedInstanceState.getParcelable("file_uri");
        imagenCambiado=savedInstanceState.getBoolean("imagenCambiado");
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        /*Método para comprobar si un servicio está ejecutándose:
        //https://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android*/
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onStop(){
        super.onStop();
        if(mBound && reproduciendo){
            unbindService(laconexion);
            mBound=false;
        }
    }

    public boolean permisoEstadoTelefono(){
        /*Método que comprueba si el usuario ha concedido a la aplicación el permisos READ_PHONE_STATE.
        En caso de que no lo haya concedido, se le pide. Si el usuario no quiere dar el permiso,
        el método devuelve false por lo que no se ejecutará la funcionalidad. En caso contrario
        devuelve true.*/

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            //EL PERMISO NO ESTÁ CONCEDIDO, PEDIRLO
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                // MOSTRAR AL USUARIO UNA EXPLICACIÓN DE POR QUÉ ES NECESARIO EL PERMISO

            } else {
                //EL PERMISO NO ESTÁ CONCEDIDO TODAVÍA O EL USUARIO HA INDICADO
                //QUE NO QUIERE QUE SE LE VUELVA A SOLICITAR
            }
            //PEDIR EL PERMISO
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},
                    3);

        } else {
            return true;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        /*Método que se ejecuta cuando el usuario concede los permisos. Según el código del permiso, se
        ejecutará una funcionalidad diferente.*/


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 0:{ //Permiso ACCESS_FINE_LOCATION, abre la actividad "GoogleMaps"
                // Si la petición se cancela, granResults estará vacío
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // PERMISO CONCEDIDO, EJECUTAR LA FUNCIONALIDAD
                    Intent intent = new Intent(this, GoogleMaps.class);
                    startActivity(intent);

                }
                else {// PERMISO DENEGADO, DESHABILITAR LA FUNCIONALIDAD O EJECUTAR ALTERNATIVA
                }
                return;
            }
            case 2:{ //Permisos de CAMERA y WRITE_EXTERNAL_STORAGE, abre el diálogo de seleccionar la imagen
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
            case 3:{ //Permiso de READ_PHONE_STATE, reproduce la música
                // Si la petición se cancela, granResults estará vacío
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!mBound) {
                        Intent myService = new Intent(getApplicationContext(), ServicioMusica.class);
                        bindService(myService, laconexion, Context.BIND_AUTO_CREATE);
                        mBound = true;
                    }
                    System.out.println("REPRODUCIR," + reproduciendo);
                    if (!reproduciendo) {
                        Intent myService = new Intent(getApplicationContext(), ServicioMusica.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(myService);
                        } else {
                            startService(myService);
                        }
                        bindService(myService, laconexion, Context.BIND_AUTO_CREATE);
                        reproduciendo = true;
                    }

                }
                else {// PERMISO DENEGADO, DESHABILITAR LA FUNCIONALIDAD O EJECUTAR ALTERNATIVA
                }
                return;
            }
        }
    }


}