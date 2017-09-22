package com.datpug

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight


class ThisMeansWar(val arRenderer: ARRenderer) : ApplicationAdapter() {

    private lateinit var img: Texture
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var environment: Environment
    private lateinit var perspectiveCamera: PerspectiveCamera
    private lateinit var model: Model
    private lateinit var modelInstance: ModelInstance
    private lateinit var modelBatch: ModelBatch

    override fun create() {
        perspectiveCamera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        perspectiveCamera.apply {
            position.set(10f, 10f, 10f)
            lookAt(0f, 0f, 0f)
            near = 1f
            far = 300f
            update()
        }

        environment = Environment()
        environment.apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        }
        modelBatch = ModelBatch()
        model = ModelBuilder().createBox(5f, 5f, 5f, Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position.or(Usage.Normal).toLong())
        modelInstance = ModelInstance(model)
        spriteBatch = SpriteBatch()
        img = Texture("badlogic.jpg")

        arRenderer.initRendering(Gdx.graphics.width, Gdx.graphics.height)
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        arRenderer.render()
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
//        //Gdx.gl.glClearColor(1f, 0f, 0f, 1f)
//
//        modelBatch.begin(perspectiveCamera)
//        modelBatch.render(modelInstance, environment)
//        modelBatch.end()

//        spriteBatch.begin()
//        spriteBatch.draw(img, 0f, 0f)
//        spriteBatch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        arRenderer.resize(width, height)
    }

    override fun dispose() {
        modelBatch.dispose()
        model.dispose()
        spriteBatch.dispose()
        img.dispose()
    }
}
