package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import com.datpug.entity.Direction

/**
 * Created by longv on 03-Oct-17.
 */

object ChallengeController: ApplicationListener {

    private lateinit var spriteBatch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer

    private lateinit var challengeTimeAnim: Animation<TextureRegion>
    private lateinit var playerTimeAnim: Animation<TextureRegion>
    private lateinit var trailAnim: Animation<TextureRegion>
    private val trailSheetCols = 12
    private val trailSheetRows = 1
    private val trailSize = 200f
    private val puppySize = 200f
    private var timePassed: Float = 0f
    private val fps = 12f

    override fun create() {
        spriteBatch = SpriteBatch()
        shapeRenderer = ShapeRenderer()

        // A dog running anim represent s time for challenge
        challengeTimeAnim = Animation(
            1 / fps, // Frame duration
            Array(GameAssets.dogRunningTextures.map { TextureRegion(it) }.toTypedArray()) // Textures for animation
        )
        challengeTimeAnim.playMode = Animation.PlayMode.LOOP

        // A cat running anim represents time for challenge
        playerTimeAnim = Animation(
            1 / fps, // Frame duration
            Array(GameAssets.catRunningTextures.map { TextureRegion(it) }.toTypedArray()) // Textures for animation
        )
        playerTimeAnim.keyFrames.forEach { it.flip(true, false) }
        playerTimeAnim.playMode = Animation.PlayMode.LOOP

        // Trail anim while puppies are running
        val textureRegions = TextureRegion.split(
            GameAssets.smokeTexture,
            GameAssets.smokeTexture.width.div(trailSheetCols),
            GameAssets.smokeTexture.height.div(trailSheetRows)
        )
        trailAnim = Animation(1 / fps, Array(textureRegions.flatMap { it.toList() }.toTypedArray()))
        trailAnim.playMode = Animation.PlayMode.LOOP
    }

    override fun render() {
        when (GameManager.gameState) {
            GameManager.State.CHALLENGING -> {
                renderChallengeTimeBar()
                renderChallenges()
            }
            GameManager.State.ANSWERING -> {
                renderPlayerTimeBar()
            }
            else -> {}
        }
    }

    override fun pause() {}

    override fun resume() {}

    override fun resize(width: Int, height: Int) {}

    override fun dispose() {
        spriteBatch.dispose()
    }

    private fun renderChallenges() {
        val challenges = GameManager.challenges
        var posX = 100f
        var posY = 100f

        spriteBatch.begin()
        challenges.forEach {
            val texture: Texture = when (it) {
                Direction.UP -> { GameAssets.arrowUpTexture }
                Direction.DOWN -> { GameAssets.arrowDownTexture }
                Direction.RIGHT -> { GameAssets.arrowRightTexture }
                Direction.LEFT -> { GameAssets.arrowLeftTexture }
            }
            spriteBatch.draw(texture, posX, posY, 150f, 150f)
            posX += 250f
        }
        spriteBatch.end()
    }

    private fun renderChallengeTimeBar() {
        timePassed += Gdx.graphics.deltaTime
        val spritePosX = Gdx.graphics.width.toFloat() * GameManager.timePassedRelative
        val spritePoxY = 250f

        spriteBatch.begin()
        spriteBatch.draw(trailAnim.getKeyFrame(timePassed), spritePosX - puppySize / 2, spritePoxY, trailSize, trailSize / 10)
        spriteBatch.draw(challengeTimeAnim.getKeyFrame(timePassed), spritePosX, spritePoxY, puppySize, puppySize)
        spriteBatch.end()
    }

    private fun renderPlayerTimeBar() {
        timePassed += Gdx.graphics.deltaTime
        val spritePosX = Gdx.graphics.width.toFloat() - puppySize - (Gdx.graphics.width.toFloat() * GameManager.timePassedRelative)
        val spritePoxY = 250f

        spriteBatch.begin()
        spriteBatch.draw(trailAnim.getKeyFrame(timePassed), spritePosX + puppySize / 2, spritePoxY, trailSize, trailSize / 10)
        spriteBatch.draw(playerTimeAnim.getKeyFrame(timePassed), spritePosX, spritePoxY, puppySize, puppySize)
        spriteBatch.end()
    }
}
