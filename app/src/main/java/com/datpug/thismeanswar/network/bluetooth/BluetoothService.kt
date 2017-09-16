package com.datpug.thismeanswar.network.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
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
class BluetoothService @Inject constructor(bluetoothManager: BluetoothManager): NetworkService() {
    companion object {
        val uuid = UUID.fromString("")!!
    }

    val bluetoothSupported: Boolean
        get() = btAdapter != null
    val bluetoothEnabled: Boolean
        get() = bluetoothSupported && btAdapter!!.isEnabled

    private var btAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var btServerSocket: BluetoothServerSocket? = null
    private var btConnections: List<BluetoothConnection> = listOf()

    override fun startHosting() {
        // Open bluetooth server socket
        try {
            btServerSocket = btAdapter?.listenUsingInsecureRfcommWithServiceRecord("", uuid)
            isHosting = true
        } catch (ioe: IOException) {
            Log.e(BluetoothService::class.qualifiedName, "Could not open bluetooth server socket", ioe)
        }
    }

    override fun stopHosting() {
        // Close bluetooth server socket
        try {
            btServerSocket?.close()
            isHosting = false
        } catch (ioe: IOException) {
            Log.e(BluetoothService::class.qualifiedName, "Could not close bluetooth server socket", ioe)
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

    override fun closeConnections() {
        try {
            btConnections.forEach { it.socket.close() }
            btConnections = listOf()
        } catch (ioe: IOException) {
            Log.e(BluetoothService::class.qualifiedName, "Could not close bluetooth socket", ioe)
        }
    }

}