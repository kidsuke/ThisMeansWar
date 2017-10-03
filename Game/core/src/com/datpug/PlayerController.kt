package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Logger
import com.datpug.entity.Direction

/**
 * Created by longv on 27-Sep-17.
 */

object PlayerController : ApplicationListener {

    private var logger = Logger(PlayerController::class.java.canonicalName)


    var playerAnswers: List<Direction> = listOf()
        private set
    private var allowAnswer = false

    private lateinit var shapeRenderer: ShapeRenderer

    private val screenWidth by lazy { Gdx.graphics.width.toFloat() }
    private val screenHeight by lazy { Gdx.graphics.height.toFloat() }

    private val totalHealth = 1000f
    private var playerHealth = 1000f
    private val healthBarWidth = 500f
    private val healthBarHeight = 50f
    private val healthBarOffset = 30f

    override fun create() {
        shapeRenderer = ShapeRenderer()

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
        allowAnswer = GameManager.gameState == GameManager.State.ANSWERING

        //when(GameManager.gameState) {
           renderHealthBar()

        //}
    }

    override fun pause() {}

    override fun resume() {}

    override fun dispose() {}

    private fun renderHealthBar() {
        val posX = screenWidth - healthBarWidth - healthBarOffset
        val posY = screenHeight - healthBarHeight - healthBarOffset

        // Draw health bar border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color.GREEN
        shapeRenderer.rect(posX, posY, healthBarWidth, healthBarHeight)
        shapeRenderer.end()

        // Draw player's health
        val currentHealthBarWidth = playerHealth / totalHealth * healthBarWidth
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.GREEN
        shapeRenderer.rect(posX, posY, currentHealthBarWidth, healthBarHeight)
        shapeRenderer.end()
    }

    class GameGestureListener: GestureDetector.GestureListener {
        override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
            if (allowAnswer) {
                playerAnswers = if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX > 0) {
                        logger.debug("RIGHT")
                        playerAnswers.plus(Direction.RIGHT)
                    }
                    else {
                        logger.debug("LEFT")
                        playerAnswers.plus(Direction.LEFT)
                    }
                } else {
                    if (velocityY > 0) {
                        logger.debug("DOWN")
                        playerAnswers.plus(Direction.DOWN)
                    }
                    else {
                        logger.debug("UP")
                        playerAnswers.plus(Direction.UP)
                    }
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
