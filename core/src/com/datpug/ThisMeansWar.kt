package com.datpug

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

class ThisMeansWar(val arRenderer: ARRenderer): ApplicationAdapter() {

    private var arDetectListener: ARRenderer.OnARDetectListener = object : ARRenderer.OnARDetectListener {
        override fun onARDetected(id: Int, cameraProjection: FloatArray, modelViewProjection: FloatArray) {
            MonsterController.generateMonster(modelViewProjection)
            MonsterController.setCameraProjection(cameraProjection)
        }
    }

    override fun create() {
        // Initialize AR Renderer
        arRenderer.initRendering(Gdx.graphics.width, Gdx.graphics.height)
        arRenderer.addOnARRenderListener(arDetectListener)

        // Initialize controllers
        ChallengeController.create()
        PlayerController.create()
        MonsterController.create()

        // Load assets
        GameAssets.loadAssets()
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        arRenderer.render()
        GameManager.update()
        PlayerController.render()
        MonsterController.render()
        ChallengeController.render()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        arRenderer.resize(width, height)
    }

    override fun dispose() {
        PlayerController.dispose()
        MonsterController.dispose()
        ChallengeController.dispose()
        GameAssets.dispose()
        arRenderer.removeOnARRenderListener(arDetectListener)
    }

    fun setRemoteController(remoteController: RemoteController?) {
        PlayerController.setRemoteController(remoteController)
    }
}
