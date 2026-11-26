package com.example.sips

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cambiar a false para ocultar el panel en la versión final
const val MODO_PRUEBAS = true

// Panel para probar las alertas: permite cambiar a mano el valor de cada sensor
// (simula lo que después va a mandar el Arduino)
@Composable
fun PanelPruebas(
    sensores: List<Sensor>,
    onCambiarValor: (indice: Int, nuevoValor: Int) -> Unit,
    onSoltar: (indice: Int) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color(0xFFFFA000), RoundedCornerShape(12.dp))
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp))
    {
        Text("Panel de pruebas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Mueve un valor fuera del rango normal y suéltalo para recibir la notificación",
            fontSize = 12.sp, color = Color.Gray)

        // Un control deslizante (Slider) por cada sensor
        sensores.forEachIndexed { indice, sensor ->
            Text("${sensor.nombre}: ${sensor.valorTexto()}  " +
                    "(normal: ${sensor.minimo} a ${sensor.maximo})",
                fontSize = 14.sp)
            Slider(
                value = sensor.valor.toFloat(),
                // Mientras se arrastra: se actualiza el valor y la tarjeta cambia en vivo
                onValueChange = { nuevo -> onCambiarValor(indice, nuevo.toInt()) },
                // Al soltar: se revisa si quedó fuera de rango para notificar
                onValueChangeFinished = { onSoltar(indice) },
                valueRange = 0f..(sensor.maximo * 2).toFloat()
            )
        }
    }
}
