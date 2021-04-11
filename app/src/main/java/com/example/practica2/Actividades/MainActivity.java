package com.example.practica2.Actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Logger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.practica2.AdaptadorRecycler;
import com.example.practica2.Dialogos.DialogoConfirmarBorrar;
import com.example.practica2.Dialogos.DialogoImagen;
import com.example.practica2.Libro;
import com.example.practica2.R;
import com.google.android.gms.common.util.JsonUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/*Actividad que permite al usuario hacer una busqueda relacionada con libros tras lo cual se visualizarán los resultados
en un recyclerview. Si el usuario selecciona alguno se abrirá una nueva actividad con la información de ese libro.
 */
public class MainActivity extends AppCompatActivity implements DialogoImagen.ListenerdelDialogo {

    private RequestQueue requestQueue;
    private ArrayList<Libro> bookInfoArrayList;
    private RecyclerView elreciclerview;
    private AdaptadorRecycler eladaptador;
    private EditText editTextLibro;
    private Uri uriimagen = null;
    private String imageName;
    private File fichImg = null;

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

        //Establecer la vista "activity_register.xml"
        setContentView(R.layout.activity_main);


        //Obtener referencias a los elementos del layout
        editTextLibro = (EditText) findViewById(R.id.editTextLibro);
        elreciclerview = (RecyclerView) findViewById(R.id.recyclerview);


        bookInfoArrayList = new ArrayList<>();
        //Establecer cómo se desea que se organicen los elementos dentro del RecyclerView
        eladaptador = new AdaptadorRecycler(bookInfoArrayList, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this, RecyclerView.VERTICAL, false);
        elreciclerview.setLayoutManager(linearLayoutManager);
        elreciclerview.setAdapter(eladaptador);


    }

    private void inicializarRecyclerView() {
        //Este método crea el adaptador con los datos a mostrar y se los asigna al RecyclerView
        bookInfoArrayList = new ArrayList<>();
        eladaptador = new AdaptadorRecycler(bookInfoArrayList, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this, RecyclerView.VERTICAL, false);
        elreciclerview.setLayoutManager(linearLayoutManager);
        elreciclerview.setAdapter(eladaptador);
    }

    public void onClickBuscar(View v){
        /*Método que se ejecuta cuando el usuario pulsa el bóton buscar.
        Este método recoge la sentencia de búsqueda introducida en el campo de texto,
        inicializa el adaptador del reyclerview y llama al método "obtenerDatos".*/

        //Obtener busqueda introducida por el usuario
        String busqueda = editTextLibro.getText().toString();

        inicializarRecyclerView();
        obtenerDatos(busqueda);
    }

    public void obtenerDatos(String query){
        /*Este método realiza una petición HTTP a la API de Google Books utilizando la búsqueda introducida por el usuario.
        Como respuesta de la petición obtendremos un  objeto JsonObjectRequest, del cual se conseguirá un Array con objetos Json que contienen
        la información de los libros encontrados con la búsqueda.

        Peticiones HTTP usando Volley :
        https://www.develou.com/android-volley-peticiones-http/

        Uso de la API de Google Books : "Working with volumes"
        https://developers.google.com/books/docs/v1/using */

        // Crear nueva cola de peticiones
        requestQueue = Volley.newRequestQueue(MainActivity.this);
        //Limpiar el cache
        requestQueue.getCache().clear();
        //Url con el que obtener los datos en formato JSON en el API de Google Books
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + query + "&maxResults=40&printType=books";

        //Nueva petición JSONObject
        JsonObjectRequest booksObjrequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    //Obtener un JSONArray con el atributo llamado "items"
                    JSONArray itemsArray = response.getJSONArray("items");
                    //Recorrer todos los objetos JSON
                    for (int i = 0; i < itemsArray.length(); i++) {
                        JSONObject itemsObj = itemsArray.getJSONObject(i);
                        //Obtener objeto JSON con los datos del libro
                        JSONObject volumeObj = itemsObj.getJSONObject("volumeInfo");
                        //Si la respuesta JSON no contiene ISBN y imagen, no vamos a recoger ese libro
                        if(volumeObj.has("industryIdentifiers") && volumeObj.has("previewLink")){
                            String ISBN = volumeObj.getJSONArray("industryIdentifiers").optJSONObject(0).optString("identifier");//ISBN
                            String title = volumeObj.optString("title"); //Titulo
                            String editorial =  volumeObj.optString("publisher");//Editorial
                            //String idioma = volumeObj.optString("language"); //Idioma del libro
                            String previewLink = volumeObj.optString("previewLink"); //PreviewLink
                            //Double rating = volumeObj.optDouble("averageRating"); //Nota media
                            //int numHojas = volumeObj.optInt("pageCount"); //Número de hojas
                            String descripcion = volumeObj.optString("description"); //Descripción

                            String thumbnail="";
                            if(volumeObj.has("imageLinks")){
                                JSONObject imageLinks = volumeObj.optJSONObject("imageLinks");
                                thumbnail = imageLinks.optString("thumbnail"); //Imagen del libro
                            }

                            JSONArray arrayAutores = volumeObj.optJSONArray("authors"); //Autores
                            ArrayList<String> autores=new ArrayList<String>();
                            if (arrayAutores != null) {
                                for (int j = 0; j < arrayAutores.length(); j++) {
                                    autores.add(arrayAutores.optString(j));
                                }
                            }
                            //En caso de que haya varios autores, se guardaran como autor1,autor2,autor3
                            String stringAutores = android.text.TextUtils.join(",", autores);

                            //Crear una instancia de Libro con los datos conseguidos y añadirlos al ArrayList de libros.
                            Libro libro = new Libro(ISBN,title,stringAutores,editorial,descripcion,thumbnail,previewLink);
                            bookInfoArrayList.add(libro);
                            }
                        }
                    //Notificar al adaptador de que se ha actualizado el ArrayList
                    eladaptador.notifyDataSetChanged();

                } catch (JSONException e) {
                    //e.printStackTrace();
                    String toast = getString(R.string.errorBusqueda);
                    Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String toast = getString(R.string.errorBusqueda);
                Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
        // Añadir petición a la cola
        requestQueue.add(booksObjrequest);

    }


    public void onClickEscanear(View v){

        //Si dispone de permisos, pide al usuario seleccionar la imagen del código de barras de la galeria o sacar
        //una foto nueva
        if(comprobarPermisos()) {
            DialogFragment dialogoImagen= new DialogoImagen();
            dialogoImagen.show(getSupportFragmentManager(), "etiqueta");
        }

    }

    @Override
    //Opción Obtener de la Galeria del diálogo
    public void alpulsarObtenerDeGaleria() {
        Intent elIntentGal = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(elIntentGal, 10);
    }

    @Override
    //Opción Sacar Foto del diálogo
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
        Log.i("etiqueta","start for result");
        startActivityForResult(elIntent, 11);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 10 && resultCode == RESULT_OK) { //Foto de galeria
            uriimagen = data.getData();
            escanear();

        }
        if (requestCode == 11 && resultCode == RESULT_OK) { //Foto tomada con teléfono
            Log.i("etiqueta","escanear");
            escanear();
        }
    }
    public void escanear(){
        //Código obtenido de ML Kit Scan Barcodes de Firebase
        //https://developers.google.com/ml-kit/vision/barcode-scanning/android#java

        //Configure the barcode scanner
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8)
                        .build();

        //Prepare the input image
        InputImage image=null;
        try {
            Log.i("etiqueta",uriimagen.toString());
            image = InputImage.fromFilePath(this, uriimagen);

            //Get an instance of BarcodeScanner
            BarcodeScanner scanner = BarcodeScanning.getClient(options);

            //Process the image
            Task<List<Barcode>> result = scanner.process(image)
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        // Task completed successfully
                        public void onSuccess(List<Barcode> barcodes) {
                            //Get information from barcodes
                            boolean libroEncontrado=false;
                            for (Barcode barcode : barcodes) {

                                int valueType = barcode.getValueType();
                                // See API reference for complete list of supported types
                                switch (valueType) {
                                    case Barcode.TYPE_ISBN:
                                        String isbn = barcode.getRawValue();
                                        String query="isbn:"+isbn;
                                        inicializarRecyclerView();
                                        obtenerDatos(isbn);
                                        Log.i("etiqueta","ISBN Libro:" + isbn);
                                        libroEncontrado=true;
                                        break;
                                }
                            }
                            if(!libroEncontrado){
                                Toast.makeText(MainActivity.this, R.string.errorEscaneo, Toast.LENGTH_SHORT).show();
                            }
                            boolean deleted = false;
                            try {
                                deleted = fichImg.delete();
                            } catch (SecurityException e) {
                            }
                            if (!deleted) {
                                fichImg.deleteOnExit();
                            }
                        }
                    })

                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        Toast.makeText(MainActivity.this, R.string.errorEscaneo, Toast.LENGTH_SHORT).show();
                    }
                });
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, R.string.errorEscaneo, Toast.LENGTH_SHORT).show();
        }
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
    public void onBackPressed(){
        /*Método que se ejecuta cuando el usuario pulsa el bóton del móvil para volver hacia atras.
          El método abrirá la actividad anterior a la actual, en este caso, MainActivityBiblioteca y finalizará la
          actividad actual.*/
        Context context = getApplicationContext();
        Intent newIntent = new Intent(context, MainActivityBiblioteca.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newIntent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("file_uri", uriimagen);
    }

    /*
     * Here we restore the fileUri again
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        uriimagen = savedInstanceState.getParcelable("file_uri");
    }
}