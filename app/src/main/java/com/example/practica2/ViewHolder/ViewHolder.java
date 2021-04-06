package com.example.practica2.ViewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.practica2.R;

/*ViewHolder de la clase AdaptadorRecycler*/
public class ViewHolder extends RecyclerView.ViewHolder{
    public TextView eltexto;
    public ImageView laimagen;
    public TextView autores;
    public TextView editorial;

    public ViewHolder(@NonNull View itemView) {
        /*Constructor de la clase para hacer la asociación entre los
        campos de la clase y los elementos gráficos del layout*/
        super(itemView);
        eltexto = itemView.findViewById(R.id.libro_titulo);
        laimagen = itemView.findViewById(R.id.libro_imagen);
        autores = itemView.findViewById(R.id.libro_autor);
        editorial = itemView.findViewById(R.id.libro_editorial);
    }
}
