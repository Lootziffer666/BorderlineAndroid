package com.borderline.feature.overlay.panels

import android.content.Context
import android.graphics.Color
import android.os.Vibrator
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.borderline.core.models.AccessibilityContent
import com.borderline.feature.overlay.dp
import com.borderline.feature.overlay.vibrateCompat

abstract class SmartPanel(
    protected val context: Context,
    protected val panelId: Int
) {
    var isEditMode: Boolean = false
    var isVisible: Boolean = false

    protected lateinit var rootView: FrameLayout
    protected lateinit var contentRecycler: RecyclerView
    protected lateinit var addButton: ImageButton
    protected var longPressedItem: String? = null

    abstract fun getItems(): List<AccessibilityContent>
    abstract fun onItemSelected(item: AccessibilityContent)
    abstract fun onItemAdded(item: AccessibilityContent)
    abstract fun onItemDeleted(id: String)
    abstract fun onItemReordered(oldIndex: Int, newIndex: Int)
    protected abstract fun showAddDialog()

    open fun createView(): View {
        rootView = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                280.dp,
                when (panelId) {
                    1, 2 -> 64.dp
                    else -> 280.dp
                }
            )
            setBackgroundColor(Color.WHITE)
            elevation = 8f
            visibility = View.GONE
        }

        contentRecycler = RecyclerView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                bottomMargin = 56.dp
            }

            layoutManager = LinearLayoutManager(context)
            adapter = SmartContentAdapter(
                items = getItems().toMutableList(),
                onItemClick = { item ->
                    if (isEditMode) {
                        longPressedItem = item.id
                    } else {
                        onItemSelected(item)
                    }
                },
                onItemLongPress = { item ->
                    enterEditMode(item)
                },
                isEditMode = { isEditMode }
            )
        }

        rootView.addView(contentRecycler)

        addButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_input_add)
            layoutParams = FrameLayout.LayoutParams(52.dp, 52.dp).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 4.dp
            }
            setBackgroundColor(Color.parseColor("#2196F3"))
            setColorFilter(Color.WHITE)
            setOnClickListener {
                if (isEditMode) {
                    exitEditMode()
                } else {
                    showAddDialog()
                }
            }
        }

        rootView.addView(addButton)
        return rootView
    }

    protected fun enterEditMode(item: AccessibilityContent) {
        isEditMode = true
        longPressedItem = item.id
        addButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        (contentRecycler.adapter as? SmartContentAdapter)?.setEditMode(true)
        rootView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        vibrate(50)
    }

    protected fun exitEditMode() {
        isEditMode = false
        longPressedItem = null
        addButton.setImageResource(android.R.drawable.ic_input_add)
        (contentRecycler.adapter as? SmartContentAdapter)?.setEditMode(false)
    }

    fun refreshItems() {
        (contentRecycler.adapter as? SmartContentAdapter)?.submitItems(getItems())
    }

    open fun onOpened() {
        isVisible = true
        refreshItems()
    }

    open fun onClosed() {
        isVisible = false
    }

    protected fun vibrate(durationMs: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrateCompat(durationMs)
    }
}
