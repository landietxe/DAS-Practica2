package com.example.practica2;


import android.os.Parcel;
import android.os.Parcelable;

//Clase auxiliar para guardar la información de los libros.
public class Libro implements Parcelable {
    private String ISBN;
    private String titulo;
    private String imagen;
    private String editorial;
    private String autores;
    private String descripcion;
    private String idioma;
    private String previewLink;


    public Libro(String ISBN, String title,String autores,String editorial,String descripcion, String imagen, String preview){
        //Constructor de la clase Libro
        this.ISBN=ISBN;
        this.titulo=title;
        this.editorial=editorial;
        this.descripcion=descripcion;
        this.imagen=imagen;
        this.previewLink=preview;
        this.autores=autores;
    }

    protected Libro(Parcel in) {
        ISBN = in.readString();
        titulo = in.readString();
        imagen = in.readString();
        editorial = in.readString();
        autores = in.readString();
        descripcion = in.readString();
        idioma = in.readString();
        previewLink = in.readString();
    }

    public static final Creator<Libro> CREATOR = new Creator<Libro>() {
        @Override
        public Libro createFromParcel(Parcel in) {
            return new Libro(in);
        }

        @Override
        public Libro[] newArray(int size) {
            return new Libro[size];
        }
    };

    // Métodos get para obtener los atributos de la clase
    public String getTitle() {
        return titulo;
    }
    public String getAutores(){
        return autores;
    }
    public String getISBN() { return ISBN; }
    public String getThumbnail() {
        return imagen;
    }
    public String getEditorial(){
        return this.editorial;
    }
    public String getDescripcion() { return descripcion; }
    public String getIdioma() { return idioma; }
    public String getPreviewLink() { return previewLink; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(titulo);
        out.writeString(autores);
        out.writeString(ISBN);
        out.writeString(imagen);
        out.writeString(editorial);
        out.writeString(descripcion);
        out.writeString(idioma);
        out.writeString(previewLink);

    }
}
