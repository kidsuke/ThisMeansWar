package com.datpug.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.Disposable

/**
 * Created by phocphoc on 08/10/2017.
 */
object InputProcessor: Disposable {
    private val multiplexer = InputMultiplexer()

    init {
        Gdx.input.inputProcessor = multiplexer
    }

    fun addProccessor(processor: InputProcessor) {
        multiplexer.addProcessor(processor)
    }

    override fun dispose() {
        multiplexer.clear()
    }
}