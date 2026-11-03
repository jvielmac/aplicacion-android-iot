package com.example.sips

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.sips.ui.theme.SipsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SipsTheme {
                App()
            }
        }
    }
}

// Decide qué pantalla se muestra
@Composable
fun App() {
    // Pantalla actual. Es un "estado": cuando cambia, Compose redibuja la pantalla.
    // (Antes era un String normal y además decía "EstodoActual", por eso salía en blanco)
    var pantalla by remember { mutableStateOf("EstadoActual") }

    // Función que se le pasa a la barra inferior para cambiar de pantalla
    val cambiarPantalla: (String) -> Unit = { nueva -> pantalla = nueva }

    when (pantalla) {
        "EstadoActual" -> {
            EstadoActual(pantallaActual = pantalla, onCambiarPantalla = cambiarPantalla)
        }
        "HistorialAlertas" -> {
            Scaffold(
                bottomBar = { NavBar(pantallaActual = pantalla, onCambiarPantalla = cambiarPantalla) }
            ) { innerPadding ->
                AlertasScreen(modifier = Modifier.padding(innerPadding))
            }
        }
        "PantallaErrorConexion" -> {
            PantallaErrorConexion()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppPreview() {
    SipsTheme {
        App()
    }
}
