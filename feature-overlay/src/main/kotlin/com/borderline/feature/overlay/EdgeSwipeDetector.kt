package com.borderline.feature.overlay

import android.view.MotionEvent
import kotlin.math.sqrt

data class EdgeSwipeEvent(
    val edge: ScreenEdge,
    val velocity: Float,
    val distance: Float,
    val startY: Float,
    val timestamp: Long
) {
    fun isTopSwipe(screenHeight: Int): Boolean = startY < (screenHeight / 3)
    fun isBottomSwipe(screenHeight: Int): Boolean = startY > (2 * screenHeight / 3)
}

class EdgeSwipeDetector {

    private var startX = 0f
    private var startY = 0f

    private val swipeThresholdVelocity = 120f
    private val swipeThresholdDistance = 40f
    private val edgeDetectionZone = 40f

    fun onMotionEvent(
        event: MotionEvent,
        screenWidth: Int,
        screenHeight: Int
    ): EdgeSwipeEvent? {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                null
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - startX
                val dy = event.y - startY
                val distance = sqrt(dx * dx + dy * dy)
                val duration = (event.eventTime - event.downTime).coerceAtLeast(1L).toFloat()
                val velocity = distance / duration * 1000f

                when {
                    startX < edgeDetectionZone &&
                        dx > swipeThresholdDistance &&
                        velocity > swipeThresholdVelocity -> {
                        EdgeSwipeEvent(ScreenEdge.LEFT, velocity, distance, startY, System.currentTimeMillis())
                    }

                    startX > screenWidth - edgeDetectionZone &&
                        dx < -swipeThresholdDistance &&
                        velocity > swipeThresholdVelocity -> {
                        EdgeSwipeEvent(ScreenEdge.RIGHT, velocity, distance, startY, System.currentTimeMillis())
                    }

                    startY < edgeDetectionZone &&
                        dy > swipeThresholdDistance &&
                        velocity > swipeThresholdVelocity -> {
                        EdgeSwipeEvent(ScreenEdge.TOP, velocity, distance, startY, System.currentTimeMillis())
                    }

                    startY > screenHeight - edgeDetectionZone &&
                        dy < -swipeThresholdDistance &&
                        velocity > swipeThresholdVelocity -> {
                        EdgeSwipeEvent(ScreenEdge.BOTTOM, velocity, distance, startY, System.currentTimeMillis())
                    }

                    else -> null
                }
            }
            else -> null
        }
    }
}
