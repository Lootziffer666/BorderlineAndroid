package de.lootz.borderline.feature.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

/**
 * Centralized motion, timing, and haptic constants for Borderline.
 *
 * Design intent: Borderline should appear "da" (there), not "ta-da!".
 * All timings are deliberately calm — no spring, no bounce, no techy overshoot.
 */
object BorderlineMotion {

    // ── Panel transitions ──────────────────────────────────────

    /** Panel entry: fade + slight translate. Total perceived duration. */
    const val PANEL_IN_DURATION = 160L

    /** Panel exit: fade out. Slightly faster than entry. */
    const val PANEL_OUT_DURATION = 120L

    /** Slide-in distance in dp (converted to px at use-site). */
    const val PANEL_IN_TRANSLATE_DP = 8f

    /** Interpolator factor for panel entry deceleration. */
    const val PANEL_IN_EASE = 1.8f

    /** Interpolator factor for panel exit acceleration. */
    const val PANEL_OUT_EASE = 1.6f

    // ── Handle ─────────────────────────────────────────────────

    /** Idle opacity floor — handle "breathes" between this and [HANDLE_IDLE_ALPHA_MAX]. */
    const val HANDLE_IDLE_ALPHA_MIN = 0.78f

    /** Idle opacity ceiling. */
    const val HANDLE_IDLE_ALPHA_MAX = 0.82f

    /** Full breathing cycle duration (min→max→min). */
    const val HANDLE_BREATHE_DURATION = 3200L

    /** Opacity when panel is open — handle dims slightly. */
    const val HANDLE_DIMMED_ALPHA = 0.55f

    /** Touch halo fade-in duration. */
    const val HALO_IN_DURATION = 80L

    /** Touch halo fade-out duration. */
    const val HALO_OUT_DURATION = 200L

    // ── Haptics ────────────────────────────────────────────────

    /** Light tick for non-destructive taps (copy, open, navigate). */
    const val HAPTIC_LIGHT = 0

    /** Confirmation haptic for save, pin, successful action. */
    const val HAPTIC_CONFIRM = 1

    /** Rejection / destructive haptic for delete, disable. */
    const val HAPTIC_REJECT = 2

    // ── Swipe thresholds (px) ──────────────────────────────────

    const val SWIPE_MIN_DISTANCE = 40f
    const val SWIPE_MIN_VELOCITY = 120f

    // ── Grab feedback ──────────────────────────────────────────

    const val GRAB_FEEDBACK_MS = 2000L

    // ── Accessibility debounce ─────────────────────────────────

    const val DEBOUNCE_MS = 100L

    // ── Interpolators (reusable singletons) ────────────────────

    val Decelerate: Interpolator = DecelerateInterpolator(1.8f)
    val Accelerate: Interpolator = AccelerateInterpolator(1.6f)
    val Linear: Interpolator = LinearInterpolator()

    // ── Panel animation builders ───────────────────────────────

    /**
     * Builds a panel-in animation: quiet fade + minimal translate.
     * No scale — scaling reads as "bouncy" and adds perceived weight.
     */
    fun panelIn(view: View, translateDx: Float, onEnd: (() -> Unit)? = null): AnimatorSet {
        val dp = view.resources.displayMetrics.density
        val translatePx = PANEL_IN_TRANSLATE_DP * dp * if (translateDx < 0) -1f else 1f

        view.alpha = 0f
        view.translationX = translatePx

        val fade = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val slide = ObjectAnimator.ofFloat(view, "translationX", translatePx, 0f)

        return AnimatorSet().apply {
            playTogether(fade, slide)
            duration = PANEL_IN_DURATION
            interpolator = DecelerateInterpolator(PANEL_IN_EASE)
            if (onEnd != null) {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = onEnd()
                })
            }
        }
    }

    /**
     * Builds a panel-out animation: quick fade, no translate.
     * Panel disappears without drawing attention to where it went.
     */
    fun panelOut(view: View, onEnd: (() -> Unit)? = null): AnimatorSet {
        val fade = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)

        return AnimatorSet().apply {
            playTogether(fade)
            duration = PANEL_OUT_DURATION
            interpolator = AccelerateInterpolator(PANEL_OUT_EASE)
            if (onEnd != null) {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = onEnd()
                })
            }
        }
    }

    // ── Handle breathing animation ─────────────────────────────

    /**
     * Subtle opacity pulse on the edge handle.
     * 78% → 82% → 78% over [HANDLE_BREATHE_DURATION]ms.
     * Repeats infinitely. Cancel when handle is touched or panel opens.
     */
    fun startHandleBreathing(view: View): ValueAnimator {
        return ValueAnimator.ofFloat(HANDLE_IDLE_ALPHA_MIN, HANDLE_IDLE_ALPHA_MAX).apply {
            duration = HANDLE_BREATHE_DURATION / 2
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                view.alpha = anim.animatedValue as Float
            }
            start()
        }
    }

    // ── Haptic performer ───────────────────────────────────────

    /**
     * Perform contextual haptic feedback.
     *
     * Uses [HapticFeedbackConstants] on API >= R (30) with appropriate constants:
     * - [HAPTIC_LIGHT]: KEYBOARD_TAP / virtual key press — barely noticeable
     * - [HAPTIC_CONFIRM]: CONFIRM — positive, final
     * - [HAPTIC_REJECT]: REJECT — heavy, cautionary
     *
     * On older APIs, falls back to KEYBOARD_TAP for light and a short vibrator pulse for heavy.
     */
    fun haptic(view: View, type: Int = HAPTIC_LIGHT) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val constant = when (type) {
                    HAPTIC_CONFIRM -> HapticFeedbackConstants.CONFIRM
                    HAPTIC_REJECT -> HapticFeedbackConstants.REJECT
                    else -> HapticFeedbackConstants.KEYBOARD_TAP
                }
                view.performHapticFeedback(constant)
            }
            type == HAPTIC_REJECT -> {
                @Suppress("DEPRECATION")
                val vibrator = view.context.getSystemService(android.os.Vibrator::class.java)
                    ?: view.context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        30, android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
            else -> {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }
}
