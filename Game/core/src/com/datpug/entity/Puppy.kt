package com.datpug.entity

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance

/**
 * Created by longv on 26-Sep-17.
 */
class Puppy(model: Model): GameObject(model) {
    var health: Int = 100
        private set
}