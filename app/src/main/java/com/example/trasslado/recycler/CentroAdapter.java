package com.example.trasslado.recycler;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trasslado.MapsActivity;
import com.example.trasslado.R;

import java.util.ArrayList;

public class CentroAdapter extends RecyclerView.Adapter<CentroViewHolder> {

    private Context contexto;
    private ArrayList<Centro> centros;
    private MapsActivity activity;

    public CentroAdapter(Context contexto, ArrayList<Centro> centros, MapsActivity activity){
        this.contexto = contexto;
        this.centros = centros;
        this.activity = activity;
    }

    @NonNull
    @Override
    public CentroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(contexto).inflate(
                R.layout.recycler_elemento,
                parent,
                false);
        return new CentroViewHolder(item);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CentroViewHolder holder,
            int position) {
        holder.bindCentro(centros.get(position));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            int pos = holder.getAdapterPosition();
            Centro centro = centros.get(pos);
            @Override
            public void onClick(View v) {
                activity.agregarSegundaUbicacion(
                        centro.getCoordenadas(),
                        centro.getCentro(),
                        centro.mostarInformacion());
            }
        });
    }


    @Override
    public int getItemCount() {
        return centros.size();
    }
/*
    private void mostrarDialog(String contenido) {
        // creamos un nuevo diálogo
        Dialog dialog = new Dialog(contexto);
        dialog.setContentView(R.layout.descripcion);
        // vinculamos con el TextView de nuestra vista
        TextView descripcion = dialog.findViewById(R.id.txtDescripcion);
        // añadimos a nuestro TextView el contenido que vamos a mostrar
        descripcion.setText(contenido);
        dialog.show();
    }

 */
}


