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

// ✅ IMPORTS NUEVOS (Places SDK)
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

    // ✅ Cliente de Places
    private PlacesClient placesClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_supermercados, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // ✅ Inicializar Places (UNA SOLA VEZ)
        String apiKey = "AIzaSyATHJJH868G88__vrdsQdHikl2CIzTaOFY";
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey);
        }
        placesClient = Places.createClient(requireContext());

        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            }
            if (fineLocationGranted != null && fineLocationGranted) {
                activarUbicacionReal();
            } else {
                Toast.makeText(getContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
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

                    // ✅ NUEVA LLAMADA (SDK)
                    buscarSupermercadosSDK(location.getLatitude(), location.getLongitude());
                }
            });
        }
    }

    // ✅ MÉTODO NUEVO (sustituye al HTTP)

    private void buscarSupermercadosSDK(double lat, double lng) {

        LatLng latLng = new LatLng(lat, lng);

        LocationRestriction restriction =
                CircularBounds.newInstance(latLng, 1000);

        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.NAME,
                Place.Field.LAT_LNG
        );

        SearchNearbyRequest request = SearchNearbyRequest.builder(restriction, placeFields)
                .setIncludedTypes(Arrays.asList("supermarket"))
                .build();

        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {

                    mMap.clear();

                    for (Place place : response.getPlaces()) {
                        if (place.getLatLng() != null) {
                            mMap.addMarker(new MarkerOptions()
                                    .position(place.getLatLng())
                                    .title(place.getName()));
                        }
                    }

                    Toast.makeText(getContext(),
                            "Supermercados: " + response.getPlaces().size(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}