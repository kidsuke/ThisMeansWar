package com.datpug.ar

import android.util.Log
import com.vuforia.*

/**
 * Created by longv on 24-Sep-17.
 */
object SampleMath {
    private val LOGTAG = "SampleMath"

    private val temp = FloatArray(16)
    private var mLineStart = Vec3F()
    private var mLineEnd = Vec3F()
    private var mIntersection: Vec3F? = Vec3F()

    fun Vec2FSub(v1: Vec2F, v2: Vec2F): Vec2F {
        temp[0] = v1.data[0] - v2.data[0]
        temp[1] = v1.data[1] - v2.data[1]
        return Vec2F(temp[0], temp[1])
    }

    fun Vec2FDist(v1: Vec2F, v2: Vec2F): Float {
        val dx = v1.data[0] - v2.data[0]
        val dy = v1.data[1] - v2.data[1]
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    fun Vec3FAdd(v1: Vec3F, v2: Vec3F): Vec3F {
        temp[0] = v1.data[0] + v2.data[0]
        temp[1] = v1.data[1] + v2.data[1]
        temp[2] = v1.data[2] + v2.data[2]
        return Vec3F(temp[0], temp[1], temp[2])
    }

    fun Vec3FSub(v1: Vec3F, v2: Vec3F): Vec3F {
        temp[0] = v1.data[0] - v2.data[0]
        temp[1] = v1.data[1] - v2.data[1]
        temp[2] = v1.data[2] - v2.data[2]
        return Vec3F(temp[0], temp[1], temp[2])
    }

    fun Vec3FScale(v: Vec3F, s: Float): Vec3F {
        temp[0] = v.data[0] * s
        temp[1] = v.data[1] * s
        temp[2] = v.data[2] * s
        return Vec3F(temp[0], temp[1], temp[2])
    }

    fun Vec3FDot(v1: Vec3F, v2: Vec3F): Float {
        return v1.data[0] * v2.data[0] + v1.data[1] * v2.data[1] + v1.data[2] * v2.data[2]
    }

    fun Vec3FCross(v1: Vec3F, v2: Vec3F): Vec3F {
        temp[0] = v1.data[1] * v2.data[2] - v1.data[2] * v2.data[1]
        temp[1] = v1.data[2] * v2.data[0] - v1.data[0] * v2.data[2]
        temp[2] = v1.data[0] * v2.data[1] - v1.data[1] * v2.data[0]
        return Vec3F(temp[0], temp[1], temp[2])
    }

    fun Vec3FNormalize(v: Vec3F): Vec3F {
        var length = Math
                .sqrt((v.data[0] * v.data[0] + v.data[1] * v.data[1] + v.data[2] * v.data[2]).toDouble()).toFloat()
        if (length != 0.0f)
            length = 1.0f / length

        temp[0] = v.data[0] * length
        temp[1] = v.data[1] * length
        temp[2] = v.data[2] * length

        return Vec3F(temp[0], temp[1], temp[2])
    }

    fun Vec3FTransform(v: Vec3F, m: Matrix44F): Vec3F {
        val lambda: Float
        lambda = (m.data[12] * v.data[0] + m.data[13] * v.data[1] + m.data[14] * v.data[2]
                + m.data[15])

        temp[0] = m.data[0] * v.data[0] + m.data[1] * v.data[1] + m.data[2] * v.data[2] + m.data[3]
        temp[1] = m.data[4] * v.data[0] + m.data[5] * v.data[1] + m.data[6] * v.data[2] + m.data[7]
        temp[2] = (m.data[8] * v.data[0] + m.data[9] * v.data[1] + m.data[10] * v.data[2]
                + m.data[11])

        temp[0] /= lambda
        temp[1] /= lambda
        temp[2] /= lambda

        return Vec3F(temp[0], temp[1], temp[2])
    }

    fun Vec3FTransformNormal(v: Vec3F, m: Matrix44F): Vec3F {
        temp[0] = m.data[0] * v.data[0] + m.data[1] * v.data[1] + m.data[2] * v.data[2]
        temp[1] = m.data[4] * v.data[0] + m.data[5] * v.data[1] + m.data[6] * v.data[2]
        temp[2] = m.data[8] * v.data[0] + m.data[9] * v.data[1] + m.data[10] * v.data[2]

        return Vec3F(temp[0], temp[1], temp[2])
    }

    fun Vec4FTransform(v: Vec4F, m: Matrix44F): Vec4F {
        temp[0] = m.data[0] * v.data[0] + m.data[1] * v.data[1] + m.data[2] * v.data[2] + m.data[3] * v.data[3]
        temp[1] = m.data[4] * v.data[0] + m.data[5] * v.data[1] + m.data[6] * v.data[2] + m.data[7] * v.data[3]
        temp[2] = (m.data[8] * v.data[0] + m.data[9] * v.data[1] + m.data[10] * v.data[2]
                + m.data[11] * v.data[3])
        temp[3] = (m.data[12] * v.data[0] + m.data[13] * v.data[1] + m.data[14] * v.data[2]
                + m.data[15] * v.data[3])

        return Vec4F(temp[0], temp[1], temp[2], temp[3])
    }

    fun Vec4FDiv(v: Vec4F, s: Float): Vec4F {
        temp[0] = v.data[0] / s
        temp[1] = v.data[1] / s
        temp[2] = v.data[2] / s
        temp[3] = v.data[3] / s
        return Vec4F(temp[0], temp[1], temp[2], temp[3])
    }

    fun Matrix44FIdentity(): Matrix44F {
        val r = Matrix44F()

        for (i in 0..15)
            temp[i] = 0.0f

        temp[0] = 1.0f
        temp[5] = 1.0f
        temp[10] = 1.0f
        temp[15] = 1.0f

        r.data = temp

        return r
    }

    fun Matrix44FTranspose(m: Matrix44F): Matrix44F {
        val r = Matrix44F()
        for (i in 0..3)
            for (j in 0..3)
                temp[i * 4 + j] = m.data[i + 4 * j]

        r.data = temp
        return r
    }

    fun Matrix44FDeterminate(m: Matrix44F): Float {
        return ((((((((m.data[12] * m.data[9] * m.data[6]
                * m.data[3]) - (m.data[8] * m.data[13]
                * m.data[6] * m.data[3]) - (m.data[12]
                * m.data[5] * m.data[10] * m.data[3])
                + (m.data[4] * m.data[13] * m.data[10]
                * m.data[3]) + (m.data[8] * m.data[5]
                * m.data[14] * m.data[3])) - (m.data[4]
                * m.data[9] * m.data[14] * m.data[3])
                - (m.data[12] * m.data[9] * m.data[2]
                * m.data[7])) + (m.data[8] * m.data[13]
                * m.data[2] * m.data[7]) + (m.data[12]
                * m.data[1] * m.data[10] * m.data[7])
                - (m.data[0] * m.data[13] * m.data[10]
                * m.data[7]) - (m.data[8] * m.data[1]
                * m.data[14] * m.data[7])) + (m.data[0]
                * m.data[9] * m.data[14] * m.data[7])
                + (m.data[12] * m.data[5] * m.data[2]
                * m.data[11])) - (m.data[4] * m.data[13]
                * m.data[2] * m.data[11]) - (m.data[12]
                * m.data[1] * m.data[6] * m.data[11])
                + (m.data[0] * m.data[13] * m.data[6]
                * m.data[11]) + (m.data[4] * m.data[1]
                * m.data[14] * m.data[11])) - (m.data[0]
                * m.data[5] * m.data[14] * m.data[11])
                - (m.data[8] * m.data[5] * m.data[2]
                * m.data[15])) + (m.data[4] * m.data[9]
                * m.data[2] * m.data[15]) + (m.data[8]
                * m.data[1] * m.data[6] * m.data[15])
                - (m.data[0] * m.data[9] * m.data[6]
                * m.data[15]) - (m.data[4] * m.data[1]
                * m.data[10] * m.data[15])) + (m.data[0]
                * m.data[5] * m.data[10] * m.data[15])
    }

    fun Matrix44FInverse(m: Matrix44F): Matrix44F {
        val r = Matrix44F()

        val det = 1.0f / Matrix44FDeterminate(m)

        temp[0] = (m.data[6] * m.data[11] * m.data[13] - m.data[7] * m.data[10] * m.data[13] + m.data[7] * m.data[9] * m.data[14]
                - m.data[5] * m.data[11] * m.data[14]
                - m.data[6] * m.data[9] * m.data[15]) + m.data[5] * m.data[10] * m.data[15]

        temp[4] = ((m.data[3] * m.data[10] * m.data[13]
                - m.data[2] * m.data[11] * m.data[13]
                - m.data[3] * m.data[9] * m.data[14])
                + m.data[1] * m.data[11] * m.data[14]
                + m.data[2] * m.data[9] * m.data[15]) - m.data[1] * m.data[10] * m.data[15]

        temp[8] = (m.data[2] * m.data[7] * m.data[13] - m.data[3] * m.data[6] * m.data[13] + m.data[3] * m.data[5] * m.data[14]
                - m.data[1] * m.data[7] * m.data[14]
                - m.data[2] * m.data[5] * m.data[15]) + m.data[1] * m.data[6] * m.data[15]

        temp[12] = (m.data[3] * m.data[6] * m.data[9]
                - m.data[2] * m.data[7] * m.data[9] - (m.data[3]
                * m.data[5] * m.data[10])) + (m.data[1]
                * m.data[7] * m.data[10]) + (m.data[2]
                * m.data[5] * m.data[11]) - (m.data[1]
                * m.data[6] * m.data[11])

        temp[1] = ((m.data[7] * m.data[10] * m.data[12]
                - m.data[6] * m.data[11] * m.data[12]
                - m.data[7] * m.data[8] * m.data[14])
                + m.data[4] * m.data[11] * m.data[14]
                + m.data[6] * m.data[8] * m.data[15]) - m.data[4] * m.data[10] * m.data[15]

        temp[5] = (m.data[2] * m.data[11] * m.data[12] - m.data[3] * m.data[10] * m.data[12] + m.data[3] * m.data[8] * m.data[14]
                - m.data[0] * m.data[11] * m.data[14]
                - m.data[2] * m.data[8] * m.data[15]) + m.data[0] * m.data[10] * m.data[15]

        temp[9] = ((m.data[3] * m.data[6] * m.data[12]
                - m.data[2] * m.data[7] * m.data[12]
                - m.data[3] * m.data[4] * m.data[14])
                + m.data[0] * m.data[7] * m.data[14]
                + m.data[2] * m.data[4] * m.data[15]) - m.data[0] * m.data[6] * m.data[15]

        temp[13] = m.data[2] * m.data[7] * m.data[8] - m.data[3] * m.data[6] * m.data[8] + (m.data[3]
                * m.data[4] * m.data[10]) - (m.data[0]
                * m.data[7] * m.data[10]) - (m.data[2]
                * m.data[4] * m.data[11]) + (m.data[0]
                * m.data[6] * m.data[11])

        temp[2] = (m.data[5] * m.data[11] * m.data[12] - m.data[7] * m.data[9] * m.data[12] + m.data[7] * m.data[8] * m.data[13]
                - m.data[4] * m.data[11] * m.data[13]
                - m.data[5] * m.data[8] * m.data[15]) + m.data[4] * m.data[9] * m.data[15]

        temp[6] = ((m.data[3] * m.data[9] * m.data[12]
                - m.data[1] * m.data[11] * m.data[12]
                - m.data[3] * m.data[8] * m.data[13])
                + m.data[0] * m.data[11] * m.data[13]
                + m.data[1] * m.data[8] * m.data[15]) - m.data[0] * m.data[9] * m.data[15]

        temp[10] = (m.data[1] * m.data[7] * m.data[12] - m.data[3] * m.data[5] * m.data[12] + m.data[3] * m.data[4] * m.data[13]
                - m.data[0] * m.data[7] * m.data[13]
                - m.data[1] * m.data[4] * m.data[15]) + m.data[0] * m.data[5] * m.data[15]

        temp[14] = (m.data[3] * m.data[5] * m.data[8]
                - m.data[1] * m.data[7] * m.data[8] - (m.data[3]
                * m.data[4] * m.data[9])) + (m.data[0] * m.data[7]
                * m.data[9]) + (m.data[1] * m.data[4]
                * m.data[11]) - (m.data[0] * m.data[5]
                * m.data[11])

        temp[3] = ((m.data[6] * m.data[9] * m.data[12]
                - m.data[5] * m.data[10] * m.data[12]
                - m.data[6] * m.data[8] * m.data[13])
                + m.data[4] * m.data[10] * m.data[13]
                + m.data[5] * m.data[8] * m.data[14]) - m.data[4] * m.data[9] * m.data[14]

        temp[7] = (m.data[1] * m.data[10] * m.data[12] - m.data[2] * m.data[9] * m.data[12] + m.data[2] * m.data[8] * m.data[13]
                - m.data[0] * m.data[10] * m.data[13]
                - m.data[1] * m.data[8] * m.data[14]) + m.data[0] * m.data[9] * m.data[14]

        temp[11] = ((m.data[2] * m.data[5] * m.data[12]
                - m.data[1] * m.data[6] * m.data[12]
                - m.data[2] * m.data[4] * m.data[13])
                + m.data[0] * m.data[6] * m.data[13]
                + m.data[1] * m.data[4] * m.data[14]) - m.data[0] * m.data[5] * m.data[14]

        temp[15] = m.data[1] * m.data[6] * m.data[8] - m.data[2] * m.data[5] * m.data[8] + (m.data[2]
                * m.data[4] * m.data[9]) - (m.data[0] * m.data[6]
                * m.data[9]) - (m.data[1] * m.data[4]
                * m.data[10]) + (m.data[0] * m.data[5]
                * m.data[10])

        for (i in 0..15)
            temp[i] *= det

        r.data = temp
        return r
    }

    fun linePlaneIntersection(lineStart: Vec3F, lineEnd: Vec3F,
                              pointOnPlane: Vec3F, planeNormal: Vec3F): Vec3F? {
        var lineDir = Vec3FSub(lineEnd, lineStart)
        lineDir = Vec3FNormalize(lineDir)

        val planeDir = Vec3FSub(pointOnPlane, lineStart)

        val n = Vec3FDot(planeNormal, planeDir)
        val d = Vec3FDot(planeNormal, lineDir)

        if (Math.abs(d) < 0.00001) {
            // Line is parallel to plane
            return null
        }

        val dist = n / d

        val offset = Vec3FScale(lineDir, dist)

        return Vec3FAdd(lineStart, offset)
    }

    private fun projectScreenPointToPlane(inverseProjMatrix: Matrix44F,
                                          modelViewMatrix: Matrix44F, screenWidth: Float, screenHeight: Float,
                                          point: Vec2F, planeCenter: Vec3F, planeNormal: Vec3F) {
        // Window Coordinates to Normalized Device Coordinates
        val config = Renderer.getInstance()
                .videoBackgroundConfig

        val halfScreenWidth = screenWidth / 2.0f
        val halfScreenHeight = screenHeight / 2.0f

        val halfViewportWidth = config.size.data[0] / 2.0f
        val halfViewportHeight = config.size.data[1] / 2.0f

        val x = (point.data[0] - halfScreenWidth) / halfViewportWidth
        val y = (point.data[1] - halfScreenHeight) / halfViewportHeight * -1

        val ndcNear = Vec4F(x, y, -1f, 1f)
        val ndcFar = Vec4F(x, y, 1f, 1f)

        // Normalized Device Coordinates to Eye Coordinates
        var pointOnNearPlane = Vec4FTransform(ndcNear, inverseProjMatrix)
        var pointOnFarPlane = Vec4FTransform(ndcFar, inverseProjMatrix)
        pointOnNearPlane = Vec4FDiv(pointOnNearPlane,
                pointOnNearPlane.data[3])
        pointOnFarPlane = Vec4FDiv(pointOnFarPlane,
                pointOnFarPlane.data[3])

        // Eye Coordinates to Object Coordinates
        val inverseModelViewMatrix = Matrix44FInverse(modelViewMatrix)

        val nearWorld = Vec4FTransform(pointOnNearPlane,
                inverseModelViewMatrix)
        val farWorld = Vec4FTransform(pointOnFarPlane, inverseModelViewMatrix)

        mLineStart = Vec3F(nearWorld.data[0], nearWorld.data[1],
                nearWorld.data[2])
        mLineEnd = Vec3F(farWorld.data[0], farWorld.data[1],
                farWorld.data[2])
        mIntersection = linePlaneIntersection(mLineStart, mLineEnd,
                planeCenter, planeNormal)

        if (mIntersection == null)
            Log.e(LOGTAG, "No intersection with the plane")
    }

    fun getPointToPlaneIntersection(
            inverseProjMatrix: Matrix44F, modelViewMatrix: Matrix44F,
            screenWidth: Float, screenHeight: Float, point: Vec2F, planeCenter: Vec3F,
            planeNormal: Vec3F): Vec3F? {
        projectScreenPointToPlane(inverseProjMatrix, modelViewMatrix,
                screenWidth, screenHeight, point, planeCenter, planeNormal)
        return mIntersection
    }

    fun getPointToPlaneLineStart(inverseProjMatrix: Matrix44F,
                                 modelViewMatrix: Matrix44F, screenWidth: Float, screenHeight: Float,
                                 point: Vec2F, planeCenter: Vec3F, planeNormal: Vec3F): Vec3F {
        projectScreenPointToPlane(inverseProjMatrix, modelViewMatrix,
                screenWidth, screenHeight, point, planeCenter, planeNormal)
        return mLineStart
    }

    fun getPointToPlaneLineEnd(inverseProjMatrix: Matrix44F,
                               modelViewMatrix: Matrix44F, screenWidth: Float, screenHeight: Float,
                               point: Vec2F, planeCenter: Vec3F, planeNormal: Vec3F): Vec3F {
        projectScreenPointToPlane(inverseProjMatrix, modelViewMatrix,
                screenWidth, screenHeight, point, planeCenter, planeNormal)
        return mLineEnd
    }
}