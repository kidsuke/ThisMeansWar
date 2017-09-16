package com.datpug.thismeanswar.multiplayer

import com.datpug.thismeanswar.model.Player
import io.reactivex.Observable

/**
 * Created by longvu on 14/09/2017.
 */
interface MultiplayerService {
    fun startHosting()
    fun stopHosting()
    fun startFindingMatch()
    fun stopFindingMatch()
    fun getPlayers(): Observable<Player>
    fun addPlayer(player: Player)
    fun removePlayer(player: Player)
}