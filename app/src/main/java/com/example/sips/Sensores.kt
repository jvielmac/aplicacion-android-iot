package com.example.sips

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Co2
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.ui.graphics.vector.ImageVector

// Datos de un sensor: valor medido y rango normal (mínimo y máximo)
data class Sensor(
    val nombre: String,
    val valor: Int,
    val unidad: String,
    val minimo: Int,
    val maximo: Int,
    val icono: ImageVector
) {
    // El sensor está en alerta si el valor está bajo el mínimo o sobre el máximo
    fun fueraDeRango(): Boolean = valor < minimo || valor > maximo

    // Texto para mostrar, por ejemplo "15 °C"
    fun valorTexto(): String = "$valor $unidad"
}

// Valores de ejemplo (después se reemplazan por los que mande el Arduino)
val sensoresEjemplo = listOf(
    Sensor("Luminosidad", 300, "Lux", minimo = 200, maximo = 1000, icono = Icons.Outlined.Lightbulb),
    Sensor("Temperatura", 15, "°C", minimo = 18, maximo = 26, icono = Icons.Outlined.Thermostat),
    Sensor("CO2", 520, "ppm", minimo = 0, maximo = 1000, icono = Icons.Outlined.Co2)
)
