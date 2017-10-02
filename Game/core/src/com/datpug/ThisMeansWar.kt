package com.datpug

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.utils.Logger
import com.datpug.entity.Monster
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.TimeUtils


class ThisMeansWar(val arRenderer: ARRenderer): ApplicationAdapter() {

    private var score: Int = 0
    private val scoreTextScale = 5f
    private val scoreTextOffset = 15f

    private val searchingTextScale = 5f


    private val healthBarWidth = 500f
    private val healthBarHeight = 50f
    private val healthBarOffset = 30f

    private val screenWidth by lazy { Gdx.graphics.width.toFloat() }
    private val screenHeight by lazy { Gdx.graphics.height.toFloat() }

    private lateinit var spriteBatch: SpriteBatch
    private lateinit var scoreText: BitmapFont
    private lateinit var shapeRenderer: ShapeRenderer

    private lateinit var environment: Environment
    private lateinit var perspectiveCamera: PerspectiveCamera
    private lateinit var model: Model
    private lateinit var modelBatch: ModelBatch

    private var arDetectListener: ARRenderer.OnARDetectListener = object : ARRenderer.OnARDetectListener {
        override fun onARDetected(id: Int, cameraProjection: FloatArray, modelViewProjection: FloatArray) {
            MonsterController.generateMonster(modelViewProjection)
            MonsterController.setCameraProjection(cameraProjection)
        }
    }

    override fun create() {
        // Initialize Bullet, this must be done before using any class of this lib
        Bullet.init()

        // Initialize AR Renderer
        arRenderer.initRendering(screenWidth.toInt(), screenHeight.toInt())
        arRenderer.addOnARRenderListener(arDetectListener)

        perspectiveCamera = PerspectiveCamera(67f, screenWidth, screenHeight)
        perspectiveCamera.apply {
            position.set(0f, 0f, 0f)
            lookAt(0f, 0f, 1f)
            near = 1f
            far = 5000f
            update()
        }

        environment = Environment()
        environment.apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        }

        GameManager.init()
        PlayerController.create()
        MonsterController.create()

        shapeRenderer = ShapeRenderer()

        spriteBatch = SpriteBatch()
        scoreText = BitmapFont()
        scoreText.color = Color.CORAL
        scoreText.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        scoreText.data.scale(scoreTextScale)

        // Load assets
        GameAssets.loadAssets()
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        arRenderer.render()
        GameManager.update()
        PlayerController.render()
        MonsterController.render()

        if (GameManager.isSearchingForMonster) {
            renderSearchingText()
        } else {
            renderHealthBar()
            renderScoreText()
        }
//
//        if (GameManager.showChallenge) {
//            renderChallenge()
//        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        arRenderer.resize(width, height)
    }

    private fun renderHealthBar() {
        val posX = screenWidth - healthBarWidth - healthBarOffset
        val posY = screenHeight - healthBarHeight - healthBarOffset

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = Color.GREEN
        shapeRenderer.rect(posX, posY, healthBarWidth, healthBarHeight)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.GREEN
        shapeRenderer.rect(posX, posY, healthBarWidth - 40, healthBarHeight)
        shapeRenderer.end()
    }

    private fun renderScoreText() {
        spriteBatch.begin()
        scoreText.draw(spriteBatch, "Score: $score", scoreTextOffset, screenHeight - scoreTextOffset)
        spriteBatch.end()
    }

    private fun renderSearchingText() {
        spriteBatch.begin()
        scoreText.draw(spriteBatch, "Searching for monsters...", screenWidth/2, screenHeight/2)
        spriteBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        model.dispose()
        spriteBatch.dispose()
        arRenderer.removeOnARRenderListener(arDetectListener)
    }
}
