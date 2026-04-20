package com.das.das_proyecto1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Login extends AppCompatActivity {
    private EditText usuario;
    private EditText contrasena;
    private Button btnEntrar, btnRegistrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        usuario = findViewById(R.id.et_usuario);
        contrasena = findViewById(R.id.et_contrasena);
        btnEntrar = findViewById(R.id.btn_iniciar);
        btnRegistrar = findViewById(R.id.btn_registrar);
        //listeners
        btnEntrar.setOnClickListener(v -> lanzarWorker("login"));
        btnRegistrar.setOnClickListener(v -> lanzarWorker("registro"));
    }

    private void lanzarWorker(String tipo) {
        String u = usuario.getText().toString().trim();
        String c = contrasena.getText().toString().trim();
        //comprobamos que los campos no estén vacíos
        if (u.isEmpty()) {
            usuario.setError(getString(R.string.vacio_u));
            usuario.requestFocus();
            return;
        }
        if (c.isEmpty()) {
            contrasena.setError(getString(R.string.vacia_c));
            contrasena.requestFocus();
            return;
        }
        Data datos = new Data.Builder()
                .putString("usuario", u)
                .putString("contrasena", c)
                .putString("tipo", tipo)
                .build();
        OneTimeWorkRequest otwr = new OneTimeWorkRequest.Builder(ConexionBDWebService.class)
                .setInputData(datos)
                .build();
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(otwr.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        procesarResultado(workInfo.getOutputData().getString("resultado"));
                    }
                });
        WorkManager.getInstance(this).enqueue(otwr);
    }

    private void procesarResultado(String result) {
        if (result == null) {
            Toast.makeText(this, getString(R.string.error_conexion), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            //parseamos
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(result);

            String status = (String) json.get("status");
            String mensaje = (String) json.get("mensaje");

            if ("ok".equals(status)) {
                //guardar datos del usuario
                android.content.SharedPreferences prefs = getSharedPreferences("MisPreferencias", android.content.Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putString("nombre_usuario", usuario.getText().toString());
                editor.putString("password_usuario", contrasena.getText().toString());
                if (json.containsKey("id_usuario") && json.get("id_usuario") != null) {
                    String idUsuario = String.valueOf(json.get("id_usuario"));
                    editor.putString("id_usuario", idUsuario);
                } else {
                    Log.e("ERROR_LOGIN", "revisar login.php");
                }
                editor.apply();

                //login correcto --> mainactivity
                Intent intent = new Intent(Login.this, MainActivity.class);
                intent.putExtra("nombre_usuario", usuario.getText().toString());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_respuesta), Toast.LENGTH_SHORT).show();
        }
    }
}