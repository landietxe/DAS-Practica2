package com.example.practica2.Actividades;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import com.example.practica2.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class GoogleMaps extends FragmentActivity implements OnMapReadyCallback {
    private LatLng ubicacionActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps);

        System.out.println("ON CREATE");
        SupportMapFragment elfragmento = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.fragmentoMapa);
        elfragmento.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        System.out.println("ON MAP READY");

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        System.out.println("");
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient proveedordelocalizacion = LocationServices.getFusedLocationProviderClient(this);
            proveedordelocalizacion.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        ubicacionActual = new LatLng(location.getLatitude(),location.getLongitude());

                        //Construimos un objeto de tipo CameraUpdate que será el que se le proporcione al mapa para modificar su aspecto.
                        CameraUpdate actualizar = CameraUpdateFactory.newLatLngZoom(ubicacionActual,13);
                        //Marcador
                        System.out.println("entra aqui");
                        System.out.println(ubicacionActual.toString());
                        googleMap.addMarker(new MarkerOptions()
                                .position(ubicacionActual)
                                .title("Tu ubicación"));
                        googleMap.animateCamera(actualizar);
                    } else {
                        System.out.println("Latitud y longitud desconocidad");
                    }
                }
            }).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                }
            });
        }

    }
}