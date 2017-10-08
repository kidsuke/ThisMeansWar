package com.datpug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.TimeUtils
import com.datpug.controller.MonsterController
import com.datpug.controller.PlayerController
import com.datpug.entity.Direction

/**
 * Created by longv on 02-Oct-17.
 */
object GameManager {
    enum class Level { LEVEL_1, LEVEL_2, LEVEL_3 }
    enum class Stage { STAGE_1, STAGE_2, STAGE_3 }
    enum class State { IDLE, SEARCHING, CHALLENGING, ANSWERING, CHECKING, LOSE, WIN }

    private var currentChallenges: Map<Stage, List<Direction>> = mapOf()
    private var startTime = TimeUtils.millis()
    private var showChallengeTime = 10f
    private var answerChallengeTime = 5f
    private var stageTransitionTime = 5f
    private var levelTransitionTime = 5f

    var currentLevel: Level = Level.LEVEL_1
        private set
    var currentStage: Stage = Stage.STAGE_1
        private set
    var answerListeners: List<OnAnswerListener> = listOf()
    val challenges: List<Direction>
        get() = currentChallenges[currentStage] ?: listOf()
    var gameState: State = State.SEARCHING
        private set
    var timePassedRelative: Float = 0f

    init {
        setupChallenges()
    }

    fun update() {
        when (gameState) {
            State.SEARCHING -> { }
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
                if (!PlayerController.isPlayerAlive) {
                    // If player is not alive, he has lost
                    gameState = State.LOSE
                } else {
                    if (MonsterController.isMonsterDead) {
                        PlayerController.healthBonus(getHealthBonus())
                    }

                    if (hasNextStage()) {
                        // Start next stage in [stageTransitionTime] seconds
                        val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
                        if (timePassed >= stageTransitionTime) {
                            moveToNextStage()
                            startChallenges()
                        }
                    } else {
                        if (hasNextLevel()) {
                            // Start next stage in [levelTransitionTime] seconds
                            val timePassed: Float = (TimeUtils.timeSinceMillis(startTime) + Gdx.graphics.deltaTime).div(1000)
                            if (timePassed >= levelTransitionTime) {
                                // Searching for monster again...
                                moveToNextLevel()
                                gameState = State.SEARCHING
                                startTime = TimeUtils.millis()
                            }
                        } else {
                            // If there is no level left, the player has won
                            gameState = State.WIN
                        }
                    }
                }
            }
            State.WIN -> {}
            State.LOSE -> {}
        }
    }

    private fun hasNextStage(): Boolean = currentStage != Stage.STAGE_3

    private fun hasNextLevel(): Boolean = currentLevel != Level.LEVEL_3

    private fun moveToNextStage() {
        currentStage = when (currentStage) {
            Stage.STAGE_1 -> Stage.STAGE_2
            Stage.STAGE_2 -> Stage.STAGE_3
            Stage.STAGE_3 -> Stage.STAGE_1
        }
        setupChallenges()
    }

    private fun moveToNextLevel() {
        currentStage = Stage.STAGE_1
        currentLevel = when (currentLevel) {
            Level.LEVEL_1 -> Level.LEVEL_2
            Level.LEVEL_2 -> Level.LEVEL_3
            Level.LEVEL_3 -> Level.LEVEL_1
        }
        setupChallenges()
    }

    fun startChallenges() {
        gameState = State.CHALLENGING
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
                    listOf(
                        Direction.DOWN, Direction.DOWN, Direction.UP, Direction.UP,
                        Direction.RIGHT, Direction.LEFT)
                )
                // Stage 3
                challenges.put(
                    Stage.STAGE_3,
                    listOf(Direction.RIGHT, Direction.LEFT, Direction.LEFT, Direction.LEFT,
                           Direction.UP, Direction.UP, Direction.DOWN, Direction.DOWN)
                )
            }
            Level.LEVEL_2 -> {
                // Stage 1
                challenges.put(
                    Stage.STAGE_1,
                    listOf(Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.RIGHT)
                )
                // Stage 2
                challenges.put(
                    Stage.STAGE_2,
                    listOf(Direction.LEFT, Direction.LEFT, Direction.DOWN,
                            Direction.RIGHT, Direction.DOWN, Direction.RIGHT)
                )
                // Stage 3
                challenges.put(
                    Stage.STAGE_3,
                    listOf(Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT,
                           Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT)
                )
            }
            Level.LEVEL_3 -> {
                // Stage 1
                challenges.put(
                    Stage.STAGE_1,
                    listOf(Direction.RIGHT, Direction.DOWN, Direction.RIGHT, Direction.LEFT)
                )
                // Stage 2
                challenges.put(
                    Stage.STAGE_2,
                    listOf(Direction.UP, Direction.LEFT, Direction.RIGHT,
                           Direction.DOWN, Direction.RIGHT, Direction.LEFT)
                )
                // Stage 3
                challenges.put(
                    Stage.STAGE_3,
                    listOf(Direction.DOWN, Direction.DOWN, Direction.UP, Direction.RIGHT,
                           Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT)
                )
            }
        }

        currentChallenges = challenges
    }

    fun getHealthBonus(): Float = when(currentLevel) {
        Level.LEVEL_1 ->  100f
        Level.LEVEL_2 -> 200f
        Level.LEVEL_3 -> 300f
    }

    fun getDamage(): Float = when(currentLevel) {
        Level.LEVEL_1 ->  150f
        Level.LEVEL_2 -> 250f
        Level.LEVEL_3 -> 350f
    }

    private fun checkAnswers(answers: List<Direction>) {
        val directions = currentChallenges[currentStage] as List<Direction>
        var result = true

        if (directions.size != answers.size) result = false
        else directions.forEachIndexed { index, direction -> if (answers[index] != direction) result = false }
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