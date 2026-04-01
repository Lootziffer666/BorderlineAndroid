package de.lootz.borderline.feature.overlay

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View

/**
 * Applies a subtle glassmorphism blur to overlay panels on API >= 31 (Android 12+).
 *
 * On older APIs this is a no-op — the semi-transparent drawable backgrounds
 * provide sufficient visual separation without real blur.
 */
object GlassEffect {

    private const val BLUR_RADIUS = 12f

    /**
     * Apply a glass blur to [view]. Safe to call on any API level.
     * On API < 31, does nothing.
     */
    fun apply(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(
                RenderEffect.createBlurEffect(BLUR_RADIUS, BLUR_RADIUS, Shader.TileMode.CLAMP)
            )
        }
    }

    /**
     * Remove any previously applied render effect.
     */
    fun clear(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(null)
        }
    }
}
