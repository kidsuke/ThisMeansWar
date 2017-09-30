package com.datpug

import com.badlogic.gdx.physics.bullet.collision.*

/**
 * Created by longv on 29-Sep-17.
 */
object CollisionWorld {
    val instance: btCollisionWorld
        get() {
            if (collisionWorld == null) {
                throw IllegalStateException("Initialize CollisionWorld before using")
            }
            return collisionWorld!!
        }

    private var collisionWorld: btCollisionWorld? = null
    private var collisionConfig: btDefaultCollisionConfiguration? = null
    private var dispatcher: btCollisionDispatcher? = null
    private var broadPhase: btDbvtBroadphase? = null

    fun init() {
        collisionConfig = btDefaultCollisionConfiguration()
        dispatcher = btCollisionDispatcher(collisionConfig)
        broadPhase = btDbvtBroadphase()
        collisionWorld = btCollisionWorld(dispatcher, broadPhase, collisionConfig)
    }

    fun dispose() {
        collisionWorld?.dispose()
        broadPhase?.dispose()
        dispatcher?.dispose()
        collisionConfig?.dispose()
    }

}