package com.das.das_proyecto1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
//para places
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.LocationRestriction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.Arrays;
import java.util.List;

public class SupermercadosFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    //places necesita cliente
    private PlacesClient placesClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_supermercados, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        //inicializamos places
        String apiKey = "AIzaSyATHJJH868G88__vrdsQdHikl2CIzTaOFY";
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey);
        }
        placesClient = Places.createClient(requireContext());
        //pedimos permisos
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            }
            if (fineLocationGranted != null && fineLocationGranted) {
                activarUbicacionReal();
            } else {
                Toast.makeText(getContext(), getString(R.string.permisos_denegados), Toast.LENGTH_SHORT).show();
            }
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return root;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        lanzarPeticionPermisos();
    }

    private void lanzarPeticionPermisos() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            activarUbicacionReal();
        } else {
            locationPermissionRequest.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }
    private void activarUbicacionReal() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    LatLng miPos = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miPos, 15f));
                    //llamada para que nos busque los supermercados
                    buscarSupermercadosSDK(location.getLatitude(), location.getLongitude());
                }
            });
        }
    }
    private void buscarSupermercadosSDK(double lat, double lng) { //métod para buscar los supermercados con places
        //definimos el punto de partida (donde estamos)
        LatLng latLng = new LatLng(lat, lng);
        //fijamos el radio de búsqueda
        LocationRestriction restriction =
                CircularBounds.newInstance(latLng, 1000);
        //datos que queremos que la api nos devuelva
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.NAME,
                Place.Field.LAT_LNG
        );
        //petición de búsqueda
        SearchNearbyRequest request = SearchNearbyRequest.builder(restriction, placeFields)
                .setIncludedTypes(Arrays.asList("supermarket"))
                .build();
        //ejecutamos la búsqueda
        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {//petición exitosa
                    //limpiamos funtos anteriores
                    mMap.clear();
                    //añadimos los elementos devueltos de la lista al mapa
                    for (Place place : response.getPlaces()) {
                        if (place.getLatLng() != null) {
                            mMap.addMarker(new MarkerOptions()
                                    .position(place.getLatLng())
                                    .title(place.getName()));
                        }
                    }
                    Toast.makeText(getContext(),
                            getString(R.string.supermercados) + response.getPlaces().size(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {//petición fallida
                    Toast.makeText(getContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}