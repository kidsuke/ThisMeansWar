package com.datpug.entity

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.math.Vector3

/**
 * Created by longv on 02-Oct-17.
 */
class Projectile(model: Model): GameObject(model) {
    val direction: Vector3 = Vector3()
}