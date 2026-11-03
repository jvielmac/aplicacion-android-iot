package com.example.sips

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Barra inferior: Estado Actual / Alertas
// pantallaActual: para saber qué botón marcar como seleccionado
// onCambiarPantalla: función que entrega MainActivity para cambiar de pantalla
@Composable
fun NavBar(pantallaActual: String, onCambiarPantalla: (String) -> Unit) {
    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {

        NavigationBarItem(
            selected = pantallaActual == "EstadoActual",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = "Estado Actual"
                )
            },
            label = { Text("Estado Actual") },
            onClick = { onCambiarPantalla("EstadoActual") },
        )

        NavigationBarItem(
            selected = pantallaActual == "HistorialAlertas",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Alertas"
                )
            },
            label = { Text("Alertas") },
            onClick = { onCambiarPantalla("HistorialAlertas") },
        )
    }
}
