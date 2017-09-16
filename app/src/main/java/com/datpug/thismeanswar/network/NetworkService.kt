package com.datpug.thismeanswar.network

import io.reactivex.Observable

/**
 * Created by longvu on 14/09/2017.
 */
abstract class NetworkService {
    var isHosting: Boolean = false
        protected set

    abstract fun startHosting()
    abstract fun stopHosting()
    abstract fun getConnections(condition: () -> Boolean): Observable<Connection>
    abstract fun closeConnections()
}