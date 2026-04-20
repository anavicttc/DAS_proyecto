package com.das.das_proyecto1;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ListaCompraFragment extends Fragment {
    private BaseDatosHelper bdHelper;
    private RecyclerView recyclerView;
    private CompraAdapter adapter;
    private List<Producto> listaProductos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_compra, container, false);
        bdHelper = new BaseDatosHelper(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewCompra);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        cargarProductos();

        EditText etNuevoProducto = view.findViewById(R.id.etNuevoProducto);
        Button btnAddProducto = view.findViewById(R.id.btnAddProducto);
        btnAddProducto.setOnClickListener(v -> {
            String nombreProducto = etNuevoProducto.getText().toString().trim();

            if (!nombreProducto.isEmpty()) {
                //guardamos en la bd
                bdHelper.insertarProducto(nombreProducto);
                actualizarWidget();
                etNuevoProducto.setText(""); //limpiar caja
                cargarProductos(); //recargar

                //noti a los 5s REVISAR
                programarNotificacion(nombreProducto);

                Toast.makeText(requireContext(), R.string.añadido_a_la_lista, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),R.string.anadir_lista_error , Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    private void cargarProductos() {
        listaProductos = bdHelper.obtenerCompra();
        adapter = new CompraAdapter(listaProductos);
        recyclerView.setAdapter(adapter);
    }
    private void actualizarWidget(){
        // --- FORZAR ACTUALIZACIÓN DEL WIDGET ---
        android.content.Intent intentWidget = new android.content.Intent(requireContext(), CompraAppWidget.class);
        intentWidget.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Obtener los IDs de todos los widgets de la compra que el usuario tenga en la pantalla
        int[] ids = android.appwidget.AppWidgetManager.getInstance(requireActivity().getApplication())
                .getAppWidgetIds(new android.content.ComponentName(requireActivity().getApplication(), CompraAppWidget.class));

        intentWidget.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        // Enviar el aviso para que el widget ejecute su onUpdate() inmediatamente
        requireContext().sendBroadcast(intentWidget);
    }
    //notificación local cuando añadimos producto a la lista
    private void programarNotificacion(String producto) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), NotificacionPrincipal.class);
                intent.putExtra("titulo_notificacion", getString(R.string.noti_compra));
                intent.putExtra("texto_notificacion", getString(R.string.noti_compra_sub) + " " + producto);
                getContext().sendBroadcast(intent); //disparar aviso
            }
        }, 5000); //5s
    }
    private class CompraAdapter extends RecyclerView.Adapter<CompraAdapter.CompraViewHolder> {
        private List<Producto> productos;
        public CompraAdapter(List<Producto> productos) { this.productos = productos; }
        @NonNull
        @Override
        public CompraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.compra, parent, false);
            return new CompraViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CompraViewHolder holder, int position) {
            Producto producto = productos.get(position);
            holder.tvNombre.setText(producto.getNombre());

            //botón para eliminar producto de la lista
            holder.ivBorrar.setOnClickListener(v -> {
                //diálogo confirmación
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.borrar_lista)
                        .setMessage(getString(R.string.mensaje_quitar)+" " + producto.getNombre() +"?")
                        .setPositiveButton(R.string.si_borrar_lista, (dialog, which) -> {
                            //borrar de la bd
                            bdHelper.borrarProducto(producto.getId());
                            //recargar la página
                            cargarProductos();
                            Toast.makeText(requireContext(), R.string.producto_eliminado, Toast.LENGTH_SHORT).show();
                            actualizarWidget();
                        })
                        .setNegativeButton(R.string.cancelar, null)
                        .show();
            });
        }
        @Override
        public int getItemCount() { return productos.size(); }
        class CompraViewHolder extends RecyclerView.ViewHolder {
            TextView tvNombre;
            android.widget.ImageView ivBorrar; //imagen de basura
            public CompraViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNombre = itemView.findViewById(R.id.tvNombreProducto);
                ivBorrar = itemView.findViewById(R.id.ivBorrarProducto); //unir al botón
            }
        }
    }
}