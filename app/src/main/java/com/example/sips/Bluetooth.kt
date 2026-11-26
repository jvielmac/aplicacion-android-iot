package com.example.sips

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

// Estados posibles de la conexión con el Arduino
enum class EstadoConexion { DESCONECTADO, CONECTANDO, CONECTADO }

// Un módulo que se puede elegir en la lista
// esBle = true  -> Bluetooth Low Energy (HM-10, AT-09, BT05, HC-08...)
// esBle = false -> Bluetooth clásico (HC-05, HC-06)
data class ModuloBluetooth(
    val dispositivo: BluetoothDevice,
    val nombre: String,
    val esBle: Boolean
)

// UUID estándar del "puerto serie por Bluetooth" clásico (SPP): HC-05 / HC-06
private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

// UUIDs del "puerto serie" de los módulos BLE tipo HM-10 (servicio FFE0, característica FFE1)
private val UUID_SERVICIO_BLE: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
private val UUID_CARACTERISTICA_BLE: UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
// Descriptor estándar para activar las notificaciones de una característica BLE
private val UUID_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

// Marca interna para avisar que la conexión BLE quedó lista
private const val MARCA_CONECTADO = "\u0000CONECTADO"

// Maneja la conexión con el módulo: Bluetooth clásico o BLE
class ConexionBluetooth(private val context: Context) {

    private var socket: BluetoothSocket? = null   // conexión clásica
    private var gatt: BluetoothGatt? = null       // conexión BLE

    // Adaptador Bluetooth del teléfono (null si el teléfono no tiene Bluetooth)
    fun adaptador(): BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private fun permisoConcedido(permiso: String): Boolean =
        ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

    // Desde Android 12 se necesita BLUETOOTH_CONNECT para conectarse
    fun tienePermiso(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            permisoConcedido(Manifest.permission.BLUETOOTH_CONNECT)

    // Permisos para BUSCAR módulos BLE:
    // Android 12 o mayor: BLUETOOTH_SCAN | Android 11 o menor: ubicación
    fun permisosBusqueda(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun tienePermisoBusqueda(): Boolean = permisosBusqueda().all { permisoConcedido(it) }

    // Lista de dispositivos ya emparejados en Ajustes > Bluetooth
    @SuppressLint("MissingPermission") // el permiso se revisa con tienePermiso()
    fun dispositivosEmparejados(): List<ModuloBluetooth> {
        if (!tienePermiso()) return emptyList()
        val emparejados = adaptador()?.bondedDevices?.toList() ?: emptyList()
        return emparejados.map { d ->
            ModuloBluetooth(
                dispositivo = d,
                nombre = d.name ?: d.address,
                esBle = d.type == BluetoothDevice.DEVICE_TYPE_LE
            )
        }
    }

    // Busca módulos BLE cercanos. Cada uno que encuentra se entrega a alEncontrar.
    // Devuelve una función para detener la búsqueda.
    @SuppressLint("MissingPermission")
    fun buscarModulosBle(alEncontrar: (ModuloBluetooth) -> Unit): () -> Unit {
        if (!tienePermisoBusqueda()) return {}
        val escaner = adaptador()?.bluetoothLeScanner ?: return {}

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val nombre = result.scanRecord?.deviceName ?: result.device.name
                // Se ignoran los dispositivos sin nombre (relojes, audífonos, etc.)
                if (!nombre.isNullOrBlank()) {
                    alEncontrar(ModuloBluetooth(result.device, nombre, esBle = true))
                }
            }
        }
        escaner.startScan(callback)
        return {
            try {
                escaner.stopScan(callback)
            } catch (e: Exception) {
                // el Bluetooth se apagó mientras buscaba; no importa
            }
        }
    }

    // Se conecta al módulo y se queda leyendo línea por línea.
    // Cada línea que llega se entrega a alRecibirLinea (en el hilo principal).
    // Termina con una excepción si no se pudo conectar o si se corta la conexión.
    suspend fun conectarYLeer(
        modulo: ModuloBluetooth,
        alConectar: () -> Unit,
        alRecibirLinea: (String) -> Unit
    ) {
        if (!tienePermiso()) throw IOException("Sin permiso de Bluetooth")
        if (modulo.esBle) {
            conectarYLeerBle(modulo.dispositivo, alConectar, alRecibirLinea)
        } else {
            conectarYLeerClasico(modulo.dispositivo, alConectar, alRecibirLinea)
        }
    }

    // ---------- Bluetooth clásico (HC-05 / HC-06) ----------
    @SuppressLint("MissingPermission")
    private suspend fun conectarYLeerClasico(
        dispositivo: BluetoothDevice,
        alConectar: () -> Unit,
        alRecibirLinea: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) { // Bluetooth clásico no se puede usar en el hilo principal
            val nuevoSocket = dispositivo.createRfcommSocketToServiceRecord(UUID_SPP)
            socket = nuevoSocket
            nuevoSocket.connect() // si el módulo no responde, lanza IOException

            withContext(Dispatchers.Main) { alConectar() }

            val lector = nuevoSocket.inputStream.bufferedReader()
            while (true) {
                val linea = lector.readLine() ?: break // null = se cerró la conexión
                withContext(Dispatchers.Main) { alRecibirLinea(linea) }
            }
            throw IOException("Conexión cerrada")
        }
    }

    // ---------- Bluetooth LE (HM-10 y similares) ----------
    // En BLE los datos no llegan como un flujo continuo, sino en "paquetes" de hasta 20 bytes
    // (notificaciones). Por eso se juntan en un buffer hasta encontrar el salto de línea.
    @SuppressLint("MissingPermission")
    private suspend fun conectarYLeerBle(
        dispositivo: BluetoothDevice,
        alConectar: () -> Unit,
        alRecibirLinea: (String) -> Unit
    ) {
        val lineas = Channel<String>(Channel.UNLIMITED)
        val buffer = StringBuilder()

        // Junta los pedazos que llegan y separa las líneas completas
        fun procesar(bytes: ByteArray) {
            buffer.append(String(bytes, Charsets.UTF_8))
            while (true) {
                val fin = buffer.indexOf("\n")
                if (fin < 0) break
                val linea = buffer.substring(0, fin).trim()
                buffer.delete(0, fin + 1)
                if (linea.isNotEmpty()) lineas.trySend(linea)
            }
        }

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    g.discoverServices() // paso siguiente: buscar el servicio FFE0
                } else {
                    lineas.close(IOException("Conexión BLE cerrada (estado $status)"))
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                val caracteristica = buscarCaracteristica(g)
                if (caracteristica == null) {
                    lineas.close(IOException("El módulo no tiene un puerto serie BLE (FFE0/FFE1)"))
                    return
                }
                // Activar las notificaciones: así el módulo nos avisa cada vez que llegan datos
                g.setCharacteristicNotification(caracteristica, true)
                val descriptor = caracteristica.getDescriptor(UUID_CCCD)
                if (descriptor != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        g.writeDescriptor(descriptor)
                    }
                }
                lineas.trySend(MARCA_CONECTADO)
            }

            // Android 13 o mayor
            override fun onCharacteristicChanged(
                g: BluetoothGatt,
                c: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                procesar(value)
            }

            // Android 12 o menor
            @Deprecated("Usado solo en Android 12 o menor")
            override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
                @Suppress("DEPRECATION")
                procesar(c.value ?: return)
            }
        }

        val nuevoGatt = dispositivo.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        gatt = nuevoGatt
        try {
            // Se queda esperando líneas hasta que se corte la conexión
            for (linea in lineas) {
                if (linea == MARCA_CONECTADO) {
                    withContext(Dispatchers.Main) { alConectar() }
                } else {
                    withContext(Dispatchers.Main) { alRecibirLinea(linea) }
                }
            }
            throw IOException("Conexión cerrada")
        } finally {
            nuevoGatt.close()
            gatt = null
        }
    }

    // Busca la característica del puerto serie: primero FFE0/FFE1 (HM-10);
    // si no existe, la primera característica que pueda mandar notificaciones
    private fun buscarCaracteristica(g: BluetoothGatt): BluetoothGattCharacteristic? {
        g.getService(UUID_SERVICIO_BLE)?.getCharacteristic(UUID_CARACTERISTICA_BLE)?.let { return it }
        for (servicio in g.services) {
            for (c in servicio.characteristics) {
                if (c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) return c
            }
        }
        return null
    }

    @SuppressLint("MissingPermission")
    fun desconectar() {
        try {
            socket?.close()
        } catch (e: IOException) {
            // ya estaba cerrada, no importa
        }
        socket = null
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (e: Exception) {
            // ya estaba cerrada, no importa
        }
        gatt = null
    }
}

// Interpreta un mensaje del Arduino con el formato:  CO2:512;LUZ:300;TEMP:22
// Devuelve un mapa  {"CO2"=512, "LUZ"=300, "TEMP"=22}  o null si el mensaje viene dañado
fun interpretarMensaje(linea: String): Map<String, Int>? {
    val datos = mutableMapOf<String, Int>()
    for (parte in linea.trim().split(";")) {
        val claveValor = parte.split(":")
        if (claveValor.size != 2) continue
        val valor = claveValor[1].trim().toFloatOrNull() ?: continue
        datos[claveValor[0].trim().uppercase()] = Math.round(valor)
    }
    return if (datos.isEmpty()) null else datos
}
