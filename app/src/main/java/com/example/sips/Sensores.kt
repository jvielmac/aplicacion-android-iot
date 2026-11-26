package com.example.sips

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Co2
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.ui.graphics.vector.ImageVector

// Datos de un sensor: valor medido y rango normal (mínimo y máximo)
data class Sensor(
    val nombre: String,
    val clave: String,      // nombre que usa el Arduino en el mensaje: "CO2", "LUZ", "TEMP"
    val valor: Int,
    val unidad: String,     // vacío = lectura directa del Arduino (0 a 1023)
    val minimo: Int,
    val maximo: Int,
    val limite: Int,        // valor más alto que puede llegar a mandar el Arduino
    val icono: ImageVector
) {
    // El sensor está en alerta si el valor está bajo el mínimo o sobre el máximo
    fun fueraDeRango(): Boolean = valor < minimo || valor > maximo

    // Texto para mostrar, por ejemplo "22 °C" o "512"
    fun valorTexto(): String = if (unidad.isEmpty()) "$valor" else "$valor $unidad"
}

// Valores iniciales y rangos normales.
// IMPORTANTE: los rangos son los MISMOS umbrales del código del Arduino,
// para que el LED y la app avisen exactamente en el mismo momento.
//   Arduino: CO2 > 600 = alerta | LUZ < 300 = alerta | TEMP < 15 o > 30 = alerta
// CO2 y Luz son la lectura directa del potenciómetro/sensor (0 a 1023), no ppm ni lux.
val sensoresEjemplo = listOf(
    Sensor("Luminosidad", "LUZ", 500, "", minimo = 300, maximo = 1023, limite = 1023,
        icono = Icons.Outlined.Lightbulb),
    Sensor("Temperatura", "TEMP", 22, "°C", minimo = 15, maximo = 30, limite = 50,
        icono = Icons.Outlined.Thermostat),
    Sensor("CO2", "CO2", 400, "", minimo = 0, maximo = 600, limite = 1023,
        icono = Icons.Outlined.Co2)
)
