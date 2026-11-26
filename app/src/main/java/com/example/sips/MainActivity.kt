package com.example.sips

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.sips.ui.theme.SipsTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

// Decide qué pantalla se muestra y conecta todas las partes de la app
@Composable
fun App() {
    // Pantalla actual. Es un "estado": cuando cambia, Compose redibuja la pantalla.
    var pantalla by remember { mutableStateOf("EstadoActual") }

    // Función que se le pasa a la barra inferior para cambiar de pantalla
    val cambiarPantalla: (String) -> Unit = { nueva -> pantalla = nueva }

    val context = LocalContext.current

    // Datos de los sensores (valores iniciales en Sensores.kt).
    // Es una lista "de estado": si cambia un valor, la pantalla se actualiza sola.
    val sensores = remember { mutableStateListOf(*sensoresEjemplo.toTypedArray()) }

    // ---------- Historial de alertas (ver HistorialAlertas.kt) ----------
    // Al abrir la app se carga lo que estaba guardado en el teléfono
    val historial = remember {
        mutableStateListOf<Alerta>().apply { addAll(cargarAlertas(context)) }
    }

    // Recuerda si cada sensor ya estaba en alerta, para no repetir la misma alerta
    // en cada lectura mientras el valor siga fuera de rango
    val estabaEnAlerta = remember {
        mutableStateListOf<Boolean>().apply { repeat(sensores.size) { add(false) } }
    }

    // Revisa un sensor: si ACABA de salir de su rango normal,
    // agrega la alerta al historial, la guarda y manda la notificación
    val revisarSensor: (Int) -> Unit = { indice ->
        val sensor = sensores[indice]
        val ahoraEnAlerta = sensor.fueraDeRango()
        if (ahoraEnAlerta && !estabaEnAlerta[indice]) {
            historial.add(0, crearAlerta(sensor)) // la más nueva queda arriba
            guardarAlertas(context, historial)
            enviarNotificacion(context, indice, sensor)
        }
        estabaEnAlerta[indice] = ahoraEnAlerta
    }

    // Revisa todos los sensores
    val revisarTodos: () -> Unit = {
        for (indice in sensores.indices) {
            revisarSensor(indice)
        }
    }

    // Botón "Borrar historial" de la pantalla de Alertas
    val borrarHistorial: () -> Unit = {
        historial.clear()
        guardarAlertas(context, historial)
    }

    // ---------- Panel de pruebas (sin Arduino) ----------
    val cambiarValor: (Int, Int) -> Unit = { indice, nuevoValor ->
        sensores[indice] = sensores[indice].copy(valor = nuevoValor)
    }
    val alSoltar: (Int) -> Unit = { indice -> revisarSensor(indice) }

    // ---------- Bluetooth (ver Bluetooth.kt) ----------
    val conexion = remember { ConexionBluetooth(context) }
    val scope = rememberCoroutineScope()
    var trabajoConexion by remember { mutableStateOf<Job?>(null) }
    var estado by remember { mutableStateOf(EstadoConexion.DESCONECTADO) }
    var nombreDispositivo by remember { mutableStateOf("") }
    var ultimaLectura by remember { mutableLongStateOf(0L) }
    var mostrarDispositivos by remember { mutableStateOf(false) }

    // Búsqueda de módulos BLE (HM-10 y similares)
    val modulosBle = remember { mutableStateListOf<ModuloBluetooth>() }
    var buscando by remember { mutableStateOf(false) }
    var detenerBusqueda by remember { mutableStateOf<(() -> Unit)?>(null) }

    val pararBusqueda: () -> Unit = {
        detenerBusqueda?.invoke()
        detenerBusqueda = null
        buscando = false
    }

    // Busca módulos BLE durante 10 segundos
    val iniciarBusqueda: () -> Unit = {
        pararBusqueda()
        modulosBle.clear()
        detenerBusqueda = conexion.buscarModulosBle { modulo ->
            // no repetir el mismo módulo en la lista
            if (modulosBle.none { it.dispositivo.address == modulo.dispositivo.address }) {
                modulosBle.add(modulo)
            }
        }
        buscando = true
        scope.launch {
            delay(10_000)
            pararBusqueda()
        }
    }

    // Permisos para buscar módulos BLE
    val pedirPermisosBusqueda = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.all { it }) {
            iniciarBusqueda()
        } else {
            Toast.makeText(context, "Sin permiso no se pueden buscar módulos BLE", Toast.LENGTH_LONG).show()
        }
    }

    val alPresionarBuscar: () -> Unit = {
        if (conexion.tienePermisoBusqueda()) {
            iniciarBusqueda()
        } else {
            pedirPermisosBusqueda.launch(conexion.permisosBusqueda())
        }
    }

    // Cada mensaje del Arduino ("CO2:512;LUZ:300;TEMP:22") actualiza los sensores
    val alRecibirMensaje: (String) -> Unit = { linea ->
        val datos = interpretarMensaje(linea)
        if (datos != null) {
            for (indice in sensores.indices) {
                val nuevoValor = datos[sensores[indice].clave]
                if (nuevoValor != null) {
                    sensores[indice] = sensores[indice].copy(valor = nuevoValor)
                    revisarSensor(indice) // ¿quedó fuera de rango? -> alerta + notificación
                }
            }
            ultimaLectura = System.currentTimeMillis()
        }
    }

    // Conecta con el dispositivo elegido y se queda escuchando en segundo plano
    val conectar: (ModuloBluetooth) -> Unit = { modulo ->
        pararBusqueda()
        mostrarDispositivos = false
        nombreDispositivo = if (modulo.esBle) "${modulo.nombre} · BLE" else modulo.nombre
        estado = EstadoConexion.CONECTANDO
        ultimaLectura = 0L
        trabajoConexion = scope.launch {
            try {
                conexion.conectarYLeer(
                    modulo,
                    alConectar = { estado = EstadoConexion.CONECTADO },
                    alRecibirLinea = alRecibirMensaje
                )
            } catch (e: CancellationException) {
                throw e // se desconectó a propósito (botón Desconectar)
            } catch (e: Exception) {
                conexion.desconectar()
                // Si el estado ya es DESCONECTADO, fue el botón "Desconectar": no es un error.
                // Si no, falló la conexión o se cortó -> pantalla de error
                if (estado != EstadoConexion.DESCONECTADO) {
                    estado = EstadoConexion.DESCONECTADO
                    pantalla = "PantallaErrorConexion"
                }
            }
        }
    }

    val desconectar: () -> Unit = {
        estado = EstadoConexion.DESCONECTADO // primero, para que no se tome como error
        trabajoConexion?.cancel()
        conexion.desconectar()
    }

    // Abre la lista de dispositivos emparejados (revisando antes Bluetooth y permisos)
    val abrirListaDispositivos: () -> Unit = {
        val adaptador = conexion.adaptador()
        if (adaptador == null) {
            Toast.makeText(context, "Este teléfono no tiene Bluetooth", Toast.LENGTH_LONG).show()
        } else if (!adaptador.isEnabled) {
            Toast.makeText(context, "Activa el Bluetooth del teléfono", Toast.LENGTH_LONG).show()
        } else {
            mostrarDispositivos = true
        }
    }

    // Permiso BLUETOOTH_CONNECT (Android 12 o superior)
    val pedirPermisoBluetooth = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            abrirListaDispositivos()
        } else {
            Toast.makeText(context, "Sin permiso de Bluetooth no se puede conectar", Toast.LENGTH_LONG).show()
        }
    }

    // Botón "Conectar"
    val alPresionarConectar: () -> Unit = {
        if (conexion.tienePermiso()) {
            abrirListaDispositivos()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pedirPermisoBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    // Al cerrar la app se corta la conexión
    DisposableEffect(Unit) {
        onDispose {
            pararBusqueda()
            conexion.desconectar()
        }
    }

    // ---------- Notificaciones ----------
    // Pide el permiso de notificaciones; cuando el usuario responde, revisa los sensores.
    // Aunque rechace el permiso, las alertas igual se guardan en el historial.
    val pedirPermisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        revisarTodos()
    }

    // LaunchedEffect(Unit) se ejecuta una sola vez, cuando se abre la app
    LaunchedEffect(Unit) {
        crearCanalNotificaciones(context)

        val faltaPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

        if (faltaPermiso) {
            pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            revisarTodos()
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
                onSoltar = alSoltar,
                estado = estado,
                nombreDispositivo = nombreDispositivo,
                ultimaLectura = ultimaLectura,
                onConectar = alPresionarConectar,
                onDesconectar = desconectar
            )
        }
        "HistorialAlertas" -> {
            Scaffold(
                bottomBar = { NavBar(pantallaActual = pantalla, onCambiarPantalla = cambiarPantalla) }
            ) { innerPadding ->
                AlertasScreen(
                    alertas = historial,
                    onBorrar = borrarHistorial,
                    modifier = Modifier.padding(innerPadding),
                    conectado = estado == EstadoConexion.CONECTADO
                )
            }
        }
        "PantallaErrorConexion" -> {
            PantallaErrorConexion(
                onReintentar = {
                    pantalla = "EstadoActual"
                    alPresionarConectar()
                },
                onVolver = { pantalla = "EstadoActual" }
            )
        }
    }

    // Ventana para elegir el módulo: emparejados (clásico) + búsqueda BLE
    if (mostrarDispositivos) {
        val emparejados = conexion.dispositivosEmparejados()
        val cerrar = {
            pararBusqueda()
            mostrarDispositivos = false
        }
        AlertDialog(
            onDismissRequest = cerrar,
            title = { Text("Elige el módulo Bluetooth") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                    // --- Bluetooth clásico (y BLE ya emparejados) ---
                    Text("Emparejados (HC-05 / HC-06)", fontWeight = FontWeight.Bold)
                    if (emparejados.isEmpty()) {
                        Text("No hay dispositivos emparejados. Para un HC-05 / HC-06, " +
                            "emparéjalo en Ajustes > Bluetooth (clave 1234 o 0000).",
                            fontSize = 13.sp)
                    } else {
                        for (modulo in emparejados) {
                            TextButton(
                                onClick = { conectar(modulo) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (modulo.esBle) "${modulo.nombre} (BLE)" else modulo.nombre)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // --- Bluetooth LE (HM-10, AT-09, BT05...) ---
                    Text("Módulos BLE (HM-10 y similares)", fontWeight = FontWeight.Bold)
                    Text("Estos no se emparejan en Ajustes: se buscan desde aquí.",
                        fontSize = 13.sp)
                    OutlinedButton(
                        onClick = alPresionarBuscar,
                        enabled = !buscando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (buscando) "Buscando..." else "Buscar módulos BLE")
                    }
                    if (!buscando && modulosBle.isEmpty()) {
                        Text("Toca \"Buscar\" con el Arduino encendido cerca.", fontSize = 13.sp)
                    }
                    for (modulo in modulosBle) {
                        TextButton(
                            onClick = { conectar(modulo) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${modulo.nombre}  (${modulo.dispositivo.address})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = cerrar) { Text("Cancelar") }
            }
        )
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
