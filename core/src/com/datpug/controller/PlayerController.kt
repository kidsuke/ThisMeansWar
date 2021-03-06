package com.datpug.controller

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.utils.TimeUtils
import com.datpug.util.GameAssets
import com.datpug.GameManager
import com.datpug.util.InputProcessor
import com.datpug.entity.Direction
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import java.util.*

/**
 * Created by longv on 27-Sep-17.
 */

object PlayerController : ApplicationListener {

    private val logger = Logger(PlayerController::class.java.canonicalName)

    private lateinit var disposables: CompositeDisposable
    private val disposableTextures: MutableList<Texture> = mutableListOf()
    private var remoteController: RemoteController? = null
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
    val isPlayerAlive: Boolean
        get() = playerHealth > 0
    private val totalHealth = 1000f
    private var playerHealth = 1000f
    private val healthBarWidth = 500f
    private val healthBarHeight = 50f
    private val healthBarOffset = 30f
    private val answerSize = 150f
    private val answerVelocity = 10f
    private var currentAnswerPosX = screenWidth / 2 - answerSize / 2
    private var currentAnswerPosY = screenHeight / 2 - answerSize / 2
    private var currentAnswerAt = 0

    private var allowAnswer = false
    private var remoteControl = false

    override fun create() {
        spriteBatch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        disposables = CompositeDisposable()

        // Create explosion animation
        val textureRegions = TextureRegion.split(
                GameAssets.explosionTexture,
            GameAssets.explosionTexture.width.div(explosionSheetCols),
            GameAssets.explosionTexture.height.div(explosionSheetRows)
        )
        explosionAnim = Animation(1 / fps, Array(textureRegions.flatMap { it.toList() }.toTypedArray()))
        explosionAnim.playMode = Animation.PlayMode.LOOP

        // Listen to answers' result
        GameManager.addOnAnswerListener(object : GameManager.OnAnswerListener {
            override fun onCorrectAnswer() {
                // Reset answers and related fields
                playerAnswers = listOf()
                currentAnswerPosX = screenWidth / 2 - answerSize / 2
                currentAnswerPosY = screenHeight / 2 - answerSize / 2
                currentAnswerAt = 0
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
                // Reset answers and related fields
                playerAnswers = listOf()
                currentAnswerPosX = screenWidth / 2 - answerSize / 2
                currentAnswerPosY = screenHeight / 2 - answerSize / 2
                currentAnswerAt = 0
                // Health decrease
                playerHealth = MathUtils.clamp(playerHealth - GameManager.getDamage(), 0f, totalHealth)
            }
        })

        // Set listener for input processor
        InputProcessor.addProccessor(GestureDetector(GameGestureListener()))
    }

    override fun resize(width: Int, height: Int) {}

    override fun render() {
        allowAnswer = GameManager.gameState == GameManager.State.ANSWERING

        if (allowAnswer && playerAnswers.isNotEmpty()) {
            renderAnswers()
        }

        renderHealthBar()
        if (shouldRenderExplosion) {
            renderExplosion()
            val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
            if (timePassed >= explosionTime) {
                shouldRenderExplosion = false
            }
        }
    }

    override fun pause() {
        if (remoteControl) remoteController?.stopRemoteControl()
    }

    override fun resume() {
        if (remoteControl) remoteController?.startRemoteControl()
    }

    override fun dispose() {
        spriteBatch.dispose()
        shapeRenderer.dispose()
        disposables.dispose()
        disposableTextures.forEach { it.dispose() }
        disposableTextures.clear()
        if (remoteControl) remoteController?.stopRemoteControl()
    }

    fun healthBonus(bonus: Float) {
        playerHealth = MathUtils.clamp(playerHealth + bonus, 0f, totalHealth)
    }

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

    private fun renderAnswers() {
        spriteBatch.begin()
        val answer = playerAnswers[currentAnswerAt]
        when (answer) {
            Direction.UP -> {
                val texture: Texture = GameAssets.arrowUpTexture
                disposableTextures.add(texture)
                currentAnswerPosY += answerVelocity
                spriteBatch.draw(texture, currentAnswerPosX, currentAnswerPosY, answerSize, answerSize)
            }
            Direction.DOWN -> {
                val texture: Texture = GameAssets.arrowDownTexture
                disposableTextures.add(texture)
                currentAnswerPosY -= answerVelocity
                spriteBatch.draw(texture, currentAnswerPosX, currentAnswerPosY, answerSize, answerSize)
            }
            Direction.RIGHT -> {
                val texture: Texture = GameAssets.arrowRightTexture
                disposableTextures.add(texture)
                currentAnswerPosX += answerVelocity
                spriteBatch.draw(texture, currentAnswerPosX, currentAnswerPosY, answerSize, answerSize)
            }
            Direction.LEFT -> {
                val texture: Texture = GameAssets.arrowLeftTexture
                disposableTextures.add(texture)
                currentAnswerPosX -= answerVelocity
                spriteBatch.draw(texture, currentAnswerPosX, currentAnswerPosY, answerSize, answerSize)
            }
        }
        spriteBatch.end()
    }

    fun setRemoteController(remoteController: RemoteController?) {
        if (remoteController != null) {
            remoteControl = true
            PlayerController.remoteController = remoteController

            remoteController.getRemoteDirection()
            .subscribeBy {
                if (allowAnswer && remoteControl) {
                    playerAnswers = playerAnswers.plus(it)
                    currentAnswerAt = playerAnswers.size - 1
                    currentAnswerPosX = screenWidth / 2 - answerSize / 2
                    currentAnswerPosY = screenHeight / 2 - answerSize / 2
                }
            }
            .addTo(disposables)

            remoteController.startRemoteControl()
        }
    }

    class GameGestureListener: GestureDetector.GestureListener {
        override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
            if (allowAnswer && !remoteControl) {
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
                currentAnswerAt = playerAnswers.size - 1
                currentAnswerPosX = screenWidth / 2 - answerSize / 2
                currentAnswerPosY = screenHeight / 2 - answerSize / 2
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
