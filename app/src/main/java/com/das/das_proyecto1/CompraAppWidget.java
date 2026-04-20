package com.das.das_proyecto1;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class CompraAppWidget extends AppWidgetProvider {

    public static final String acc_actualizar = "com.das.das_proyecto1.AUTO_UPDATE";
    private void actualizarWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.compra_app_widget);
        //obtenemos lista de la bd
        BaseDatosHelper dbHelper = new BaseDatosHelper(context);
        List<Producto> listaProductos = dbHelper.obtenerCompra();

        StringBuilder listaTexto = new StringBuilder();
        if (listaProductos != null && !listaProductos.isEmpty()) {
            //desordenar lista
            Collections.shuffle(listaProductos);
            for (Producto p : listaProductos) {
                listaTexto.append("• ").append(p.getNombre()).append("\n");
            }
            views.setTextViewText(R.id.appwidget_text, listaTexto.toString());
        } else {
            views.setTextViewText(R.id.appwidget_text, context.getString(R.string.lista_vacia));
        }

        //tocamos widget--> abrimos appq
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //actualizar
        for (int appWidgetId : appWidgetIds) {
            actualizarWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        //configuramos la alarma cuando añadimos el widget
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CompraAppWidget.class);
        intent.setAction(acc_actualizar);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        //cada 30s
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 30000, pendingIntent);
    }

    @Override
    public void onDisabled(Context context) {
        //apagamos la alarma si se borra
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, CompraAppWidget.class);
        intent.setAction(acc_actualizar);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        //forzar actualización 30s
        if (acc_actualizar.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, CompraAppWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}