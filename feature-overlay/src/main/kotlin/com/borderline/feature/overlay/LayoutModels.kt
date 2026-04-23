package com.borderline.feature.overlay

import android.view.Gravity

data class ScreenDimensions(
    val screenWidth: Int,
    val screenHeight: Int,
    val upper3rdEnd: Int,
    val middle3rdStart: Int,
    val middle3rdEnd: Int,
    val lower3rdStart: Int
)

data class PanelPosition(
    val panelId: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val gravity: Int,
    val edge: ScreenEdge
)

enum class ScreenEdge {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
}

object AccessibilityBarLayout {

    fun calculatePanelPositions(
        dimensions: ScreenDimensions,
        imeHeight: Int = 0
    ): Map<Int, PanelPosition> {
        val panelWidth = 280
        val panelHeight = 280
        val barHeight = 64
        val imeOffset = if (imeHeight > 0) imeHeight + 16 else 0

        return mapOf(
            1 to PanelPosition(
                panelId = 1,
                x = 0,
                y = dimensions.upper3rdEnd - imeOffset,
                width = panelWidth,
                height = barHeight,
                gravity = Gravity.TOP or Gravity.START,
                edge = ScreenEdge.LEFT
            ),
            2 to PanelPosition(
                panelId = 2,
                x = dimensions.screenWidth - panelWidth,
                y = dimensions.upper3rdEnd - imeOffset,
                width = panelWidth,
                height = barHeight,
                gravity = Gravity.TOP or Gravity.END,
                edge = ScreenEdge.RIGHT
            ),
            3 to PanelPosition(
                panelId = 3,
                x = 0,
                y = dimensions.lower3rdStart - imeOffset,
                width = panelWidth,
                height = panelHeight,
                gravity = Gravity.TOP or Gravity.START,
                edge = ScreenEdge.LEFT
            ),
            4 to PanelPosition(
                panelId = 4,
                x = dimensions.screenWidth - panelWidth,
                y = ((dimensions.lower3rdStart + dimensions.screenHeight) / 2) - imeOffset,
                width = panelWidth,
                height = panelHeight - 64,
                gravity = Gravity.TOP or Gravity.END,
                edge = ScreenEdge.RIGHT
            )
        )
    }
}
