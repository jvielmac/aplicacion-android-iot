package com.example.sips

import android.R.attr.end
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
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
import com.example.sips.ui.theme.SipsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SipsTheme {
                Scaffold( // ToDo: Comprender esto epico
                    // Barra inferior: Estado Actual / Alertas
                    bottomBar = { NavBar() }
                ) { innerPadding -> // ToDo: Comprender esto

                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Título
                        Text("Estado Actual - Sala", fontSize = 24.sp)

                        // Estado conexión
                        Text("Arduino conectado!",
                            color = Color(0xFF2EB82E),
                            fontWeight = FontWeight.Bold)
                        Text("Actualizado hace 3 min", fontSize = 11.sp)

                        // Foto sala
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color.LightGray),
                            contentAlignment = Alignment.Center)
                        {
                            Text("Foto de la sala")
                        }

                        // Espacio vacío del diseño
                        Spacer(modifier = Modifier.height(48.dp))

                        // Tarjetas de sensores
                        TarjetaSensor(
                            "Luminosidad", "300 Lux", alerta = false,
                            icono = Icons.Outlined.Lightbulb
                        )
                        TarjetaSensor(
                            "Temperatura", "15° C", alerta = true,
                            icono = Icons.Outlined.Thermostat
                        )
                        TarjetaSensor(
                            "CO2", "520 ppm", alerta = false,
                            icono = Icons.Outlined.Co2
                        )
                    }
                }
            }
        }
    }
}

// Una tarjeta: nombre y valor
@Composable
fun TarjetaSensor(nombre: String, valor: String, alerta: Boolean, icono: ImageVector) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
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

@Composable
fun NavBar() {
    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
        NavigationBarItem(
            selected = true,
            icon = { Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = "Estado Actual"
            )},
            label = { Text("Estado Actual") },
            onClick = {},
        )

        NavigationBarItem(
            selected = false,
            icon = { Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = "Alertas"
            )},
            label = { Text("Alertas") },
            onClick = {},
        )
    }
}