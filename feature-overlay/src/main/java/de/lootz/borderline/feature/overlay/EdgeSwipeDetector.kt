package de.lootz.borderline.feature.overlay

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Detects horizontal fling / swipe-in gestures on edge handle views.
 *
 * @param onSwipeIn called when the user swipes inward (from the screen edge).
 * @param onTap    called for a simple single tap (fallback if no fling detected).
 */
class EdgeSwipeDetector(
    context: Context,
    private val isLeftEdge: Boolean,
    private val onSwipeIn: () -> Unit,
    private val onTap: () -> Unit
) : View.OnTouchListener {

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onTap()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val dx = e2.x - e1.x
            val dy = e2.y - e1.y
            if (abs(dx) < abs(dy)) return false
            if (abs(dx) < BorderlineMotion.SWIPE_MIN_DISTANCE) return false
            if (abs(velocityX) < BorderlineMotion.SWIPE_MIN_VELOCITY) return false

            // left-edge: swipe must go RIGHT (dx > 0); right-edge: swipe must go LEFT (dx < 0)
            val valid = if (isLeftEdge) dx > 0 else dx < 0
            if (valid) {
                onSwipeIn()
                return true
            }
            return false
        }

        override fun onDown(e: MotionEvent): Boolean = true
    })

    @Suppress("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}
