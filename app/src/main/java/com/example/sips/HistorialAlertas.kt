package com.example.sips

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Una alerta del historial: qué sensor, qué pasó, cuánto marcó y cuándo
data class Alerta(
    val sensor: String,   // "Temperatura", "CO2", "Luminosidad"
    val titulo: String,   // por ejemplo "Temperatura baja"
    val valor: String,    // por ejemplo "15 °C"
    val fecha: Long       // momento en milisegundos
)

// Crea una alerta a partir de un sensor que está fuera de rango
fun crearAlerta(sensor: Sensor): Alerta {
    val esBajo = sensor.valor < sensor.minimo
    val femenino = sensor.nombre != "CO2" // "Temperatura baja" / "CO2 bajo"
    val estado = if (esBajo) {
        if (femenino) "baja" else "bajo"
    } else {
        if (femenino) "alta" else "alto"
    }
    return Alerta(
        sensor = sensor.nombre,
        titulo = "${sensor.nombre} $estado",
        valor = sensor.valorTexto(),
        fecha = System.currentTimeMillis()
    )
}

// Ícono según el sensor
fun iconoDe(sensor: String): ImageVector = when (sensor) {
    "Temperatura" -> Icons.Filled.Thermostat
    "CO2" -> Icons.Filled.Co2
    "Luminosidad" -> Icons.Filled.Lightbulb
    else -> Icons.Filled.Warning
}

// Texto de la hora: "Hoy, 15:10" o "12/11, 09:30"
fun formatearFecha(fecha: Long): String {
    val hoy = Calendar.getInstance()
    val dia = Calendar.getInstance().apply { timeInMillis = fecha }
    val esHoy = hoy.get(Calendar.YEAR) == dia.get(Calendar.YEAR) &&
            hoy.get(Calendar.DAY_OF_YEAR) == dia.get(Calendar.DAY_OF_YEAR)
    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(fecha))
    return if (esHoy) {
        "Hoy, $hora"
    } else {
        SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(fecha)) + ", $hora"
    }
}

// ---------- Guardar en el teléfono (SharedPreferences) ----------
// Así el historial no se borra al cerrar la app.
// Cada alerta se guarda como una línea: sensor|titulo|valor|fecha

private const val ARCHIVO = "historial_alertas"
private const val CLAVE = "alertas"

fun guardarAlertas(context: Context, alertas: List<Alerta>) {
    val texto = alertas.joinToString("\n") { a ->
        "${a.sensor}|${a.titulo}|${a.valor}|${a.fecha}"
    }
    context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        .edit()
        .putString(CLAVE, texto)
        .apply()
}

fun cargarAlertas(context: Context): List<Alerta> {
    val texto = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        .getString(CLAVE, "") ?: ""
    if (texto.isBlank()) return emptyList()

    return texto.lines().mapNotNull { linea ->
        val partes = linea.split("|")
        if (partes.size == 4) {
            Alerta(partes[0], partes[1], partes[2], partes[3].toLongOrNull() ?: 0L)
        } else {
            null // línea dañada: se ignora
        }
    }
}
