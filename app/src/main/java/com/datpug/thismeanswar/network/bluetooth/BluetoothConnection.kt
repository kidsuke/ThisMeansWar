package com.datpug.thismeanswar.network.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.datpug.thismeanswar.network.Connection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by longvu on 15/09/2017.
 */
class BluetoothConnection(val socket: BluetoothSocket): Connection {
    override val connectionId: String
        get() = socket.remoteDevice.address
    override val inputStream: InputStream
        get() = socket.inputStream
    override val outputStream: OutputStream
        get() = socket.outputStream

    override fun disconnect() {
        try {
            socket.close()
        } catch (ioe: IOException) {
            Log.e(BluetoothService::class.qualifiedName, "Could not close bluetooth socket", ioe)
        }
    }
}