package com.datpug.entity

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.utils.Disposable

/**
 * Created by longv on 26-Sep-17.
 */
open class GameObject(model: Model, x: Float, y: Float, z: Float): ModelInstance(model, x, y, z), Disposable {
    var center: Vector3 = Vector3()
    var dimensions: Vector3 = Vector3()
    val radius: Float
        get() = dimensions.len() / 2
    val boundingBox = BoundingBox()
    val body: btCollisionObject = btCollisionObject()
    var isDisposed: Boolean = false
        private set

    constructor(model: Model): this(model, 0f, 0f, 0f)

    constructor(model: Model, position: Vector3): this(model, position.x, position.y, position.z)

    init {
        calculateBoundingBox(boundingBox)
        boundingBox.getCenter(center)
        boundingBox.getDimensions(dimensions)
    }

    override final fun calculateBoundingBox(out: BoundingBox?): BoundingBox = super.calculateBoundingBox(out)

    override fun dispose() {
        body.dispose()
        isDisposed = true
    }
}