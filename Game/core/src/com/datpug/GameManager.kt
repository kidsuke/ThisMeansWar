package com.datpug

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import com.datpug.entity.Direction

/**
 * Created by longv on 02-Oct-17.
 */
object GameManager: Disposable {
    enum class Level { LEVEL_1, LEVEL_2, LEVEL_3 }
    enum class Stage { STAGE_1, STAGE_2, STAGE_3 }

    var currentLevel: Level = Level.LEVEL_1
    var currentStage: Stage = Stage.STAGE_1
    var currentChallenges: Map<Stage, List<Direction>> = mapOf()
    var answerListeners: List<OnAnswerListener> = listOf()

    var isSearchingForMonster: Boolean = true
    val showChallenge: Boolean
        get() = challengeStarted && TimeUtils.timeSinceMillis(startTime) >= showChallengeTime


    private var shouldResetTime: Boolean = true

    var challengeStarted: Boolean = false
        private set
    var startTime = TimeUtils.millis()
    var showChallengeTime = 10f
    var answerChallengeTime = 5f
    var stageTransitionTime = 5f

    var step = 1

    private lateinit var spriteBatch: SpriteBatch


    fun init() {
        spriteBatch = SpriteBatch()
    }

    fun update() {
        if (challengeStarted) {
            when (step) {
                1 -> {
                    // STEP 1
                    // Show challenge to player for a certain time
                    if (shouldResetTime) {
                        startTime = TimeUtils.millis()
                        shouldResetTime = false
                    }
                    val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
                    if (timePassed <= showChallengeTime) {
                        renderChallenge()
                    } else {
                        shouldResetTime = true
                        step = 2
                    }
                }
                2 -> {
                    // STEP 2
                    // Hide challenge, give an amount of time for the player to answer
                    if (shouldResetTime) {
                        startTime = TimeUtils.millis()
                        shouldResetTime = false
                        PlayerController.startAnswer = true
                    }
                    val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
                    if (timePassed > answerChallengeTime) {
                        PlayerController.startAnswer = false
                        step = 3
                    }
                }
                3 -> {
                    // STEP 3
                    // Check player's answers
                    checkAnswers(PlayerController.playerAnswers)
                    shouldResetTime = true
                    step = 4
                }
                4 -> {
                    // STEP 4
                    // Start next stage in [stageTransitionTime] seconds
                    if (shouldResetTime) {
                        startTime = TimeUtils.millis()
                        shouldResetTime = false
                    }
                    val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
                    if (timePassed >= stageTransitionTime) {
                        // If there is another stage, returns to step 1, else ends this challenge
                        if (startNextStage()) step = 1
                        else challengeStarted = false
                    }
                }
            }
        }
    }

    override fun dispose() { answerListeners = listOf() }

    private fun startNextStage(): Boolean {
        var result = true

        currentStage = when (currentStage) {
            Stage.STAGE_1 -> Stage.STAGE_2
            Stage.STAGE_2 -> Stage.STAGE_3
            else -> {
                result = false
                Stage.STAGE_1
            }
        }

        if (result) setupNextChallenges()

        return result
    }

    fun startCurrentLevel() {
        setupNextChallenges()
        challengeStarted = true
    }

    fun startNextLevel(): Boolean {
        var result = true

        currentLevel = when (currentLevel) {
            Level.LEVEL_1 -> Level.LEVEL_2
            Level.LEVEL_2 -> Level.LEVEL_3
            else -> {
                result = false
                Level.LEVEL_1
            }
        }

        if (result) {
            setupNextChallenges()
            challengeStarted = true
        }

        return result
    }

    private fun setupNextChallenges() {
        val challenges = mutableMapOf<Stage, List<Direction>>()

        when (currentLevel) {
            Level.LEVEL_1 -> {
                // Stage 1
                challenges.put(
                    Stage.STAGE_1,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )
                // Stage 2
                challenges.put(
                    Stage.STAGE_2,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )
                // Stage 3
                challenges.put(
                    Stage.STAGE_3,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )
            }
            Level.LEVEL_2 -> {
                // Stage 1
                challenges.put(
                    Stage.STAGE_1,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )
                // Stage 2
                challenges.put(
                    Stage.STAGE_2,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )
                // Stage 3
                challenges.put(
                    Stage.STAGE_3,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )
            }
            Level.LEVEL_3 -> {
                // Stage 1
                challenges.put(
                    Stage.STAGE_1,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )
                // Stage 2
                challenges.put(
                    Stage.STAGE_2,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )
                // Stage 3
                challenges.put(
                    Stage.STAGE_3,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )
            }
        }

        currentChallenges = challenges
    }

    private fun renderChallenge() {
        val challenges = currentChallenges[currentStage] as List<Direction>
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

    fun addOnAnswerListener(listener: OnAnswerListener) {
        answerListeners = answerListeners.plus(listener)
    }

    private fun checkAnswers(answers: List<Direction>) {
        val directions = currentChallenges[currentStage] as List<Direction>
        var result = true

        directions.forEach {
            if (!answers.contains(it))
                result = false
        }
        if (result) answerListeners.forEach { it.onCorrectAnswer() }
        else answerListeners.forEach { it.onWrongAnswer() }
    }


    interface OnAnswerListener {
        fun onCorrectAnswer()
        fun onWrongAnswer()
    }

}