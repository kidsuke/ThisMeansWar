package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.utils.TimeUtils
import com.datpug.entity.Direction
import java.util.*

/**
 * Created by longv on 27-Sep-17.
 */

object PlayerController : ApplicationListener {

    private val logger = Logger(PlayerController::class.java.canonicalName)

    private lateinit var explosionAnim: Animation<TextureRegion>
    private val explosionSheetCols = 12
    private val explosionSheetRows = 1
    private val explosionSize = 380f
    private var explosionPos: List<Vector2> = listOf()
    private val explosionTime = 2.5f
    private var shouldRenderExplosion = false
    private val fps = 12f
    private var timePassed = 0f
    private var startTime = TimeUtils.millis()

    private lateinit var spriteBatch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer
    private val random: Random = Random()

    private val screenWidth by lazy { Gdx.graphics.width.toFloat() }
    private val screenHeight by lazy { Gdx.graphics.height.toFloat() }

    var playerAnswers: List<Direction> = listOf()
        private set
    private var allowAnswer = false

    private val totalHealth = 1000f
    private var playerHealth = 1000f
    private val healthBarWidth = 500f
    private val healthBarHeight = 50f
    private val healthBarOffset = 30f

    override fun create() {
        spriteBatch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        // Create explosion animation
        val textureRegions = TextureRegion.split(
            GameAssets.explosionTexture,
            GameAssets.explosionTexture.width.div(explosionSheetCols),
            GameAssets.explosionTexture.height.div(explosionSheetRows)
        )
        explosionAnim = Animation(1 / fps, Array(textureRegions.flatMap { it.toList() }.toTypedArray()))
        explosionAnim.playMode = Animation.PlayMode.LOOP

        // Listen to answers' result
        GameManager.addOnAnswerListener(object : GameManager.OnAnswerListener{
            override fun onCorrectAnswer() {
                // Reset answers
                playerAnswers = listOf()
            }

            override fun onWrongAnswer() {
                // Allow render explosion
                shouldRenderExplosion = true
                startTime = TimeUtils.millis()
                explosionPos = List(5, {
                    val posX = random.nextFloat() * screenWidth
                    val poxY = random.nextFloat() * screenHeight
                    Vector2(posX, poxY)
                })
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
        if (shouldRenderExplosion) {
            renderExplosion()
            val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
            if (timePassed >= explosionTime) {
                shouldRenderExplosion = false
            }
        }
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

    private fun renderExplosion() {
        timePassed += Gdx.graphics.deltaTime

        spriteBatch.begin()
        explosionPos.forEach { spriteBatch.draw(explosionAnim.getKeyFrame(timePassed), it.x, it.y, explosionSize, explosionSize) }
        spriteBatch.end()
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
