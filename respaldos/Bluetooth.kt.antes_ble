package com.example.sips

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

// Estados posibles de la conexión con el Arduino
enum class EstadoConexion { DESCONECTADO, CONECTANDO, CONECTADO }

// UUID estándar del "puerto serie por Bluetooth" (SPP), el que usan el HC-05 y el HC-06
private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

// Maneja la conexión Bluetooth clásica con el módulo HC-05 / HC-06
class ConexionBluetooth(private val context: Context) {

    private var socket: BluetoothSocket? = null

    // Adaptador Bluetooth del teléfono (null si el teléfono no tiene Bluetooth)
    fun adaptador(): BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    // Desde Android 12 se necesita el permiso BLUETOOTH_CONNECT para conectarse
    fun tienePermiso(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    // Lista de dispositivos ya emparejados en Ajustes > Bluetooth
    @SuppressLint("MissingPermission") // el permiso se revisa con tienePermiso()
    fun dispositivosEmparejados(): List<BluetoothDevice> {
        if (!tienePermiso()) return emptyList()
        return adaptador()?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun nombreDe(dispositivo: BluetoothDevice): String =
        if (tienePermiso()) dispositivo.name ?: dispositivo.address else dispositivo.address

    // Se conecta al módulo y se queda leyendo línea por línea.
    // Cada línea que llega se entrega a alRecibirLinea (en el hilo principal).
    // Termina con una excepción (IOException) si no se pudo conectar o si se corta la conexión.
    @SuppressLint("MissingPermission")
    suspend fun conectarYLeer(
        dispositivo: BluetoothDevice,
        alConectar: () -> Unit,
        alRecibirLinea: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) { // Bluetooth no se puede usar en el hilo principal
            if (!tienePermiso()) throw IOException("Sin permiso de Bluetooth")

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

    fun desconectar() {
        try {
            socket?.close()
        } catch (e: IOException) {
            // ya estaba cerrada, no importa
        }
        socket = null
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
