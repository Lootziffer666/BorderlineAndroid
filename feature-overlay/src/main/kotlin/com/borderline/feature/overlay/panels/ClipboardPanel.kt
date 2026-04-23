package com.borderline.feature.overlay.panels

import android.content.Context
import android.widget.Toast
import com.borderline.core.models.AccessibilityContent
import com.borderline.core.models.ClipboardContent

class ClipboardPanel(
    context: Context,
    private val itemsProvider: () -> List<ClipboardContent>,
    private val onClipboardSelected: (ClipboardContent) -> Unit
) : SmartPanel(context, panelId = 2) {

    override fun getItems(): List<AccessibilityContent> = itemsProvider()

    override fun onItemSelected(item: AccessibilityContent) {
        (item as? ClipboardContent)?.let {
            onClipboardSelected(it)
        }
    }

    override fun onItemAdded(item: AccessibilityContent) {
    }

    override fun onItemDeleted(id: String) {
    }

    override fun onItemReordered(oldIndex: Int, newIndex: Int) {
    }

    override fun showAddDialog() {
        Toast.makeText(context, "Clipboard add is capture-driven", Toast.LENGTH_SHORT).show()
    }
}
