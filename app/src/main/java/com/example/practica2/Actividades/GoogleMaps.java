package com.example.practica2.Actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.practica2.Libro;
import com.example.practica2.R;
import com.example.practica2.WorkManager.BuscarLibrerias;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GoogleMaps extends FragmentActivity implements OnMapReadyCallback {
    /*Actividad que muestra un mapa de google maps con la ubicación del usuario actual marcada en él y que permite
    buscar librerías cercanas, las cuales también se marcarán en el mapa.*/

    private LatLng ubicacionActual;
    private Marker userMarker;
    private LatLng ultimaUbicacion;
    private ArrayList<Marker> marcadoresLibrerias = new ArrayList<Marker>();


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

        //Establecer la vista "activity_google_maps"
        setContentView(R.layout.activity_google_maps);

        //Para poder trabajar con el mapa se utiliza el identificador asignado al fragment donde se encuentra el mapa y llama al método getMapAsync
        SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
        //El método getMapAsync llama al método onMapReady, que recibe como parámetro el objeto GoogleMap con el que trabajar.
        elfragmento.getMapAsync(this);


    }

    @Override
    //El mapa ya está listo y se puede utilizar.
    public void onMapReady(GoogleMap googleMap) {

        //Definir el tipo de visualización que queremos tener del mapa
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Obtener la última ubicación conocida del dispositivo para marcarlo en el mapa
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient proveedordelocalizacion = LocationServices.getFusedLocationProviderClient(this);
            proveedordelocalizacion.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        ultimaUbicacion = new LatLng(location.getLatitude(), location.getLongitude());

                        //Construimos un objeto de tipo CameraUpdate que será el que se le proporcione al mapa para modificar su aspecto.
                        CameraUpdate actualizar = CameraUpdateFactory.newLatLngZoom(ultimaUbicacion, 15);
                        //Se asigna un icono al marcador
                        Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker);
                        markerBitmap = Bitmap.createScaledBitmap(markerBitmap, 90, 90, false);
                        //Establece en el marcador la longitud,latitud, un título y el icono.
                        userMarker = googleMap.addMarker(new MarkerOptions()
                                .position(ultimaUbicacion)
                                .title("Tu ubicación")
                                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap)));
                        //Mueve la cámara al nuevo marcador
                        googleMap.animateCamera(actualizar);
                    } else {
                        //Si no se ha encontrado la ubicación actual se muestra un Toast.
                        String mensaje = getString(R.string.ubicacionDesconocida);
                        Toast toast = Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT);
                        toast.setGravity( Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    }
                }

            }).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                }
            });

        }

        ////Una vez intentado conseguir la primera ubicación del dispositivo, cada 10 segundos se irán detectando los cambios de posición.
        FusedLocationProviderClient proveedordelocalizacion = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest peticion = LocationRequest.create();
        peticion.setInterval(10000); //Cada cuántos milisegundos debe actualizarse
        peticion.setFastestInterval(10000); //Cada cuántos milisegundos somos capaces de gestionar una actualización
        peticion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //Precisión

        //Indicar qué hacer cuando se actualiza la posición
        LocationCallback actualizador = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    //Si se ha encontrado la nueva localización, se llama al método actualizarMarcador para cambiar de posición el marcador
                    // con la ubicación del usuario.
                    ultimaUbicacion = new LatLng(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude());
                    actualizarMarcador(googleMap);
                }
            }
        };

        //Iniciar la captura de los cambios de posición
        proveedordelocalizacion.requestLocationUpdates(peticion, actualizador, null);

        //Obtener la referencia al botón de la actividad y asignarle un listener para cuando el usuario haga click en él.
        Button botonBuscar = (Button) findViewById(R.id.boton);
        botonBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ultimaUbicacion!=null) {
                    //Si la última ubicación conocida no es null, llama al método "buscarLibrerias".
                    buscarLibrerias(googleMap, ultimaUbicacion);
                }
            }
        });
    }

    public void buscarLibrerias(GoogleMap googleMap,LatLng ubicacion) {

        /*Método que se ejecuta cuando el usuario pulsa en el botón BUSCAR de la actividad.
        Por un lado se calcula el bounding box o el área alrededor del usuario en un radio de 3 kilómetros.
        Una vez obtenido el bounding box, mediante la la tarea de la clase "BuscarLibrerias" se buscan las librerias cercanas y se obtiene el resultado.

        Si se ha recibido un resultado de forma correcta, se recorre el json de la respuesta para obtener las ubicaciones y los nombres de las tiendas
        para poder ponerlos como marcadores en el mapa. Una vez puestos los marcadores, se indica mediante un Toast el número de tiendas
        que se han encontrado (o en caso de no encontrarse ninguna se indica mediante otro Toast).
         */

        //Calcular el bounding box desde la ubicación actual
        //https://stackoverflow.com/questions/1689096/calculating-bounding-box-a-certain-distance-away-from-a-lat-long-coordinate-in-j
        double rTierra = 6371;  // earth radius in km
        double radius =3; // km

        //Obtener la longitud y latitud actuales
        double lon=ubicacion.longitude;
        double lat = ubicacion.latitude;

        //Calcular los puntos del oeste,este,norte y sur desde la ubicación actual y a 3 kilómetros de distancia.
        double oeste = lon - Math.toDegrees(radius/rTierra/Math.cos(Math.toRadians(lat))); //oeste
        double este = lon + Math.toDegrees(radius/rTierra/Math.cos(Math.toRadians(lat)));  //este
        double norte = lat + Math.toDegrees(radius/rTierra); //Norte
        double sur = lat - Math.toDegrees(radius/rTierra); //Sur

        //(south,west,north,east)
        String bbox = "(" + String.valueOf(sur) + "," + String.valueOf(oeste) + "," + String.valueOf(norte) + "," + String.valueOf(este)+")";


        //Se crea un objeto Data para enviar a la tarea el bounding box.
        Data datos = new Data.Builder()
                .putString("bbox",bbox)
                .build();

        //Ejecuta la tarea de la clase "BuscarLibrerias" para buscar las librerías
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(BuscarLibrerias.class).setInputData(datos).build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            //Obtiene los resultados de la tarea
                            String result = workInfo.getOutputData().getString("resultados");
                            if ("SUCCEEDED".equals(workInfo.getState().name())) {
                                //Si se ha completado bien la tarea, obtenemos un objeto Json de la respuesta
                                try {
                                    JSONObject json = new JSONObject(result);
                                    //Obtenemos un JSONArray que contiene los datos de las tiendas
                                    JSONArray jsonArray = (JSONArray) json.get("elements");
                                    if(jsonArray.length()>0) {
                                        //Si se han encontrado tiendas, se borran los marcadores antiguos y se recorre el array
                                        borrarMarcadores();
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            //Por cada tienda se obtiene su longitud y latitud y en caso de que lo tenga, su nombre.
                                            JSONObject tienda = jsonArray.getJSONObject(i);
                                            double lon = tienda.getDouble("lon");
                                            double lat = tienda.getDouble("lat");
                                            String nombreTienda = "";
                                            if (tienda.has("tags")) {
                                                JSONObject tags = tienda.getJSONObject("tags");
                                                if (tags.has("name")) {
                                                    nombreTienda = (String) tags.getString("name");
                                                }
                                            }

                                            //Se crea un nuevo marcador con los datos de la tienda y se añade al mapa
                                            Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bookstore);
                                            markerBitmap = Bitmap.createScaledBitmap(markerBitmap, 70, 70, false);
                                            MarkerOptions marker = new MarkerOptions()
                                                    .position(new LatLng(lat, lon))
                                                    .title(nombreTienda)
                                                    .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap));
                                            Marker m =googleMap.addMarker(marker);
                                            marcadoresLibrerias.add(m);

                                        }
                                        //Se muestra un Toast indicando cuantas tiendas se han encontrado.
                                        String mensaje = jsonArray.length() + " " + getString(R.string.libreriasEncontradas);
                                        Toast toast = Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT);
                                        toast.setGravity( Gravity.CENTER_VERTICAL, 0, 0);
                                        toast.show();
                                    }
                                    else{
                                        //Si no se ha encontrado ninguna tienda se indica mediante un Toast.
                                        String mensaje = getString(R.string.noLibrerias);
                                        Toast toast = Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT);
                                        toast.setGravity( Gravity.CENTER_VERTICAL, 0, 0);
                                        toast.show();

                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            else{
                                //Si no se ha podido completar la búsqueda se indica mediante un Toast.
                                String mensaje = getString(R.string.errorBusqueda);
                                Toast toast = Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT);
                                toast.setGravity( Gravity.CENTER_VERTICAL, 0, 0);
                                toast.show();
                                
                            }
                        }
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);
    }

    public void actualizarMarcador(GoogleMap googleMap){
        /*En este método se actualiza el marcador de la posición del usuario. En primer lugar se comprueba si
        ya existe un marcador, y en ese caso se borra. Después, se crea un marcador con la nueva ubicación y se
        añade al mapa.*/

        //Si ya existe el marcador, se borra del mapa para actualizarlo
        if(userMarker!=null) {
            userMarker.remove();
        }

        //Se crea el nuevo marcador con la nueva ubicación.
        Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker);
        markerBitmap = Bitmap.createScaledBitmap(markerBitmap, 75, 75, false);
        userMarker = googleMap.addMarker(new MarkerOptions()
                .position(ultimaUbicacion)
                .title("Tu ubicación")
                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap)));
    }
    
    public void borrarMarcadores(){
        /*Método que se ejecuta antes de añadir los marcadores de las librerías cercanas.
        En caso de que no exista ningún marcador añadido, el método no hace nada. En caso contrario, recorre
        el array de los marcadores y los elimina.
         */
        if(marcadoresLibrerias.size()>0){
            for(int i = 0; i<marcadoresLibrerias.size();i++){
                Marker m = marcadoresLibrerias.get(i);
                //Borra el marcador del mapa
                m.remove();
            }
            marcadoresLibrerias=new ArrayList<Marker>();
        }
    }
    @Override
    public void onBackPressed(){
        /*Método que se ejecutará cuando el usuario pulsa el botón del movil para volver atras.
        Abrirá la Actividad "MainActivityBiblioteca" y cerrará la actividad actual.*/
        Context context = getApplicationContext();
        Intent newIntent = new Intent(context, MainActivityBiblioteca.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newIntent);
        finish();
    }
}