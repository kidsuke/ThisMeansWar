package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.AnimationController
import com.badlogic.gdx.math.Vector3
import com.datpug.entity.Monster

/**
 * Created by longv on 30-Sep-17.
 */
object MonsterController: ApplicationListener {

    private lateinit var modelBatch: ModelBatch
    private lateinit var camera: PerspectiveCamera
    private lateinit var environment: Environment
    private var currentMonster: Monster? = null
    private var monsterAnimController: AnimationController? = null
    val isMonsterDead: Boolean
        get() = currentMonster?.isDead ?: true

    override fun create() {
        // Initialize batches
        modelBatch = ModelBatch()

        // Initialize camera
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.apply {
            position.set(10f, 10f, 10f)
            lookAt(0f, 0f, 0f)
            near = 10f
            far = 50000f
            update()
        }

        // Initialize environment
        environment = Environment()
        environment.apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        }

        // Listen to answers' result
        GameManager.addOnAnswerListener(object : GameManager.OnAnswerListener{
            override fun onCorrectAnswer() {
                // Animate Hit animation
                try {
                    monsterAnimController?.animate("Armature|Hit", 2, null, 1f)
                } catch (e: Exception) {
                    monsterAnimController?.animate("Armature|hit", 2, null, 1f)
                }
                // Health decrease
                currentMonster!!.takeDamage(100f)
                // Animate appropriate animation
                if (currentMonster!!.isDead) {
                    try {
                        monsterAnimController?.queue("Armature|Die", 1, 1f, null, 1f)
                    } catch (e: Exception) {
                        monsterAnimController?.queue("Armature|die", 1, 1f, null, 1f)
                    }
                    GameManager.moveToNextLevel()
                } else {
                    monsterAnimController?.queue("Armature|Idle", 3, 1f, null, 1f)
                    monsterAnimController?.queue("Armature|Walk", -1, 1f, null, 1f)
                }
            }

            override fun onWrongAnswer() {
                monsterAnimController?.animate("Armature|Attack", 1, null, 1f)
                monsterAnimController?.queue("Armature|Walk", -1, 1f, null, 1f)
            }
        })
    }

    override fun render() {
        when (GameManager.gameState) {
            GameManager.State.SEARCHING -> {}
            else -> {
                monsterAnimController?.update(Gdx.graphics.deltaTime)

                if (currentMonster != null) {
                    modelBatch.begin(camera)
                    modelBatch.render(currentMonster)
                    modelBatch.end()
                }
            }
        }
    }

    override fun pause() {}

    override fun resume() {}

    override fun resize(width: Int, height: Int) {}

    override fun dispose() {
        modelBatch.dispose()
    }

    fun generateMonster(modelViewProjection: FloatArray) {
        if (GameManager.gameState == GameManager.State.SEARCHING) {
            currentMonster = when (GameManager.currentLevel) {
                GameManager.Level.LEVEL_1 -> {
                    Monster(GameAssets.mageModel)
                }
                GameManager.Level.LEVEL_2 -> {
                    Monster(GameAssets.cerberusModel)
                }
                GameManager.Level.LEVEL_3 -> {
                    Monster(GameAssets.diabloModel)
                }
            }
            currentMonster!!.transform.setToWorld(
                Vector3(modelViewProjection[12], modelViewProjection[13], modelViewProjection[14]),
                Vector3(modelViewProjection[8], modelViewProjection[9], modelViewProjection[10]),
                Vector3(modelViewProjection[4], modelViewProjection[5], modelViewProjection[6])
            )
            // Setup animation controller for monster. Pose is the starting anim
            monsterAnimController = AnimationController(currentMonster)
            monsterAnimController!!.setAnimation("Armature|Idle", -1)
            // But first has a chat first lol
            monsterAnimController!!.animate("Armature|Walk", -1, null, 1f)
            GameManager.startChallenges()
        } else {
            currentMonster!!.transform.setToWorld(
                Vector3(modelViewProjection[12], modelViewProjection[13], modelViewProjection[14]),
                Vector3(modelViewProjection[8], modelViewProjection[9], modelViewProjection[10]),
                Vector3(modelViewProjection[4], modelViewProjection[5], modelViewProjection[6])
            )
        }
    }

    fun setCameraProjection(cameraProjection: FloatArray) {
        camera.position.set(cameraProjection[12], cameraProjection[13], cameraProjection[14])
        camera.up.set(cameraProjection[4], cameraProjection[5], cameraProjection[6])
        camera.direction.set(cameraProjection[8], cameraProjection[9], cameraProjection[10])
        camera.update()
    }

}