package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Logger
import com.datpug.entity.Direction
import com.datpug.entity.Puppy

/**
 * Created by longv on 27-Sep-17.
 */

object PuppyController: ApplicationListener {

    var playerHealth = 1000
        private set
    var playerAnswers: List<Direction> = listOf()
        private set
    private var allowAnswer = true

    override fun create() {
        // Listen to answers' result
        GameManager.addOnAnswerListener(object : GameManager.OnAnswerListener{
            override fun onCorrectAnswer() {
                // Reset answers
                playerAnswers = listOf()
            }

            override fun onWrongAnswer() {
                // Reset answers
                playerAnswers = listOf()
                // Health decrease
                playerHealth -= 100
            }
        })

        // Set listener for input processor
        Gdx.input.inputProcessor = GestureDetector(GameGestureListener())
    }

    override fun resize(width: Int, height: Int) {}

    override fun render() {

    }

    override fun pause() {}

    override fun resume() {}

    override fun dispose() {}

    fun startAnswer() {
        allowAnswer = true
    }

    fun stopAnswer() {
        allowAnswer = false
    }

    class GameGestureListener: GestureDetector.GestureListener {
        override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
            if (allowAnswer) {
                playerAnswers = if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX > 0) playerAnswers.plus(Direction.RIGHT)
                    else playerAnswers.plus(Direction.LEFT)
                } else {
                    if (velocityY > 0) playerAnswers.plus(Direction.DOWN)
                    else playerAnswers.plus(Direction.UP)
                }
            }

            return true
        }

        override fun zoom(initialDistance: Float, distance: Float): Boolean = false
        override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean = false
        override fun pinchStop() {}
        override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean  = false
        override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean = false
        override fun longPress(x: Float, y: Float): Boolean = false
        override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean = false
        override fun pinch(initialPointer1: Vector2?, initialPointer2: Vector2?, pointer1: Vector2?, pointer2: Vector2?): Boolean = false
    }

}
