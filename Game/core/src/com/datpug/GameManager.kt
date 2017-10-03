package com.datpug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import com.datpug.entity.Direction

/**
 * Created by longv on 02-Oct-17.
 */
object GameManager: Disposable {
    enum class Level { LEVEL_1, LEVEL_2, LEVEL_3 }
    enum class Stage { STAGE_1, STAGE_2, STAGE_3 }
    enum class State { IDLE, CHALLENGING, ANSWERING, CHECKING }

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

    val challenges: List<Direction>
        get() = currentChallenges[currentStage] ?: listOf()
    var gameState: State = State.IDLE
        private set
    var timePassedRelative: Float = 0f

    fun init() {

    }

    fun update() {
        if (challengeStarted) {
            when (gameState) {
                State.CHALLENGING -> {
                    // STATE 1
                    // Show challenge to player for a certain time
                    val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
                    timePassedRelative = timePassed / showChallengeTime
                    if (timePassed > showChallengeTime) {
                        gameState = State.ANSWERING
                        timePassedRelative = 0f
                        startTime = TimeUtils.millis()
                    }
                }
                State.ANSWERING -> {
                    // STATE 2
                    // Give an amount of time for the player to answer
                    val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
                    timePassedRelative = timePassed / answerChallengeTime
                    if (timePassed > answerChallengeTime) {
                        gameState = State.CHECKING
                        timePassedRelative = 0f
                        startTime = TimeUtils.millis()
                    }
                }
                State.CHECKING -> {
                    // STEP 3
                    // Check player's answers
                    checkAnswers(PlayerController.playerAnswers)
                    gameState = State.IDLE
                    startTime = TimeUtils.millis()
                }
                State.IDLE -> {
                    // STEP 4
                    // Start next stage in [stageTransitionTime] seconds if possible
                    if (hasNextStage()) {
                        val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
                        if (timePassed >= stageTransitionTime) {
                            moveToNextStage()
                            gameState = State.CHALLENGING
                            startTime = TimeUtils.millis()
                        }
                    } else {
                        challengeStarted = false
                    }
                }
            }
        }
    }

    override fun dispose() { answerListeners = listOf() }

    private fun hasNextStage(): Boolean = currentStage != Stage.STAGE_3

    private fun moveToNextStage() {
        currentStage = when (currentStage) {
            Stage.STAGE_1 -> Stage.STAGE_2
            Stage.STAGE_2 -> Stage.STAGE_3
            Stage.STAGE_3 -> Stage.STAGE_1
        }
        setupChallenges()
    }

    fun moveToNextLevel(): Boolean {
        var result = true

        currentLevel = when (currentLevel) {
            Level.LEVEL_1 -> Level.LEVEL_2
            Level.LEVEL_2 -> Level.LEVEL_3
            Level.LEVEL_3 -> Level.LEVEL_1
        }

        if (result) {
            setupChallenges()
            challengeStarted = true
        }

        return result
    }

    fun startChallenges() {
        setupChallenges()
        gameState = State.CHALLENGING
        challengeStarted = true
        startTime = TimeUtils.millis()
    }

    private fun setupChallenges() {
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

    private fun checkAnswers(answers: List<Direction>) {
        val directions = currentChallenges[currentStage] as List<Direction>
        var result = true

        directions.forEach { if (!answers.contains(it)) result = false }
        if (result) answerListeners.forEach { it.onCorrectAnswer() }
        else answerListeners.forEach { it.onWrongAnswer() }
    }

    fun addOnAnswerListener(listener: OnAnswerListener) {
        answerListeners = answerListeners.plus(listener)
    }

    interface OnAnswerListener {
        fun onCorrectAnswer()
        fun onWrongAnswer()
    }

}