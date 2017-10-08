package com.datpug.controller

import com.datpug.entity.Direction
import io.reactivex.Observable

/**
 * Created by phocphoc on 08/10/2017.
 */
interface RemoteController {
    fun startRemoteControl()
    fun stopRemoteControl()
    fun getRemoteDirection(): Observable<Direction>
}