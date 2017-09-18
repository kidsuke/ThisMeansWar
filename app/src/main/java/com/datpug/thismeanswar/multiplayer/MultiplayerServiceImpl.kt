package com.datpug.thismeanswar.multiplayer

import android.util.Log
import com.datpug.thismeanswar.model.Player
import com.datpug.thismeanswar.network.Connection
import com.datpug.thismeanswar.network.NetworkService
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.IOException

/**
 * Created by longvu on 17/09/2017.
 */
class MultiplayerServiceImpl(private val networkService: NetworkService): MultiplayerService {
    override val totalPlayers: Int = 4
    var playerLeft: Int = totalPlayers
        private set

    private val connectedPlayersSubject: PublishSubject<Player> = PublishSubject.create()
    private val disconnectedPlayersSubject: PublishSubject<Player> = PublishSubject.create()
    private var connections: List<Connection> = listOf()
    private var streamBuffer: ByteArray? = null // Byte storage for the stream

    private val connectedPlayersObservable = Observable.create<Player> {  }

    override fun startHostingGame(): Boolean {
        if (networkService.startHosting()) {
            // Start waiting for players
            Thread(
                Runnable {
                    while (playerLeft > 0 && networkService.isHosting) {
                        val connection: Connection? = networkService.getConnection()

                        if (connection != null) {
                            addAndListenToConnection(connection)
                            // Notify subscribers this connection has been connected
                            connectedPlayersSubject.onNext(Player(connection.connectionId))
                            // Update number of players left
                            playerLeft--
                        }
                    }
                }
            ).start()
        }

        return networkService.isHosting
    }

    override fun stopHostingGame(): Boolean {
        networkService.stopHosting()
        return !networkService.isHosting
    }

    override fun startFindingGame() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stopFindingGame() {
    }

    override fun waitingForPlayers() {
        // Start waiting for players
        Thread(
            Runnable {
                while (playerLeft > 0) {
                    val connection: Connection? = networkService.getConnection()

                    if (connection != null) {
                        addAndListenToConnection(connection)
                        // Notify subscribers this connection has been connected
                        connectedPlayersSubject.onNext(Player(connection.connectionId))
                        // Update number of players left
                        playerLeft--
                    }
                }
            }
        ).start()
    }

    private fun addAndListenToConnection(connection: Connection) {
        // Keep track of the connection
        connections = connections.plus(connection)

        // Start listening to the connection
//        Thread(
//            Runnable {
//                streamBuffer = ByteArray(1024) // Buffer with appropriate size
//                var numBytes: Int // Bytes returned from read()
//
//                while (true) {
//                    try {
//                        numBytes = connection.inputStream.read(streamBuffer)
//                        if (numBytes > 0) {
//                            // OnNext(ByteArray)
//                        }
//                    } catch (ioe: IOException) {
//                        Log.d(MultiplayerServiceImpl::class.qualifiedName, "Connection has been disconnected", ioe)
//                        // Notify subscribers that this connection has been disconnected
//                        disconnectedPlayersSubject.onNext(Player(connection.connectionId))
//                        // Update number of players left
//                        playerLeft++
//                    }
//                }
//            }
//        ).start()
    }

    override fun getConnectedPlayer(): Observable<Player> {
        return Observable.create { emitter ->
            while (playerLeft > 0) {
                val connection: Connection? = networkService.getConnection()

                if (connection != null) {
                    addAndListenToConnection(connection)
                    // Notify subscribers this connection has been connected
                    connectedPlayersSubject.onNext(Player(connection.connectionId))
                    // Update number of players left
                    playerLeft--
                }
            }
        }
    }

    override fun getDisconnectedPlayer(): Observable<Player> = disconnectedPlayersSubject
}