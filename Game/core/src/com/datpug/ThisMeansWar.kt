package com.datpug

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.utils.Logger
import com.datpug.entity.Monster
import com.datpug.entity.Puppy
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.instances
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import java.nio.file.Files.size
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration
import com.badlogic.gdx.physics.bullet.collision.btCylinderShape
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape
import com.badlogic.gdx.physics.bullet.collision.btConeShape
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btSphereShape
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseInterface
import com.badlogic.gdx.physics.bullet.collision.btDispatcher
import com.badlogic.gdx.physics.bullet.collision.btCollisionConfiguration
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.utils.ArrayMap
import com.badlogic.gdx.utils.Disposable


class ThisMeansWar(val arRenderer: ARRenderer): ApplicationAdapter() {

    private var score: Int = 0


    private val logger = Logger("TESTING")

    private lateinit var spriteBatch: SpriteBatch
    private lateinit var scoreText: BitmapFont

    private lateinit var environment: Environment
    private lateinit var perspectiveCamera: PerspectiveCamera
    private lateinit var model: Model
    private lateinit var monster: Monster
    private lateinit var modelBatch: ModelBatch

    private lateinit var puppyController: PuppyController
    private var monsters: Map<Int, Monster> = mapOf()

    inner class MyContactListener : ContactListener() {
        override fun onContactAdded(colObj0: btCollisionObject?, partId0: Int, index0: Int, colObj1: btCollisionObject?, partId1: Int, index1: Int): Boolean {
            logger.error("COLLIDEEEEEEEEEEEEEEEEEE")
            return true
        }
    }

    private lateinit var listener: MyContactListener

    private var arDetectListener: ARRenderer.OnARDetectListener = object : ARRenderer.OnARDetectListener {
        override fun onARDetected(id: Int, data: FloatArray, fieldOfView: Float) {
            // Update camera so that it syncs with the AR camera
//            perspectiveCamera.position.set(data[12], data[13], data[14])
//            perspectiveCamera.up.set(data[4], data[5], data[6])
//            perspectiveCamera.direction.set(data[8], data[9], data[10])
//            perspectiveCamera.fieldOfView = fieldOfView
//            perspectiveCamera.update()

            // Add new monster if there isn't any for the id
            if (!monsters.keys.contains(id)) {
                val newMonster = Monster(model)
                newMonster.body.collisionShape = btBoxShape(Vector3(5f, 5f, 5f)) as btCollisionShape
                monsters = monsters.plus(Pair(id, newMonster))
                CollisionWorld.instance.addCollisionObject(newMonster.body)
            }
            // Find monster with current id and render it
            val monster: Monster? = monsters[id]
            if (monster != null) {
                modelBatch.begin(perspectiveCamera)
                modelBatch.render(monster, environment)
                modelBatch.end()
            }
        }
    }

    override fun create() {
        // Initialize Bullet, this must be done before using any class of this lib
        Bullet.init()

        // Initialize CollisionWord which helps detect collision
        CollisionWorld.init()
        listener = MyContactListener()

        arRenderer.initRendering(Gdx.graphics.width, Gdx.graphics.height)
        arRenderer.addOnARRenderListener(arDetectListener)

        perspectiveCamera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        perspectiveCamera.apply {
            position.set(10f, 10f, 10f)
            lookAt(0f, 0f, 0f)
            near = 10f
            far = 5000f
            update()
        }

        environment = Environment()
        environment.apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
        }

        puppyController = PuppyController(perspectiveCamera)
        puppyController.create()

        spriteBatch = SpriteBatch()
        scoreText = BitmapFont()
        scoreText.color = Color.CORAL
        scoreText.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        scoreText.data.scale(6f)

        modelBatch = ModelBatch()
        model = ModelBuilder().createBox(5f, 5f, 5f, Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position.or(Usage.Normal).toLong())
        monster = Monster(model)
    }

    override fun render() {
        // Detect collision
        CollisionWorld.instance.performDiscreteCollisionDetection()

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        arRenderer.render()

        fireIfDetectsMonster()
        removeDeadMonster()
        puppyController.render()

        spriteBatch.begin()
        scoreText.draw(spriteBatch, "Score: $score", Gdx.graphics.width.toFloat(), Gdx.graphics.width.toFloat())
        spriteBatch.end()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        arRenderer.resize(width, height)
    }

    private fun fireIfDetectsMonster() {
        val ray: Ray = perspectiveCamera.getPickRay(Gdx.graphics.width.toFloat()/2, Gdx.graphics.height.toFloat()/2)
        val monster: Monster? = monsters.values.find { Intersector.intersectRayBoundsFast(ray, it.boundingBox) && !it.isDead }

        if (monster != null) {
            if (!puppyController.isFiring) puppyController.startFire(monster)
        } else {
            if (puppyController.isFiring) puppyController.stopFire()
        }
    }

    private fun removeDeadMonster() {
        monsters =
            monsters.onEach {
                if (it.value.isDead) {
                    score += it.value.score
                    arRenderer.removeAR(it.key)
                }
            }.filterNot { it.value.isDead }
    }

    override fun dispose() {
        modelBatch.dispose()
        model.dispose()
        spriteBatch.dispose()
        arRenderer.removeOnARRenderListener(arDetectListener)
        CollisionWorld.dispose()
        listener.dispose()
    }

}
