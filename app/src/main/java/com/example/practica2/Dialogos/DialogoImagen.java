package com.example.practica2.Dialogos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.practica2.R;

public class DialogoImagen extends DialogFragment {
    DialogoImagen.ListenerdelDialogo miListener;
    private String titulo;
    private String texto;
    /*Interfaz del diálogo para que las acciones se ejecuten
    en la actividad que llamó al dialogo*/
    public interface ListenerdelDialogo {
        void alpulsarSacarFoto();
        void alpulsarObtenerDeGaleria();
    }
    public DialogoImagen(String titulo,String texto){
        this.titulo=titulo;
        this.texto=texto;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        miListener =(DialogoImagen.ListenerdelDialogo) getActivity();

        //Obtener string según el idioma para el texto del dialog
        String galeria = getString(R.string.escanearGaleria);
        String foto = getString(R.string.escanearFoto);
        //Crear  un AlertDialog con el estilo "AlertDialogCustom"
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),R.style.AlertDialogCustom);
        builder.setTitle(titulo);
        builder.setMessage(texto);

        //Establecer boton para confirmar la acción del usuario.
        builder.setPositiveButton(foto, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Método a ejecutar cuando el usuario pulsa el botón afirmando la acción.
                miListener.alpulsarSacarFoto();
            }

        });
        //Establecer boton para negar la acción del usuario.
        builder.setNegativeButton(galeria, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                miListener.alpulsarObtenerDeGaleria();
            }

        });
        return builder.create();
    }
}
