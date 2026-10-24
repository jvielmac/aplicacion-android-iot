package com.example.sips

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sips.ui.theme.SipsTheme
@Composable
fun PantallaErrorConexion() {
    Scaffold { innerPadding ->

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top)
        {
            // Espaciado para que no se pegue la imagen con el texto
            Spacer(modifier = Modifier.height(48.dp))
            // Icono
            Image(painter = painterResource(id = R.drawable.ic_bluetooth_disabled),
                contentDescription = "Bluetooth desconectado",
                colorFilter = ColorFilter.tint(Color.Black),
                modifier = Modifier.size(130.dp))
            Spacer(modifier = Modifier.height(64.dp))
            Text("Error de Conexión",
                fontSize = 36.sp,
                textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(40.dp))
            Text("La aplicación no se ha conectado con el dispositivo Arduino. " +
                    "Revisa la conectividad de Bluetooth.",
                fontSize = 20.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center)
        }
    }
}
