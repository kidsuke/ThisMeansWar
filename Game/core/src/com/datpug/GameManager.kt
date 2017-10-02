package com.datpug

import com.badlogic.gdx.utils.Disposable
import com.datpug.entity.Direction

/**
 * Created by longv on 02-Oct-17.
 */
object LevelManager: Disposable {
    enum class Level { LEVEL_1, LEVEL_2, LEVEL_3 }
    enum class Stage { STAGE_1, STAGE_2, STAGE_3 }

    var currentLevel: Level = Level.LEVEL_1
    var currentStage: Stage = Stage.STAGE_1
    var currentChallenges: Map<Stage, List<Direction>> = mapOf()
    var answerListeners: List<OnAnswerListener> = listOf()

    fun getDirections(): Map<Stage, List<Direction>> {
        val challenges = mutableMapOf<Stage, List<Direction>>()

        when (currentLevel) {
            Level.LEVEL_1 -> {
                // Stage 1
                challenges.put(
                    Stage.STAGE_1,
                    listOf(Direction.LEFT, Direction.DOWN, Direction.LEFT, Direction.DOWN)
                )

                // Stage 2

                // Stage 3
            }
            Level.LEVEL_2 -> {

            }
            Level.LEVEL_3 -> {

            }
        }

        currentChallenges = challenges

        return currentChallenges
    }

    fun addOnAnswerListener(listener: OnAnswerListener) {
        answerListeners = answerListeners.plus(listener)
    }

    fun checkChallenges(answers: List<Direction>) {
        val directions = currentChallenges[currentStage] as List<Direction>
        var result: Boolean = true

        directions.forEach { if (!answers.contains(it)) result = false }
        if (result) answerListeners.forEach { it.onCorrectAnswer() }
        else answerListeners.forEach { it.onWrongAnswer() }
    }

    override fun dispose() {
        answerListeners = listOf()
    }

    interface OnAnswerListener {
        fun onCorrectAnswer()
        fun onWrongAnswer()
    }

}