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
import com.badlogic.gdx.graphics.g3d.ModelInstance

class ThisMeansWar(val arRenderer: ARRenderer): ApplicationAdapter() {

    private lateinit var img: Texture
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var environment: Environment
    private lateinit var perspectiveCamera: PerspectiveCamera
    private lateinit var model: Model
    private lateinit var modelInstance: ModelInstance
    private lateinit var modelBatch: ModelBatch

    override fun create() {
        arRenderer.initRendering(Gdx.graphics.width, Gdx.graphics.height)

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

    }

    override fun render() {
        super.render()

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)

        val test: Array<FloatArray> = arRenderer.processFrame()

        if (test.isNotEmpty()) {
            modelBatch.begin(perspectiveCamera)
            model = ModelBuilder().createBox(test[0][12], test[0][13], test[0][14], Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position.or(Usage.Normal).toLong())
            modelInstance = ModelInstance(model)
            modelBatch.render(modelInstance, environment)
            modelBatch.end()
        }

        spriteBatch.begin()
        spriteBatch.draw(img, 0f, 0f)
        spriteBatch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        arRenderer.resize(width, height)
    }

//    private fun setProjectionAndCamera(test: Array<FloatArray>) {
//        for (tIdx in 0 until test.size) {
//            perspectiveCamera.position.set(data[12], data[13], data[14])
//            perspectiveCamera.up.set(data[4], data[5], data[6])
//            perspectiveCamera.direction.set(data[8], data[9], data[10])
//        }
//
//        if (trackables != null && trackables.isNotEmpty()) {
//            //transform all content
//            val trackable = trackables[0]
//
//            val modelViewMatrix = Tool.convertPose2GLMatrix(trackable.getPose())
//            val raw = modelViewMatrix.getData()
//
//            val rotated: FloatArray
//            //switch axis and rotate to compensate coordinates change
//            if (com.vuforia.Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON) {
//                // Front camera
//                rotated = floatArrayOf(raw[1], raw[0], raw[2], raw[3], raw[5], raw[4], raw[6], raw[7], raw[9], raw[8], raw[10], raw[11], raw[13], raw[12], raw[14], raw[15])
//            } else {
//                // Back camera
//                rotated = floatArrayOf(raw[1], -raw[0], raw[2], raw[3], raw[5], -raw[4], raw[6], raw[7], raw[9], -raw[8], raw[10], raw[11], raw[13], -raw[12], raw[14], raw[15])
//            }
//            val rot = Matrix44F()
//            rot.setData(rotated)
//            val inverse = SampleMath.Matrix44FInverse(rot)
//            val transp = SampleMath.Matrix44FTranspose(inverse)
//
//            val data = transp.getData()
//            perspectiveCamera.position.set(data[12], data[13], data[14])
//            perspectiveCamera.up.set(data[4], data[5], data[6])
//            perspectiveCamera.direction.set(data[8], data[9], data[10])
//
//        } else {
//            perspectiveCamera.position.set(100f, 100f, 100f)
//            perspectiveCamera.lookAt(1000f, 1000f, 1000f)
//        }
////
////        model.transform.set(Matrix4())
////        //the model is rotated
////        model.transform.rotate(1.0f, 0.0f, 0.0f, 90.0f)
////        model.transform.rotate(0.0f, 1.0f, 0.0f, 90.0f)
////        model.transform.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE)
//
//        perspectiveCamera.update()
//    }

    override fun dispose() {
        modelBatch.dispose()
        model.dispose()
        spriteBatch.dispose()
        img.dispose()
    }

}
