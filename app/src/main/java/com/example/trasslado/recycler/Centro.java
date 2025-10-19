package com.example.trasslado.recycler;

import com.google.android.gms.maps.model.LatLng;

public class Centro {

    private String provincia;
    private String centro;
    private String direccion;
    private LatLng coordenadas;

    public Centro(
            String provincia,
            String centro,
            String direccion,
            LatLng coordenadas) {
        this.provincia = provincia;
        this.centro = centro;
        this.direccion = direccion;
        this.coordenadas = coordenadas;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getCentro() {
        return centro;
    }

    public void setCentro(String centro) {
        this.centro = centro;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public LatLng getCoordenadas() {
        return coordenadas;
    }

    public void setCoordenadas(LatLng coordenadas) {
        this.coordenadas = coordenadas;
    }

    @Override
    public String toString() {
        return "Centro{" +
                "provincia='" + provincia + '\'' +
                ", centro='" + centro + '\'' +
                ", direccion='" + direccion + '\'' +
                ", coordenadas=" + coordenadas +
                '}';
    }
    public String mostarInformacion(){
        return direccion
                + "\n" + provincia
                + "\n" + coordenadas.latitude
                + "\n" + coordenadas.longitude;
    }
}
