package com.das.das_proyecto1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //leer el idioma de SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        String idiomaGuardado = prefs.getString("idioma", "es"); //español por defecto
        //aplicar idioma
        java.util.Locale locale = new java.util.Locale(idiomaGuardado);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        setContentView(R.layout.activity_main);

        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //menú lateral (navigation drawer)
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        //botón de hamburguesa
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open_nav, R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment fragmentSeleccionado = null;
            int itemId = item.getItemId();
            if(itemId==R.id.nav_perfil){
                fragmentSeleccionado= new PerfilFragment();
                toolbar.setTitle(getString(R.string.miperfil));
            }
            else if (itemId == R.id.nav_menu_semanal) {//si tocamos menú semanal que nos lleve ahí
                fragmentSeleccionado = new MenuSemanalFragment();
                toolbar.setTitle(getString(R.string.menu_semanal));
            } else if (itemId == R.id.nav_lista_compra) {
                fragmentSeleccionado = new ListaCompraFragment();
                toolbar.setTitle(getString(R.string.lista_compra));
            }
            else if (itemId == R.id.nav_supermercados) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SupermercadosFragment())
                        .commit();
            }
            else if (itemId == R.id.nav_ideas_comer) {
                fragmentSeleccionado = new IdeasComerFragment();//ideas para comer
                toolbar.setTitle(getString(R.string.ideas_comer));
            } else if (itemId==R.id.nav_idioma) {//cambiar el idioma
                //leer idioma actual
                android.content.SharedPreferences pref = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
                String idiomaAct = pref.getString("idioma", "es");
                //cambiar idioma
                String nuevoIdioma = idiomaAct.equals("es") ? "en" : "es";
                //guardar
                pref.edit().putString("idioma", nuevoIdioma).apply();
                //reiniciar actividad
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId==R.id.nav_logout) {
                Intent intent = new Intent(MainActivity.this, Login.class);

                // 2. Añadimos Flags para borrar el historial de pantallas y que no pueda hacer "Back"
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                // 3. Saltamos y destruimos la MainActivity actual
                startActivity(intent);
                finish();

                return true;
            }

            //cambio de pantalla
            if (fragmentSeleccionado != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragmentSeleccionado)
                        .commit();
            }
            //cerrar caja
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        //cargamos menú semanal nada más abrir la app
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MenuSemanalFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_menu_semanal);
            toolbar.setTitle(R.string.menu_semanal);
        }

        // necesario para que cuando demos para atrás con el menú lateral abierto lo cerremos
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    // menú lateral abierto -> cerrarlo
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    //menú lateral cerrado -> salir normal
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        //comprobamos permisos y lanzamos noti
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String nombre = extras.getString("nombre_usuario");
            Toast.makeText(this, getString(R.string.hola) + nombre + "!", Toast.LENGTH_LONG).show();
        }
    }

}