package com.datpug.thismeanswar.network

import io.reactivex.Observable

/**
 * Created by longvu on 14/09/2017.
 */
interface NetworkService {
    var isHosting: Boolean

    fun startHosting(): Boolean
    fun stopHosting(): Boolean
    fun getConnections(condition: () -> Boolean): Observable<Connection>
    fun getConnection(): Connection?
    fun closeConnections()
}