package com.datpug.thismeanswar.multiplayer

import com.datpug.thismeanswar.model.Player
import io.reactivex.Observable

/**
 * Created by longvu on 14/09/2017.
 */
interface MultiplayerService {
    val totalPlayers: Int

    fun startHostingGame(): Boolean
    fun stopHostingGame(): Boolean
    fun startFindingGame()
    fun stopFindingGame()
    fun waitingForPlayers()
    fun getConnectedPlayer(): Observable<Player>
    fun getDisconnectedPlayer(): Observable<Player>
}