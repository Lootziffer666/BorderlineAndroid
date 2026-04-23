package com.borderline.feature.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.Resources
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Animator.doOnEnd(action: () -> Unit) {
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            action()
        }
    })
}

fun View.backgroundColor(color: Int) {
    setBackgroundColor(color)
}

fun Vibrator.vibrateCompat(durationMs: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrate(durationMs)
    }
}
