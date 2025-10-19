package com.example.trasslado;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trasslado.databinding.ActivityMapsBinding;
import com.example.trasslado.recycler.Centro;
import com.example.trasslado.recycler.CentroAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;

import java.util.ArrayList;
import java.util.List;

// TODO CLAVE SHA: 67:D3:F2:54:4B:EC:99:BA:C5:1E:D0:60:7F:27:FD:EF:28:30:20:98 BORRAR

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private ArrayList<Centro> listaCentros = rellenarCentros();
    CentroAdapter adaptador;
    private RecyclerView rvCentros;

    // Variables del mapa
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int PERMISSIONS_READ_FINE_LOCATION = 100;
    private float zoom = 16.0f;
    private float zoomOut = 8.0f;
    private boolean isZoomOut;
    Location ubicacionDispositivo;


    // Variables de sensores
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 20;
    private static final float AMPLITUDE_THRESHOLD = 2.0f;
    private SensorManager sensorManager;
    private Sensor acelerometro;
    private Sensor senTemperatura, senPresion, senHumedad;
    private float temperatura = 0.0f;
    private float presion = 0.0f;
    private float humedad = 0.0f;
    private TextView txtTemperatura;
    private TextView txtPresion;
    private TextView txtHumedad;
    private AlertDialog dialogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isZoomOut = false;

        // Solicitar permisos de ubicación
        requestPermissions();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtener el SupportMapFragment y ser notificados cuando el mapa esté listo
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Creamos el adaptador y configuramos nuestro RecyclerView
        adaptador = new CentroAdapter(this, listaCentros, this);
        if (adaptador.getItemCount() > 0) {
            rvCentros = findViewById(R.id.rvCentros);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            rvCentros.setLayoutManager(layoutManager);
            rvCentros.setAdapter(adaptador);
        }

        // Registro de los sensores
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (acelerometro != null)
            sensorManager.registerListener(
                    this,
                    acelerometro,
                    SensorManager.SENSOR_DELAY_NORMAL);

        // Sensor de temperatura ambiente
        senTemperatura = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (senTemperatura != null)
            sensorManager.registerListener(this, senTemperatura, SensorManager.SENSOR_DELAY_NORMAL);
        // Sensor de presión atmosférica
        senPresion = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (senPresion != null)
            sensorManager.registerListener(this, senPresion, SensorManager.SENSOR_DELAY_NORMAL);
        // Sensor de humedad relativa
        senHumedad = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        if (senHumedad != null)
            sensorManager.registerListener(this, senHumedad, SensorManager.SENSOR_DELAY_NORMAL);
        // Referencias a los campos del Dialog
        txtTemperatura = findViewById(R.id.txtTemp);
        txtPresion = findViewById(R.id.txtPresion);
        txtHumedad = findViewById(R.id.txtHumedad);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP
                && (event.getEventTime() - event.getDownTime()) >= 5000){
            if(isZoomOut)
                mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom));
            else mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomOut));
            isZoomOut = !isZoomOut;
        }
        return super.dispatchTouchEvent(event);
    }

    @SuppressLint("MissingPermission")
    protected void obtenerUbicacionActual() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(
                new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    // La ubicación actual se obtuvo con éxito
                    ActualizarUbicacion(location);
                    ubicacionDispositivo = location;
                    if(!mMap.isMyLocationEnabled()) {
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Log.e("MapsActivity", "La ubicación es nula");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Error al obtener la ubicación actual
                Log.e("MapsActivity", "Error obteniendo la última ubicación", e);
            }
        });
    }

    public void ActualizarUbicacion(Location location) {

        if (location != null) {
            LatLng latLng = new LatLng(
                    location.getLatitude(),
                    location.getLongitude());

            // Si la ubicación existe limpiamos el mapa agregamos un marcador
            mMap.clear();
            MarkerOptions markerOptionsActual = new MarkerOptions()
                    .position(latLng)
                    .title("Ubicación actual");
            mMap.addMarker(markerOptionsActual);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
            String msg = "Localización actualizada: " +
                    Double.toString(latLng.latitude) + "," +
                    Double.toString(latLng.longitude);
            System.out.println(msg);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Habilitar controles de zoom y brújula
        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);

        // Agregamos nuestro Info Window personalizado
        mMap.setInfoWindowAdapter(new InfoWindowPersonalizado(this));
        if (checkPermissions()) {
            obtenerUbicacionActual();
        }
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                // Llamar al método para manejar el clic en el botón de "Mi ubicación"
                obtenerUbicacionActual();
                return true;
            }
        });
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            requestPermissions();
            return false;
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_READ_FINE_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_READ_FINE_LOCATION: {
                // Si la solicitud se cancela los arrays resultantes están vacíos
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    obtenerUbicacionActual();
                    if (ubicacionDispositivo != null) {
                        ActualizarUbicacion(ubicacionDispositivo);
                    }
                }
                return;
            }
        }
    }

    // Método para agregar una nueva ubicación al mapa
    public void agregarSegundaUbicacion(
            LatLng nuevaUbicacion,
            String titulo,
            String info) {
        if (mMap != null && nuevaUbicacion != null) {
            // Limpiar todos los marcadores existentes
            mMap.clear();
            // Agregar un marcador para la nueva ubicación
            Marker ubicacionNueva = mMap.addMarker(
                    new MarkerOptions()
                    .position(nuevaUbicacion)
                    .title(titulo)
                    .snippet(info));
            ubicacionNueva.showInfoWindow();
            // Si la ubicación actual existe, agregar un marcador para ella
            if (ubicacionDispositivo != null) {
                LatLng ubicacionActualLatLng = new LatLng(
                        ubicacionDispositivo.getLatitude(), ubicacionDispositivo.getLongitude());
                MarkerOptions markerOptionsActual = new MarkerOptions()
                        .position(ubicacionActualLatLng)
                        .title("Ubicación actual");
                mMap.addMarker(markerOptionsActual);
                // Calcula el límite para que se muestren ambas ubicaciones en el mapa
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(ubicacionActualLatLng);
                builder.include(nuevaUbicacion);
                LatLngBounds bounds = builder.build();
                // Calcula el ancho y alto de la ventana del mapa
                int padding = 100;
                // Mueve la cámara del mapa para que se vean ambas ubicaciones con el zoom adecuado
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                isZoomOut = true;

                obtenerRuta(ubicacionActualLatLng, nuevaUbicacion);
            } else {
                // Si la ubicación actual no está disponible solo mueve la cámara hacia la nueva ubicación
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nuevaUbicacion, zoom));
                isZoomOut = false;
            }
        }
    }

    private void obtenerRuta(LatLng origen, LatLng destino) {
        String apiKey = null;
        try {
            GeoApiContext geoApiContext = null;
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(
                    getPackageName(),
                    PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                apiKey = getString(R.string.google_maps_key);
                geoApiContext = new GeoApiContext.Builder().apiKey(apiKey).build();
            }
            // Crear una solicitud de direcciones
            String origenString = origen.latitude + "," + origen.longitude;
            String destinoString = destino.latitude + "," + destino.longitude;
            DirectionsApiRequest directionsRequest = DirectionsApi.newRequest(geoApiContext)
                    .origin(origenString)
                    .destination(destinoString);
            // Manejar el resultado de la solicitud
            directionsRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                @Override
                public void onResult(DirectionsResult result) {
                    // Procesar el resultado y dibujar la ruta en el mapa
                    List<LatLng> path = new ArrayList<>();
                    for (DirectionsRoute route : result.routes) {
                        for (DirectionsLeg leg : route.legs) {
                            for (DirectionsStep step : leg.steps) {
                                path.addAll(PolyUtil.decode(step.polyline.getEncodedPath()));
                            }
                        }
                    }
                    // Dibujar la polilínea en el mapa
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMap.addPolyline(new PolylineOptions().addAll(path));
                        }
                    });
                }
                @Override
                public void onFailure(Throwable e) {
                    // Manejar el error si falla la obtención de direcciones
                    Log.e("MapsActivity", "Error al obtener direcciones", e);
                }
            });
        } catch (Exception ex) {
            Log.e("MapsActivity", "Error en la obtención de la ruta", ex);
        }
    }

    public ArrayList<Centro> rellenarCentros(){
        ArrayList<Centro> resultado = new ArrayList<>();
        resultado.add(new Centro(
                "Almería",
                "IES Aguadulce",
                "Alhambra, 11. 04720 - Roquetas de Mar (Aguadulce)",
                new LatLng(36.80972483284305, -2.5829272778073573)
        ));
        resultado.add(new Centro(
                "Cádiz",
                "IES Nuestra Señora de los Remedios",
                "Av. Herrera Oria, s/n. 11600 - Ubrique (Cádiz)",
                new LatLng(36.67994559482807, -5.444871603339807)
        ));
        resultado.add(new Centro(
                "Córdoba",
                "IES Trassierra",
                "Av. Arroyo del Moro s/n 14011 - Córdoba",
                new LatLng(37.892384279984505, -4.797732688771477)
        ));
        resultado.add(new Centro(
                "Granada",
                "IES Zaidín-Vergeles",
                "Primavera, 26-28. 18001 - Granada",
                new LatLng(37.16183621088468, -3.5912057609932577)
        ));
        resultado.add(new Centro(
                "Huelva",
                "IES Fuentepiña",
                "Camino del Saladillo s/n. Barriada Vista Alegre. - 21007 Huelva",
                new LatLng(37.26765396495996, -6.931701474481754)
        ));
        resultado.add(new Centro(
                "Jaén",
                "IES Virgen del Carmen",
                "Paseo de la Estación, 44, 23008 Jaén",
                new LatLng(37.77716620789462, -3.7886643585190654)
        ));
        resultado.add(new Centro(
                "Málaga",
                "IES Portada Alta",
                "Portada Alta, s/n. 29007- Málaga",
                new LatLng(36.719210771780666, -4.4539803953979575)
        ));
        resultado.add(new Centro(
                "Sevilla",
                "IES Cristóbal del Monroy",
                "Av. de la Constitución, s/n. 41500 - Alcalá de Guadaíra (Sevilla)",
                new LatLng(37.34914266437437, -5.843548106155746)
        ));
        return resultado;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            // Controlamos el tiempo entre eventos de agitar para ignorar
            // la detección si ya se está agitando
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 1000) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                // Calculamos la velocidad de agitación y la comparamos con el umbral
                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
                float amplitud = (float) Math.sqrt(x * last_x + y * last_y + z * last_z);
                if (speed > SHAKE_THRESHOLD && amplitud > AMPLITUDE_THRESHOLD) {
                    System.out.println("AGITACION!!!");
                    ventanaInformacion();
                }
            }
            // Actualizamos los valores anteriores
            last_x = x;
            last_y = y;
            last_z = z;
        // Capturamos los valores de los sensores cuando cambian
        }else if (mySensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
            temperatura = event.values[0];
        }else if (mySensor.getType() == Sensor.TYPE_PRESSURE){
            presion = event.values[0];
        }else if (mySensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY){
            humedad = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volver a registrar el listener del sensor al reanudar la actividad.
        if (acelerometro != null)
            sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_NORMAL);
        if (senTemperatura != null)
            sensorManager.registerListener(this, senTemperatura, SensorManager.SENSOR_DELAY_NORMAL);
        if (senPresion != null)
            sensorManager.registerListener(this, senPresion, SensorManager.SENSOR_DELAY_NORMAL);
        if (senHumedad != null)
            sensorManager.registerListener(this, senHumedad, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Dar de baja el listener del sensor al pausar la actividad
        sensorManager.unregisterListener(this);
    }

    public void ventanaInformacion(){
        if (dialogo != null && dialogo.isShowing()) {
            dialogo.dismiss();
        }
        View vistaDialogo = LayoutInflater.from(this).inflate(
                R.layout.sensores, null);
        txtTemperatura = vistaDialogo.findViewById(R.id.txtTemp);
        txtPresion = vistaDialogo.findViewById(R.id.txtPresion);
        txtHumedad = vistaDialogo.findViewById(R.id.txtHumedad);
        txtTemperatura.setText(String.format("%.1f ºC", temperatura));
        txtPresion.setText(String.format("%.1f hPa", presion));
        txtHumedad.setText(String.format("%.1f %%", humedad));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(vistaDialogo);
        dialogo = builder.create();
        dialogo.show();
    }
}