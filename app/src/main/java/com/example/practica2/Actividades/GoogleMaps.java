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
    private LatLng ubicacionActual;
    private RequestQueue requestQueue;
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

        setContentView(R.layout.activity_google_maps);

        SupportMapFragment elfragmento = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
        elfragmento.getMapAsync(this);


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient proveedordelocalizacion = LocationServices.getFusedLocationProviderClient(this);
            proveedordelocalizacion.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        ultimaUbicacion = new LatLng(location.getLatitude(), location.getLongitude());

                        //Construimos un objeto de tipo CameraUpdate que será el que se le proporcione al mapa para modificar su aspecto.
                        CameraUpdate actualizar = CameraUpdateFactory.newLatLngZoom(ultimaUbicacion, 15);
                        //Marcador
                        Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker);
                        markerBitmap = Bitmap.createScaledBitmap(markerBitmap, 90, 90, false);
                        userMarker = googleMap.addMarker(new MarkerOptions()
                                .position(ultimaUbicacion)
                                .title("Tu ubicación")
                                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap)));
                        googleMap.animateCamera(actualizar);
                    } else {
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

        //PARA DETECTAR CAMBIOS DE POSICIÓN
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
                    ultimaUbicacion = new LatLng(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude());
                    actualizarMarcador(googleMap);

                }
            }
        };


        proveedordelocalizacion.requestLocationUpdates(peticion, actualizador, null);


        Button botonBuscar = (Button) findViewById(R.id.boton);
        botonBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ultimaUbicacion!=null) {
                    buscarLibrerias(googleMap, ultimaUbicacion);
                }
            }
        });
    }

    public void buscarLibrerias(GoogleMap googleMap,LatLng ubicacion) {

        //Calcuar el boudning box desde la ubicación actual
        //https://stackoverflow.com/questions/1689096/calculating-bounding-box-a-certain-distance-away-from-a-lat-long-coordinate-in-j
        double radioTierra = 6371;  // earth radius in km
        double radius =3; // km

        double lon=ubicacion.longitude;
        double lat = ubicacion.latitude;


        double x1 = lon - Math.toDegrees(radius/radioTierra/Math.cos(Math.toRadians(lat))); //Este
        double x2 = lon + Math.toDegrees(radius/radioTierra/Math.cos(Math.toRadians(lat)));  //Oeste
        double y1 = lat + Math.toDegrees(radius/radioTierra); //Norte
        double y2 = lat - Math.toDegrees(radius/radioTierra); //Sur

        //(south,west,north,east)
        String bbox = "(" + String.valueOf(y2) + "," + String.valueOf(x1) + "," + String.valueOf(y1) + "," + String.valueOf(x2)+")";

        Data datos = new Data.Builder()
                .putString("bbox",bbox)
                .build();

        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(BuscarLibrerias.class).setInputData(datos).build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String result = workInfo.getOutputData().getString("resultados");
                            if ("SUCCEEDED".equals(workInfo.getState().name())) {
                                try {
                                    JSONObject json = new JSONObject(result);
                                    JSONArray jsonArray = (JSONArray) json.get("elements");
                                    if(jsonArray.length()>0) {
                                        borrarMarcadores();
                                        for (int i = 0; i < jsonArray.length(); i++) {
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
                                            //System.out.println("TIENDA " + i + " " + nombreTienda + "(" + String.valueOf(lat) + ", " + String.valueOf(lon) + ")");
                                            Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bookstore);
                                            markerBitmap = Bitmap.createScaledBitmap(markerBitmap, 70, 70, false);
                                            MarkerOptions marker = new MarkerOptions()
                                                    .position(new LatLng(lat, lon))
                                                    .title(nombreTienda)
                                                    .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap));
                                            Marker m =googleMap.addMarker(marker);
                                            marcadoresLibrerias.add(m);

                                            String mensaje = jsonArray.length() + " " + getString(R.string.libreriasEncontradas);
                                            Toast toast = Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT);
                                            toast.setGravity( Gravity.CENTER_VERTICAL, 0, 0);
                                            toast.show();
                                        }
                                    }
                                    else{//No se ha encontrado ninguna libreria cercana, indicarlo mediante un Toast
                                        String mensaje = getString(R.string.noLibrerias);
                                        Toast toast = Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT);
                                        toast.setGravity( Gravity.CENTER_VERTICAL, 0, 0);
                                        toast.show();

                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            else{ //No se ha podido completar la búsqueda
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
        //Marcador
        if(userMarker!=null) {
            userMarker.remove();
        }
        Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker);
        markerBitmap = Bitmap.createScaledBitmap(markerBitmap, 75, 75, false);
        userMarker = googleMap.addMarker(new MarkerOptions()
                .position(ultimaUbicacion)
                .title("Tu ubicación")
                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap)));
    }
    
    public void borrarMarcadores(){
        /*Método que se ejecuta antes de añadir los marcadores de las librerias cercanas.
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
        /*Método que se ejecutará cuando el usuario pulse el botón del movil para volver atras.
        Abrirá la Actividad "MainActivityBiblioteca" y cerrará la actividad actual.*/
        Context context = getApplicationContext();
        Intent newIntent = new Intent(context, MainActivityBiblioteca.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newIntent);
        finish();
    }
}