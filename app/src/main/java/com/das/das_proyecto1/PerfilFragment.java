package com.das.das_proyecto1;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.util.Log;
public class PerfilFragment extends Fragment {

    // Variables globales de la clase
    private ImageView imgPerfil;
    private EditText etNombre, etPassword;
    private Uri uriImagen;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private Bitmap bitmapFotoActual; // Este contendrá la imagen ya escalada [cite: 15]
    private ActivityResultLauncher<String> pedirPermisoCamara;
    private static final String TAG = "PERFIL_DEBUG";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_perfil, container, false);

        // 1. Inicializar vistas
        imgPerfil = root.findViewById(R.id.img_perfil);
        etNombre = root.findViewById(R.id.et_nombre_usuario);
        etPassword = root.findViewById(R.id.et_password);
        Button btnHacerFoto = root.findViewById(R.id.btn_hacer_foto);
        Button btnGuardar = root.findViewById(R.id.btn_guardar_perfil);

        // --- CARGAR DATOS DEL USUARIO ACTUAL (SharedPreferences) ---
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("MisPreferencias", android.content.Context.MODE_PRIVATE);
        String nombreGuardado = prefs.getString("nombre_usuario", "Mi Usuario");
        String passGuardada = prefs.getString("password_usuario", "");
        String idUsuario = prefs.getString("id_usuario", "1");

        etNombre.setText(nombreGuardado);
        etPassword.setText(passGuardada);

        // --- DESCARGAR LA IMAGEN DEL SERVIDOR CON GLIDE (Apuntes Pág. 30) ---
        // Se añade timestamp para evitar problemas de caché al actualizar la foto [cite: 30]
        String direccion = "http://34.12.153.133:81/perfil_" + idUsuario + ".jpg?v=" + System.currentTimeMillis();

        Log.d("GLIDE_DEBUG", "Intentando cargar: " + direccion);
        com.bumptech.glide.Glide.with(requireContext())
                .load(direccion)
                .into(imgPerfil);

        // 2. Configurar el launcher para recoger la foto y ESCALARLA [cite: 15]
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK) {
                        try {
                            int reqWidth = 500;
                            int reqHeight = 500;
                            Log.d(TAG, "URI imagen: " + uriImagen);
                            // 🥇 PASO 1: Leer dimensiones sin cargar la imagen
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;

                            InputStream is = requireActivity()
                                    .getContentResolver()
                                    .openInputStream(uriImagen);

                            BitmapFactory.decodeStream(is, null, options);
                            is.close();

                            int width = options.outWidth;
                            int height = options.outHeight;
                            Log.d(TAG, "Dimensiones originales: " + width + "x" + height);
                            // 🥈 PASO 2: Calcular inSampleSize
                            int inSampleSize = 1;

                            if (height > reqHeight || width > reqWidth) {
                                int halfHeight = height / 2;
                                int halfWidth = width / 2;

                                while ((halfHeight / inSampleSize) >= reqHeight &&
                                        (halfWidth / inSampleSize) >= reqWidth) {
                                    inSampleSize *= 2;
                                }
                            }

                            // 🥉 PASO 3: Cargar imagen reducida
                            options.inJustDecodeBounds = false;
                            options.inSampleSize = inSampleSize;

                            InputStream is2 = requireActivity()
                                    .getContentResolver()
                                    .openInputStream(uriImagen);

                            Bitmap bitmapReducido = BitmapFactory.decodeStream(is2, null, options);
                            is2.close();

                            // (Opcional) Ajuste fino para que encaje EXACTO en 500x500 manteniendo ratio
                            int anchoImagen = bitmapReducido.getWidth();
                            int altoImagen = bitmapReducido.getHeight();

                            float ratioImagen = (float) anchoImagen / (float) altoImagen;
                            float ratioDestino = (float) reqWidth / (float) reqHeight;

                            int anchoFinal = reqWidth;
                            int altoFinal = reqHeight;

                            if (ratioDestino > ratioImagen) {
                                anchoFinal = (int) (reqHeight * ratioImagen);
                            } else {
                                altoFinal = (int) (reqWidth / ratioImagen);
                            }

                            bitmapFotoActual = Bitmap.createScaledBitmap(
                                    bitmapReducido,
                                    anchoFinal,
                                    altoFinal,
                                    true
                            );
                            Log.d(TAG, "inSampleSize: " + inSampleSize);

                            // Mostrar en pantalla
                            imgPerfil.setImageBitmap(bitmapFotoActual);
                            Log.d(TAG, "Bitmap reducido: " + bitmapReducido.getWidth() + "x" + bitmapReducido.getHeight());

                            // Liberar memoria
                            bitmapReducido.recycle();

                        } catch (Exception e) {
                            Log.e(TAG, "Error cargando imagen", e);
                        }
                    }
                });

        // Launcher para permisos (Cámara)
        pedirPermisoCamara = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) { abrirCamara(); }
                    else { Toast.makeText(getContext(), getString(R.string.permisos_denegados), Toast.LENGTH_SHORT).show(); }
                });

        // 3. Eventos
        btnHacerFoto.setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                pedirPermisoCamara.launch(Manifest.permission.CAMERA);
            }
        });

        btnGuardar.setOnClickListener(v -> subirPerfilAlServidor());

        return root;
    }

    private void abrirCamara() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nombrefich = "IMG_" + timeStamp + "_";
        File directorio = requireActivity().getFilesDir();

        try {
            File fichImg = File.createTempFile(nombrefich, ".jpg", directorio);
            uriImagen = FileProvider.getUriForFile(requireContext(), "com.das.das_proyecto1.provider", fichImg);

            Intent elIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            elIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriImagen);
            takePictureLauncher.launch(elIntent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void subirPerfilAlServidor() {
        String nuevoNombre = etNombre.getText().toString();
        String nuevaPass = etPassword.getText().toString();

        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("MisPreferencias", android.content.Context.MODE_PRIVATE);
        String idUsuario = prefs.getString("id_usuario", "1");

        Toast.makeText(getContext(), getString(R.string.guardando), Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Convertir el bitmap ESCALADO a Base64 [cite: 20]
                String fotoEnBase64 = "";
                if (bitmapFotoActual != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    // Calidad 80% es suficiente para una imagen de 500px
                    bitmapFotoActual.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                    byte[] fototransformada = stream.toByteArray();
                    fotoEnBase64 = Base64.encodeToString(fototransformada, Base64.DEFAULT);
                }

                // Parámetros URL (Apuntes Pág. 20) [cite: 20, 21, 22]
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("id_usuario", idUsuario)
                        .appendQueryParameter("nombre", nuevoNombre)
                        .appendQueryParameter("contrasena", nuevaPass)
                        .appendQueryParameter("imagen", fotoEnBase64);
                String parametrosURL = builder.build().getEncodedQuery();

                java.net.URL url = new java.net.URL("http://34.12.153.133:81/actualizar_perfil.php");
                java.net.HttpURLConnection conexion = (java.net.HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("POST");
                conexion.setDoOutput(true);

                java.io.OutputStream os = conexion.getOutputStream();
                java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(os, "UTF-8"));
                writer.write(parametrosURL);
                writer.flush();
                writer.close();
                os.close();

                int responseCode = conexion.getResponseCode();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conexion.getInputStream())
                );

                String linea;
                StringBuilder respuesta = new StringBuilder();

                while ((linea = reader.readLine()) != null) {
                    respuesta.append(linea);
                }

                reader.close();

                Log.d(TAG, "Respuesta servidor: " + respuesta.toString());
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), getString(R.string.cambios_guardados), Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}