package com.datpug.entity

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance

/**
 * Created by longv on 26-Sep-17.
 */
class Monster(model: Model): GameObject(model) {
    var health: Float = 1000f
        private set

    val score: Int = 100

    var isVisible = false

    val isDead: Boolean
        get() = health <= 0

    fun takeDamage(damage: Float) {
        health -= damage
    }
}