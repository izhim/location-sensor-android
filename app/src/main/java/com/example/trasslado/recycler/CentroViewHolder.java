package com.example.trasslado.recycler;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trasslado.R;

public class CentroViewHolder extends RecyclerView.ViewHolder {
    private TextView rvNombre, rvProvincia;

    public CentroViewHolder(@NonNull View itemView){
        super(itemView);
        rvNombre = itemView.findViewById(R.id.rvNombre);
        rvProvincia = itemView.findViewById(R.id.rvProvincia);
    }

    public void bindCentro(Centro centro){
        rvNombre.setText(centro.getCentro());
        rvProvincia.setText(centro.getProvincia());
    }
}
