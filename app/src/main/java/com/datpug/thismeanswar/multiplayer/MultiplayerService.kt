package com.datpug.thismeanswar.multiplayer

import com.datpug.thismeanswar.model.Player
import io.reactivex.Observable

/**
 * Created by longvu on 14/09/2017.
 */
interface MultiplayerService {
    val totalPlayers: Int
    val isHostingGame: Boolean

    fun startHostingGame(): Boolean
    fun stopHostingGame(): Boolean
    fun startFindingGame()
    fun stopFindingGame()
    fun getPlayers(): Observable<List<Player>>
}