package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.datpug.entity.Direction

/**
 * Created by longv on 03-Oct-17.
 */

object ChallengeController: ApplicationListener {

    private lateinit var spriteBatch: SpriteBatch
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var fontGenerator: FreeTypeFontGenerator
    private lateinit var searchingFont: BitmapFont
    private val searchingText = "Searching for monsters..."
    private var searchingFontHeight = 0f
    private var searchingFontWidth = 0f
    private lateinit var gameOverFont: BitmapFont
    private var gameOverFontHeight = 0f
    private var gameOverFontWidth = 0f
    private lateinit var okButton: TextButton
    private lateinit var challengeTimeAnim: Animation<TextureRegion>
    private lateinit var playerTimeAnim: Animation<TextureRegion>
    private lateinit var trailAnim: Animation<TextureRegion>
    private val trailSheetCols = 12
    private val trailSheetRows = 1
    private val trailSize = 200f
    private val puppySize = 240f
    private var timePassed: Float = 0f
    private val fps = 12f
    private var winOrLose: String = "You Win"
    private val numOfChallengesPerRow = 4

    override fun create() {
        spriteBatch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        val glyphLayout = GlyphLayout()
        fontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/KBLuckyClover.ttf"))

        // FONTS
        val searchingFontParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        searchingFontParam.color = Color.CORAL
        searchingFontParam.size = 75
        searchingFont = fontGenerator.generateFont(searchingFontParam)
        glyphLayout.setText(searchingFont, searchingText)
        searchingFontWidth = glyphLayout.width
        searchingFontHeight = glyphLayout.height


        val gameOverFontParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        gameOverFontParam.color = Color.CORAL
        gameOverFontParam.size = 150
        gameOverFont = fontGenerator.generateFont(gameOverFontParam)
        glyphLayout.setText(gameOverFont, winOrLose)
        gameOverFontWidth = glyphLayout.width
        gameOverFontHeight = glyphLayout.height

        val okButtonFontParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        okButtonFontParam.color = Color.WHITE
        okButtonFontParam.size = 100
        val okButtonFont = fontGenerator.generateFont(okButtonFontParam)

        fontGenerator.dispose()

        // BUTTON
        val buttonStyle: TextButton.TextButtonStyle = TextButton.TextButtonStyle()
        buttonStyle.font = okButtonFont
        okButton = TextButton("OK", buttonStyle)
        okButton.x = Gdx.graphics.width.toFloat() / 2 - okButton.width / 2
        okButton.y = Gdx.graphics.height.toFloat() / 2 - okButton.height / 2 - gameOverFontHeight - 50f
        okButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.exit()
            }
        })
        val stage = Stage()
        stage.addActor(okButton)
        InputProcessor.addProccessor(stage)

        // ANIMATIONS
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
            GameManager.State.SEARCHING -> {
                renderSearchingText()
            }
            GameManager.State.CHALLENGING -> {
                renderChallengeTimeBar()
                renderChallenges()
            }
            GameManager.State.ANSWERING -> {
                renderPlayerTimeBar()
            }
            GameManager.State.WIN -> {
                winOrLose = "You Win"
                renderGameOver()
            }
            GameManager.State.LOSE -> {
                winOrLose = "You Lose"
                renderGameOver()
            }
            else -> {}
        }
    }

    override fun pause() {}

    override fun resume() {}

    override fun resize(width: Int, height: Int) {}

    override fun dispose() {
        spriteBatch.dispose()
        searchingFont.dispose()
        shapeRenderer.dispose()
    }

    private fun renderChallenges() {
        val challenges = GameManager.challenges
        var count = 0

        // Calculate appropriate position for challenges
        val offsetX = 60f
        val offsetY = 10f
        val size = Gdx.graphics.width.toFloat() / numOfChallengesPerRow - 2 * offsetX
        var posX = offsetX
        val numOfRows = MathUtils.ceil(challenges.size.toFloat() / numOfChallengesPerRow) - 1
        var posY = puppySize + (if (numOfRows > 0) 0f else offsetY) + numOfRows * (size + offsetY)

        spriteBatch.begin()
        challenges.forEach {
            count++
            val texture: Texture = when (it) {
                Direction.UP -> { GameAssets.arrowUpTexture }
                Direction.DOWN -> { GameAssets.arrowDownTexture }
                Direction.RIGHT -> { GameAssets.arrowRightTexture }
                Direction.LEFT -> { GameAssets.arrowLeftTexture }
            }
            spriteBatch.draw(texture, posX, posY, size, size)
            posX += (size + 2 * offsetX)

            if (count == numOfChallengesPerRow) {
                posX = offsetX
                posY -= (size + 2 * offsetY)
            }
        }
        spriteBatch.end()
    }

    private fun renderChallengeTimeBar() {
        timePassed += Gdx.graphics.deltaTime
        val spritePosX = Gdx.graphics.width.toFloat() * GameManager.timePassedRelative
        val spritePoxY = 0f

        spriteBatch.begin()
        spriteBatch.draw(trailAnim.getKeyFrame(timePassed), spritePosX - puppySize / 2, spritePoxY, trailSize, trailSize / 10)
        spriteBatch.draw(challengeTimeAnim.getKeyFrame(timePassed), spritePosX, spritePoxY, puppySize, puppySize)
        spriteBatch.end()
    }

    private fun renderPlayerTimeBar() {
        timePassed += Gdx.graphics.deltaTime
        val spritePosX = Gdx.graphics.width.toFloat() - puppySize - (Gdx.graphics.width.toFloat() * GameManager.timePassedRelative)
        val spritePoxY = 0f

        spriteBatch.begin()
        spriteBatch.draw(trailAnim.getKeyFrame(timePassed), spritePosX + puppySize / 2, spritePoxY, trailSize, trailSize / 10)
        spriteBatch.draw(playerTimeAnim.getKeyFrame(timePassed), spritePosX, spritePoxY, puppySize, puppySize)
        spriteBatch.end()
    }

    private fun renderSearchingText() {
        spriteBatch.begin()
        searchingFont.draw(spriteBatch, searchingText, Gdx.graphics.width.toFloat()/2 - searchingFontWidth / 2, Gdx.graphics.height.toFloat()/2 - searchingFontHeight / 2)
        spriteBatch.end()
    }

    private fun renderGameOver() {
        spriteBatch.begin()
        gameOverFont.draw(spriteBatch, winOrLose, Gdx.graphics.width.toFloat()/2 - gameOverFontWidth / 2, Gdx.graphics.height.toFloat()/1.8f)
        okButton.draw(spriteBatch, 1f)
        spriteBatch.end()
    }
}
