package com.example.practica2.Fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.practica2.R;
import com.example.practica2.BD.miBD;
import com.example.practica2.WorkManager.CompartirLibroFMC;
import com.example.practica2.WorkManager.ObtenerTokens;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

//Fragment que muestra la información de un libro seleccionado cuando el móvil se encuentra en orientación horizontal.
public class fragmentInfoLibroBibliotecaLand extends Fragment{
    private miBD gestorDB;
    private String isbn;
    private String previewLink;
    private listener2 elListener;


    private String tituloLibro;
    private String autorLibro;
    private String descripcionLibro;

    //Interfaz con el método del listener para la comunicación con el fragment definido en la actividad "MainActivityBiblioteca"
    public interface listener2{
        void seleccionarElemento();
    }


    public void onAttach(Context context) {
        //Método para unir el listener con los métodos implementado en la actividad
        super.onAttach(context);
        try{
            elListener=(fragmentInfoLibroBibliotecaLand.listener2) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException("La clase " +context.toString()
                    + "debe implementar listenerDelFragment");
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Enlazar la clase java del fragment con el fichero "fragment_biblioteca.xml"
        View v= inflater.inflate(R.layout.fragment_info_biblioteca_land,container,false);
        //Obtener referencia a la base de datos de la aplicación
        gestorDB = new miBD(getActivity(), "Libreria", null, 1);
        return v;
    }
    public void actualizar(String isbn,String title,String autores,String StringEditorial, String StringDescripcion,String previewLink,String urlimagen){
        /*Método llamado desde la clase MainActivityBiblioteca cuando la orientación del móvil es horizontal
        y el usuario selecciona uno de los libros. Este método obtiene las referencias a los elementos del layout, los hace visibles
        y los actualiza con los datos recibidos como parámetros.*/


        this.tituloLibro=title;
        this.autorLibro=autores;

        //Obtener referencias a los elementos del layout
        this.isbn=isbn;
        TextView titulo = (TextView) getActivity().findViewById(R.id.info_libro_titulo);
        TextView autor = (TextView) getActivity().findViewById(R.id.info_libro_autor);
        TextView editorial = (TextView) getActivity().findViewById(R.id.info_libro_editorial);
        TextView descripcion = (TextView) getActivity().findViewById(R.id.info_libro_descripcion);
        Button botonPreview = (Button) getActivity().findViewById(R.id.botonPreview);
        Button boton = (Button)getActivity().findViewById(R.id.boton);
        Button botonCompartir = (Button) getActivity().findViewById(R.id.botonCompartir);

        TextView tvTitulo = (TextView) getActivity().findViewById(R.id.tvTitulo);
        TextView tvAutores = (TextView) getActivity().findViewById(R.id.tvAutor);
        TextView tvEditorial = (TextView) getActivity().findViewById(R.id.tvEditorial);
        TextView tvDescripcion = (TextView) getActivity().findViewById(R.id.tvDescripcion);

        //Listener del bóton Borrar. Ejecutará el método seleccionarElemento del listener
        boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                elListener.seleccionarElemento();
            }
        });

        //Listener del bóton Preview. Se abre un intent implícito que muestra en el navegador una previsualización del libro.
        botonPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(previewLink));
                startActivity(i);
            }
        });

        botonCompartir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user="";
                try {//Obtener nombre del Usuario
                    BufferedReader ficherointerno = new BufferedReader(new InputStreamReader(getContext().openFileInput("usuario_actual.txt")));
                    ficherointerno.readLine();
                    String linea = ficherointerno.readLine();
                    user= linea.split(":")[1];
                    ficherointerno.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ObtenerTokens.class).build();

                String finalUser = user;
                WorkManager.getInstance(getContext()).getWorkInfoByIdLiveData(otwr.getId())
                        .observe(getViewLifecycleOwner(), new Observer<WorkInfo>() {
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
                                        enviarNotificacion(lista.toArray(new String[0]),title,autores,urlimagen, finalUser,StringDescripcion);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                WorkManager.getInstance(getContext()).enqueue(otwr);
            }
            });

        //Hacer visibles los elementos del layout
        tvTitulo.setVisibility(View.VISIBLE);
        tvAutores.setVisibility(View.VISIBLE);
        tvEditorial.setVisibility(View.VISIBLE);
        boton.setVisibility(View.VISIBLE);
        botonPreview.setVisibility(View.VISIBLE);
        tvDescripcion.setVisibility(View.VISIBLE);
        botonCompartir.setVisibility(View.VISIBLE);

        //Establecer la información del libro
        titulo.setText(title);
        autor.setText(autores);
        editorial.setText(StringEditorial);
        descripcion.setText(StringDescripcion);
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
        WorkManager.getInstance(getContext()).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if(workInfo != null && workInfo.getState().isFinished()){
                            Toast.makeText(getContext(),"Libro compartido",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        WorkManager.getInstance(getContext()).enqueue(otwr);

    }

}
