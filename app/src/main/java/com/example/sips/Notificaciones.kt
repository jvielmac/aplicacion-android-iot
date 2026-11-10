package com.example.sips

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

// Identificador del canal de notificaciones de la app
const val CANAL_ALERTAS = "alertas_sensores"

// Desde Android 8 las notificaciones tienen que pertenecer a un "canal".
// El usuario puede activar o desactivar cada canal en los ajustes de la app.
fun crearCanalNotificaciones(context: Context) {
    val canal = NotificationChannel(
        CANAL_ALERTAS,
        "Alertas de sensores",
        NotificationManager.IMPORTANCE_HIGH
    )
    canal.description = "Avisa cuando un sensor está fuera de rango"

    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(canal)
}

// Revisa todos los sensores y manda una notificación por cada uno que esté fuera de rango
fun revisarSensoresYNotificar(context: Context, sensores: List<Sensor>) {
    sensores.forEachIndexed { indice, sensor ->
        if (sensor.fueraDeRango()) {
            enviarNotificacion(context, indice, sensor)
        }
    }
}

fun enviarNotificacion(context: Context, id: Int, sensor: Sensor) {
    // Desde Android 13 hay que tener permiso del usuario para mostrar notificaciones
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        return // sin permiso no se puede notificar
    }

    // Al tocar la notificación se abre la app
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )

    val notificacion = NotificationCompat.Builder(context, CANAL_ALERTAS)
        .setSmallIcon(R.drawable.ic_alerta)
        .setContentTitle("${sensor.nombre} fuera de rango")
        .setContentText(
            "Valor actual: ${sensor.valorTexto()} " +
            "(normal: ${sensor.minimo} a ${sensor.maximo} ${sensor.unidad})"
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true) // se borra al tocarla
        .build()

    NotificationManagerCompat.from(context).notify(id, notificacion)
}
