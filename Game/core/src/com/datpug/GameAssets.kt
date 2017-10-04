package com.datpug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.utils.Disposable

/**
 * Created by longv on 30-Sep-17.
 */
object GameAssets: Disposable {

    val cerberusModel: Model by lazy { getModel(fileName = "mesh/cerberus.g3db") }
    val diabloModel: Model by lazy { getModel(fileName = "mesh/diablous.g3db") }
    val mageModel: Model by lazy { getModel(fileName = "mesh/mage.g3db") }
    val lionModel: Model by lazy { getModel(fileName = "mesh/lion.g3db") }
    val dinosaurModel: Model by lazy { getModel(fileName = "mesh/dinosaur.g3db") }
    val batModel: Model by lazy { getModel(fileName = "mesh/bat.g3db") }
    val bulletModel: Model by lazy { getModel(fileName = "mesh/bullet.g3db") }

    val arrowUpTexture: Texture by lazy { getTexture(fileName = "sprite/arrowUp.png") }
    val arrowDownTexture: Texture by lazy { getTexture(fileName = "sprite/arrowDown.png") }
    val arrowRightTexture: Texture by lazy { getTexture(fileName = "sprite/arrowRight.png") }
    val arrowLeftTexture: Texture by lazy { getTexture(fileName = "sprite/arrowLeft.png") }
    val explosionTexture: Texture by lazy { getTexture(fileName = "sprite/explosion.png") }
    val smokeTexture: Texture by lazy { getTexture(fileName = "sprite/smoke.png") }
    val dogRunningTextures: List<Texture> by lazy {
        val textures: MutableList<Texture> = mutableListOf()
        textures.add(getTexture(fileName = "sprite/dogRun_0.png"))
        textures.add(getTexture(fileName = "sprite/dogRun_1.png"))
        textures.add(getTexture(fileName = "sprite/dogRun_2.png"))
        textures.add(getTexture(fileName = "sprite/dogRun_3.png"))
        textures.add(getTexture(fileName = "sprite/dogRun_4.png"))
        textures.add(getTexture(fileName = "sprite/dogRun_5.png"))
        textures.add(getTexture(fileName = "sprite/dogRun_6.png"))
        textures.add(getTexture(fileName = "sprite/dogRun_7.png"))
        textures
    }
    val catRunningTextures: List<Texture> by lazy {
        val textures: MutableList<Texture> = mutableListOf()
        textures.add(getTexture(fileName = "sprite/catRun_0.png"))
        textures.add(getTexture(fileName = "sprite/catRun_1.png"))
        textures.add(getTexture(fileName = "sprite/catRun_2.png"))
        textures.add(getTexture(fileName = "sprite/catRun_3.png"))
        textures.add(getTexture(fileName = "sprite/catRun_4.png"))
        textures.add(getTexture(fileName = "sprite/catRun_5.png"))
        textures.add(getTexture(fileName = "sprite/catRun_6.png"))
        textures.add(getTexture(fileName = "sprite/catRun_7.png"))
        textures
    }

    private val assetManager: AssetManager = AssetManager()
    private val models: MutableList<Model> = mutableListOf()
    private val textures: MutableList<Texture> = mutableListOf()

    fun loadAssets() {
        assetManager.load("mesh/lion.g3db", Model::class.java)
        assetManager.load("mesh/bat.g3db", Model::class.java)
        assetManager.load("mesh/dinosaur.g3db", Model::class.java)
        assetManager.load("mesh/bullet.g3db", Model::class.java)
        assetManager.load("mesh/mage.g3db", Model::class.java)
        assetManager.load("mesh/diablous.g3db", Model::class.java)
        assetManager.load("mesh/cerberus.g3db", Model::class.java)

        assetManager.finishLoading()
    }

    private fun getModel(fileName: String): Model {
        val model: Model
        try {
            model = assetManager.get(fileName, Model::class.java)
            models.add(model)
        } catch (e: Exception) {
            throw IllegalStateException("Model [$fileName] has not been loaded")
        }
        return model
    }

    private fun getTexture(fileName: String): Texture {
        val texture: Texture
        try {
            texture = Texture(Gdx.files.internal(fileName))
            textures.add(texture)
        } catch (e: Exception) {
            throw IllegalStateException("Texture [$fileName] cannot be found")
        }
        return texture
    }

    override fun dispose() {
        models.forEach { it.dispose() }
        textures.forEach { it.dispose() }
    }
}