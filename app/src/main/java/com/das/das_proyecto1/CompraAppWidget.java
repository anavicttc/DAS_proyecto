package com.das.das_proyecto1;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class CompraAppWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.compra_app_widget);
        BaseDatosHelper dbHelper = new BaseDatosHelper(context);
        List<Producto> listaProductos = dbHelper.obtenerCompra();

        //formatear
        StringBuilder listaTexto = new StringBuilder();
        if (listaProductos.isEmpty()) {
            listaTexto.append(R.string.lista_vacia);
        } else {
            for (Producto p : listaProductos) {
                listaTexto.append("• ").append(p.getNombre()).append("\n");
            }
        }

        views.setTextViewText(R.id.appwidget_text, listaTexto.toString());

        Intent intent = new Intent(context, MainActivity.class);
        //FLAG_INMUTABLE obligatorio en nuevas versiones android
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //act widgets
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Funcionalidad cuando se crea el primer widget
    }

    @Override
    public void onDisabled(Context context) {
        // Funcionalidad cuando se borra el último widget
    }
}