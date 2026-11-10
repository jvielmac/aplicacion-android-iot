package com.example.sips

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
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

    // Datos de los sensores (por ahora de ejemplo, ver Sensores.kt).
    // Es una lista "de estado": si cambia un valor, la pantalla se actualiza sola.
    val sensores = remember { mutableStateListOf(*sensoresEjemplo.toTypedArray()) }

    // ---------- Notificaciones ----------
    val context = LocalContext.current

    // Panel de pruebas: cambia el valor de un sensor
    val cambiarValor: (Int, Int) -> Unit = { indice, nuevoValor ->
        sensores[indice] = sensores[indice].copy(valor = nuevoValor)
    }

    // Panel de pruebas: al soltar el control, si el sensor quedó fuera de rango se notifica
    val alSoltar: (Int) -> Unit = { indice ->
        val sensor = sensores[indice]
        if (sensor.fueraDeRango()) {
            enviarNotificacion(context, indice, sensor)
        }
    }

    // Pide el permiso de notificaciones; cuando el usuario responde, revisa los sensores
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            revisarSensoresYNotificar(context, sensores)
        }
    }

    // LaunchedEffect(Unit) se ejecuta una sola vez, cuando se abre la app
    LaunchedEffect(Unit) {
        crearCanalNotificaciones(context)

        val faltaPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

        if (faltaPermiso) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            revisarSensoresYNotificar(context, sensores)
        }
    }
    // ------------------------------------

    when (pantalla) {
        "EstadoActual" -> {
            EstadoActual(
                pantallaActual = pantalla,
                onCambiarPantalla = cambiarPantalla,
                sensores = sensores,
                onCambiarValor = cambiarValor,
                onSoltar = alSoltar
            )
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
    // La vista previa muestra la pantalla directo, porque App() pide permisos
    // y eso solo funciona en un teléfono o emulador
    SipsTheme {
        EstadoActual(
            pantallaActual = "EstadoActual",
            onCambiarPantalla = {},
            sensores = sensoresEjemplo
        )
    }
}
