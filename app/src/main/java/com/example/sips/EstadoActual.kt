package com.example.sips

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Co2
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EstadoActual(
    pantallaActual: String,
    onCambiarPantalla: (String) -> Unit,
    sensores: List<Sensor>,
    onCambiarValor: (Int, Int) -> Unit = { _, _ -> },
    onSoltar: (Int) -> Unit = {},
    estado: EstadoConexion = EstadoConexion.DESCONECTADO,
    nombreDispositivo: String = "",
    ultimaLectura: Long = 0L,
    onConectar: () -> Unit = {},
    onDesconectar: () -> Unit = {}
) {
    // Scaffold: estructura base de la pantalla, con la barra de navegación abajo
    Scaffold(
        bottomBar = { NavBar(pantallaActual, onCambiarPantalla) }
    ) { innerPadding -> // innerPadding: espacio para que la barra no tape el contenido

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Título
            Text("Estado Actual - Sala", fontSize = 24.sp)

            // Estado de la conexión Bluetooth + botón para conectar/desconectar
            FilaConexion(estado, nombreDispositivo, ultimaLectura, onConectar, onDesconectar)

            // Foto de la sala (res/drawable/foto_sala.jpg)
            Image(
                painter = painterResource(id = R.drawable.foto_sala),
                contentDescription = "Foto de la sala",
                contentScale = ContentScale.Crop, // recorta para llenar el espacio sin deformarse
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            // Espacio vacío del diseño
            Spacer(modifier = Modifier.height(48.dp))

            // Tarjetas de sensores: una por cada sensor de la lista
            for (sensor in sensores) {
                TarjetaSensor(
                    nombre = sensor.nombre,
                    valor = sensor.valorTexto(),
                    alerta = sensor.fueraDeRango(),
                    icono = sensor.icono
                )
            }

            // Panel para probar las notificaciones (ver PanelPruebas.kt)
            // (se oculta mientras el Arduino está conectado, porque él manda los valores reales)
            if (MODO_PRUEBAS && estado != EstadoConexion.CONECTADO) {
                PanelPruebas(sensores, onCambiarValor, onSoltar)
            }
        }
    }
}

@Composable
fun TarjetaSensor(nombre: String, valor: String, alerta: Boolean, icono: ImageVector) {
    // Contexto de Android, necesario para mostrar el Toast
    val context = LocalContext.current

    Row(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
        // Al tocar la tarjeta se muestra un Toast con el estado del sensor
        .clickable {
            val mensaje = if (alerta) {
                "$nombre: $valor - fuera de rango"
            } else {
                "$nombre: $valor - todo OK"
            }
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
        .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically)
    {
        Icon(
            imageVector = icono,
            contentDescription = "",
            modifier = Modifier
                .padding(end = 8.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(nombre, fontSize = 18.sp)
            Text(valor, fontSize = 12.sp)
        }

        if (alerta) {
            // Text("ALERTA", color = Color(0xFFE5534B), fontWeight = FontWeight.Bold)
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = "ALERTA",
                tint = Color(228, 105, 98),
                modifier = Modifier.size(42.dp)
            )

        } else {
            // Text("OK", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "OK",
                tint = Color(103, 80, 164),
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

// Fila con el estado de la conexión con el Arduino y el botón Conectar / Desconectar
@Composable
fun FilaConexion(
    estado: EstadoConexion,
    nombreDispositivo: String,
    ultimaLectura: Long,
    onConectar: () -> Unit,
    onDesconectar: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically)
    {
        Column(modifier = Modifier.weight(1f)) {
            when (estado) {
                EstadoConexion.CONECTADO -> {
                    Text("Arduino conectado! ($nombreDispositivo)",
                        color = Color(0xFF2EB82E),
                        fontWeight = FontWeight.Bold)
                    val texto = if (ultimaLectura == 0L) "Esperando datos..."
                        else "Última lectura: " + java.text.SimpleDateFormat(
                            "HH:mm:ss", java.util.Locale.getDefault()
                        ).format(java.util.Date(ultimaLectura))
                    Text(texto, fontSize = 11.sp)
                }
                EstadoConexion.CONECTANDO -> {
                    Text("Conectando con $nombreDispositivo...",
                        color = Color(0xFFFFA000),
                        fontWeight = FontWeight.Bold)
                }
                EstadoConexion.DESCONECTADO -> {
                    Text("Arduino desconectado",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold)
                    Text("Mostrando valores de prueba", fontSize = 11.sp)
                }
            }
        }

        when (estado) {
            EstadoConexion.CONECTADO -> OutlinedButton(onClick = onDesconectar) { Text("Desconectar") }
            EstadoConexion.CONECTANDO -> {}
            EstadoConexion.DESCONECTADO -> Button(onClick = onConectar) { Text("Conectar") }
        }
    }
}
