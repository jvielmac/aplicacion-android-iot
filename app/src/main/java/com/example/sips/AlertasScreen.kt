package com.example.sips

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.ui.graphics.vector.ImageVector


// Pantalla de Alertas (solo diseño, datos de ejemplo)
@Composable
fun AlertasScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Título
        Text("Historial de Alertas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold)

        // Estado conexión
        Text("Arduino conectado!",
            color = Color(0xFF2EB82E),
            fontWeight = FontWeight.Bold)

        // Lista de alertas (de ejemplo)
        TarjetaAlerta(
            Icons.Filled.Thermostat,
            "Temperatura baja",
            "15° C",
            "Hoy, 15:10")
        TarjetaAlerta(
            Icons.Filled.Co2,
            "CO2 alto",
            "1200 ppm",
            "Ayer, 18:45")
        TarjetaAlerta(
            Icons.Filled.Lightbulb,
            "Poca luz",
            "80 Lux",
            "Ayer, 09:30")
    }
}

// Una tarjeta de alerta: ícono, qué pasó, valor medido y cuándo
@Composable
fun TarjetaAlerta(icono: ImageVector, titulo: String, valor: String, hora: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp))
    {
        Icon(imageVector = icono,
            contentDescription = null,
            modifier = Modifier.size(28.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(titulo,
                fontSize = 18.sp)
            Text(valor,
                fontSize = 12.sp)
        }
        Text(hora, fontSize = 12.sp, color = Color.Gray)
    }
}

// Vista previa: pantalla de alertas con la barra de abajo (NavBar está en BarraInferior.kt)
@Preview(showBackground = true)
@Composable
fun AlertasPreview() {
    Scaffold(
        bottomBar = { NavBar() }
    ) { innerPadding ->
        AlertasScreen(modifier = Modifier.padding(innerPadding))
    }
}