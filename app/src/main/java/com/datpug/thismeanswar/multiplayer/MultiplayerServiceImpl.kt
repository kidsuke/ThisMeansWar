package com.datpug.thismeanswar.multiplayer

import com.datpug.thismeanswar.model.Player
import com.datpug.thismeanswar.network.Connection
import com.datpug.thismeanswar.network.NetworkService
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.*

/**
 * Created by longvu on 17/09/2017.
 */
class MultiplayerServiceImpl(private val networkService: NetworkService): MultiplayerService {
    override val totalPlayers: Int = 4
    override val isHostingGame: Boolean
        get() = networkService.isHosting
    var playerLeft: Int = totalPlayers
        private set

    private var connections: List<Connection> = listOf()
    private var streamBuffer: ByteArray? = null // Byte storage for the stream

    private val connectedPlayersSubject = PublishSubject.create<Player>()
    private val disconnectedPlayersSubject = PublishSubject.create<Player>()
    private val playersSubject = PublishSubject.create<List<Player>>()
    private var clientThreads: List<ClientThread> = listOf()

    override fun startHostingGame(): Boolean = networkService.startHosting{
        addConnection(it)
        startListeningToConnection(it)
    }

    override fun stopHostingGame(): Boolean = networkService.stopHosting()

    override fun getPlayers(): Observable<List<Player>> = playersSubject

    private fun startListeningToConnection(connection: Connection) {
        val clientThread = ClientThread(connection)
        clientThread.start()
        clientThreads = clientThreads.plus(clientThread)
    }

    private fun addConnection(connection: Connection) {
        // Add to the active connections
        connections = connections.plus(connection)
        // Notify subscribers this connection has been connected
        playersSubject.onNext(connections.map { Player(it.connectionId) })
        // Update number of players left
        playerLeft++
    }

    private fun removeConnection(connection: Connection) {
        // Remove from the active connections
        connections = connections.filterNot { it.connectionId == connection.connectionId }
        // Notify subscribers this connection has been connected
        playersSubject.onNext(connections.map { Player(it.connectionId) })
        // Update number of players left
        playerLeft--
    }

    private inner class ClientThread(val connection: Connection): Thread() {
        val input: BufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
        val output: BufferedWriter = BufferedWriter(OutputStreamWriter(connection.outputStream))

        override fun run() {
            while (true) {
                try {
                    input.readLine()
                } catch (ioe: IOException) {
                    removeConnection(connection)
                }
            }
        }

        fun write() {
            try {
                output.write("yay")
            } catch (ioe: IOException) {
                removeConnection(connection)
            }
        }
    }

    override fun startFindingGame() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stopFindingGame() {
    }
}