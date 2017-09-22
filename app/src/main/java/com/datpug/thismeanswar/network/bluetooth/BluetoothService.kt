package com.datpug.thismeanswar.network.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.datpug.thismeanswar.network.Connection
import com.datpug.thismeanswar.network.NetworkService
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.io.IOException
import java.util.*
import javax.inject.Inject

/**
 * Created by longvu on 14/09/2017.
 */
class BluetoothService @Inject constructor(val context: Context): NetworkService {
    companion object {
        val uuid = UUID.fromString("f6f9609b-5429-42bf-ac9f-69b541601b1c")!!
    }

    @Volatile override var isHosting: Boolean = false


    val bluetoothSupported: Boolean
        get() = btAdapter != null
    val bluetoothEnabled: Boolean
        get() = bluetoothSupported && btAdapter!!.isEnabled

    private val btAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }
    private var btServerSocket: BluetoothServerSocket? = null
    private var btConnections: List<BluetoothConnection> = listOf()

    override fun startHosting(connectionEstablished: (Connection) -> Unit): Boolean {
        return if (!isHosting) {
            // If there is no hosting yet, start hosting by opening bluetooth server socket
            try {
                btServerSocket = btAdapter?.listenUsingInsecureRfcommWithServiceRecord("", uuid)
                isHosting = true
                listenToBluetoothServerSocket(connectionEstablished)
                true // Indicate start hosting successfully
            } catch (ioe: IOException) {
                Log.e(BluetoothService::class.qualifiedName, "Could not open bluetooth server socket", ioe)
                false // Indicate fail to start hosting
            }
        } else {
            // Else return isHosting to prevent hosting twice
            isHosting
        }
    }

    private fun listenToBluetoothServerSocket(connectionEstablished: (Connection) -> Unit) {
        Thread(Runnable {
            var socket: BluetoothSocket? = null
            while (isHosting) {
                try {
                    socket = btServerSocket?.accept()
                } catch (ioe: IOException) {
                    Log.e(BluetoothService::class.qualifiedName, "Failed to accept bluetooth socket", ioe)
                }

                if (socket != null) {
                    val connection = BluetoothConnection(socket)
                    btConnections = btConnections.plus(connection)
                    connectionEstablished(connection)
                }
            }
        }).start()
    }

    override fun stopHosting(): Boolean {
        return if (isHosting) {
            // If there is already a hosting, stop hosting by closing bluetooth server socket
            try {
                btServerSocket?.close()
                isHosting = false
                true // Indicate stop hosting successfully
            } catch (ioe: IOException) {
                Log.e(BluetoothService::class.qualifiedName, "Could not close bluetooth server socket", ioe)
                false // Indicate fail to stop hosting
            }
        } else {
            // Else return isHosting to prevent closing twice
            !isHosting
        }
    }

    override fun getConnections(condition: () -> Boolean): Observable<Connection> {
        return Observable.create<Connection> {
            emitter: ObservableEmitter<Connection> ->
            try {
                var socket: BluetoothSocket?
                while (condition() && isHosting) {
                    try {
                        socket = btServerSocket?.accept()
                    } catch (ioe: IOException) {
                        Log.e(BluetoothService::class.qualifiedName, "Failed to accept bluetooth socket", ioe)
                        break
                    }

                    if (socket != null) {
                        val connection = BluetoothConnection(socket)
                        btConnections = btConnections.plus(connection)
                        emitter.onNext(connection)
                    }
                }
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    override fun getConnection(): Connection? {
        var socket: BluetoothSocket? = null
        try {
            socket = btServerSocket?.accept()
        } catch (ioe: IOException) {
            Log.e(BluetoothService::class.qualifiedName, "Failed to accept bluetooth socket", ioe)
        }

        return if (socket != null) {
            val connection = BluetoothConnection(socket)
            btConnections = btConnections.plus(connection)
            connection
        } else {
            null
        }
    }

    override fun closeConnections() {
        try {
            btConnections.forEach { it.socket.close() }
            btConnections = listOf()
        } catch (ioe: IOException) {
            Log.e(BluetoothService::class.qualifiedName, "Could not close bluetooth socket", ioe)
        }
    }

}