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
import com.badlogic.gdx.utils.Logger
import com.datpug.entity.Monster
import com.datpug.entity.Puppy

/**
 * Created by longv on 30-Sep-17.
 */
object MonsterController: ApplicationListener {
    private val logger = Logger("TESTING")

    private lateinit var modelBatch: ModelBatch
    private lateinit var camera: PerspectiveCamera
    private lateinit var environment: Environment

    private var currentMonster: Monster? = null
    private var monsterAnimController: AnimationController? = null

    private var animationControllers: List<AnimationController> = listOf()

    var monsterDeadListener: OnMonsterDeadListener? = null
    var monsters: Map<Int, Monster> = mapOf()
        private set

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
                monsterAnimController?.animate("Armature|Hit", 2, null, 1f)
                // Health decrease
                currentMonster!!.takeDamage(100f)
                // Animate appropriate animation
                if (currentMonster!!.isDead) {
                    monsterAnimController?.queue("Armature|Idle", -1, 1f, null, 1f)
                    GameManager.startNextLevel()
                } else {
                    monsterAnimController?.queue("Armature|Idle", 3, 1f, null, 1f)
                    monsterAnimController?.queue("Armature|Walk", -1, 1f, null, 1f)
                }
            }

            override fun onWrongAnswer() {
                monsterAnimController?.animate("Armature|Attack", 2, null, 1f)
                monsterAnimController?.queue("Armature|Walk", -1, 1f, null, 1f)
            }
        })
    }

    override fun render() {
        monsterAnimController?.update(Gdx.graphics.deltaTime * 0.75f)

        if (currentMonster != null) {
            modelBatch.begin(camera)
            modelBatch.render(currentMonster)
            modelBatch.end()
        }
    }

    override fun pause() {}

    override fun resume() {}

    override fun resize(width: Int, height: Int) {}

    override fun dispose() {
        // Dispose all monsters if there is still any left
        monsters.forEach {
            // Remove the game object from the collision world
            CollisionWorld.instance.removeCollisionObject(it.value.body)
            // Dispose the game object
            it.value.dispose()
        }
        monsters = mapOf()
    }

    fun generateMonster(data: FloatArray) {
        if (GameManager.isSearchingForMonster) {
            currentMonster = when (GameManager.currentLevel) {
                GameManager.Level.LEVEL_1 -> {
                    Monster(GameAssets.archerModel)
                }
                GameManager.Level.LEVEL_2 -> {
                    Monster(GameAssets.cerberusModel)
                }
                GameManager.Level.LEVEL_3 -> {
                    Monster(GameAssets.diabloModel)
                }
            }
            currentMonster!!.transform.setToWorld(
                    Vector3(data[12], data[13], data[14]),
                    Vector3(data[8], data[9], data[10]),
                    Vector3(data[4], data[5], data[6])
            )
            // Setup animation controller for monster. Pose is the starting anim
            monsterAnimController = AnimationController(currentMonster)
            monsterAnimController!!.setAnimation("Armature|Pose", -1)
            // Stop searching for monster and deal with this one...
            GameManager.isSearchingForMonster = false
            // But first has a chat first lol
            monsterAnimController!!.animate("Armature|Walk", -1, null, 1f)
            GameManager.startCurrentLevel()
        } else {
            currentMonster!!.transform.setToWorld(
                Vector3(data[12], data[13], data[14]),
                Vector3(data[8], data[9], data[10]),
                Vector3(data[4], data[5], data[6])
            )
        }
    }

    fun setCameraProjection(data: FloatArray) {
        camera.position.set(data[12], data[13], data[14])
        camera.up.set(data[4], data[5], data[6])
        camera.direction.set(data[8], data[9], data[10])
        camera.update()
    }

    interface OnMonsterDeadListener {
        fun onMonsterDead(monster: Pair<Int, Monster>)
    }
}