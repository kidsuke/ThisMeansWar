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
    override val isHostingGame: Boolean
        get() = networkService.isHosting
    var playerLeft: Int = totalPlayers
        private set

    private var connections: List<Connection> = listOf()
    private var streamBuffer: ByteArray? = null // Byte storage for the stream

    private val connectedPlayersObservable = Observable.create<Player> { emitter ->
        try {
            while (networkService.isHosting) {
                if (playerLeft > 0) {
                    val connection: Connection? = networkService.getConnection()

                    if (connection != null) {
                        // Add to the active connections
                        connections = connections.plus(connection)
                        // Notify subscribers this connection has been connected
                        emitter.onNext(Player(connection.connectionId))
                        // Update number of players left
                        playerLeft--
                    }
                }
            }
            emitter.onComplete()
        } catch (e: Exception) {
            Log.e(MultiplayerServiceImpl::class.qualifiedName, "Error when getting connected player", e)
            emitter.onError(e)
        }
    }

    private val disconnectedPlayersObservable = Observable.create<Player> { emitter ->
        try {
            while (networkService.isHosting) {
                connections.forEach {
                    if (!it.isConnected) {
                        // Remove from the active connections
                        connections = connections.filterNot { cnt -> cnt.connectionId == it.connectionId }
                        // Notify subscribers this connection has been connected
                        emitter.onNext(Player(it.connectionId))
                        // Update number of players left
                        playerLeft--
                    }
                }
            }
            emitter.onComplete()
        } catch (e: Exception) {
            Log.e(MultiplayerServiceImpl::class.qualifiedName, "Error when getting connected player", e)
            emitter.onError(e)
        }
    }

    override fun startHostingGame(): Boolean = networkService.startHosting()

    override fun stopHostingGame(): Boolean = networkService.stopHosting()

    override fun startFindingGame() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stopFindingGame() {
    }

    override fun getConnectedPlayer(): Observable<Player> = connectedPlayersObservable

    override fun getDisconnectedPlayer(): Observable<Player> = disconnectedPlayersObservable
}