package com.example.trasslado;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class InfoWindowPersonalizado implements GoogleMap.InfoWindowAdapter {

    private final View vista;
    private Context mContext;

    public InfoWindowPersonalizado(Context context) {
        mContext = context;
        vista = LayoutInflater.from(context).inflate(
                R.layout.custom_info_window, null);
    }
    private void renderWindowText(Marker marker, View view) {
        String title = marker.getTitle();
        TextView tvTitle = view.findViewById(R.id.titulo);
        if (!title.equals("")) {
            tvTitle.setText(title);
        }
        String snippet = marker.getSnippet();
        TextView tvSnippet = view.findViewById(R.id.snippet);
        if (!snippet.equals("")) {
            tvSnippet.setText(snippet);
        }
    }
    @Override
    public View getInfoWindow(Marker marker) {
        renderWindowText(marker, vista);
        return vista;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}