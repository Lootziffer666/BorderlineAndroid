package com.borderline.feature.overlay.panels

import android.content.Context
import android.widget.Toast
import com.borderline.core.models.AccessibilityContent
import com.borderline.core.models.QuickActionContent

class QuickActionsPanel(
    context: Context,
    private val itemsProvider: () -> List<QuickActionContent>,
    private val onQuickActionSelected: (QuickActionContent) -> Unit
) : SmartPanel(context, panelId = 4) {

    override fun getItems(): List<AccessibilityContent> = itemsProvider()

    override fun onItemSelected(item: AccessibilityContent) {
        (item as? QuickActionContent)?.let {
            onQuickActionSelected(it)
        }
    }

    override fun onItemAdded(item: AccessibilityContent) {
    }

    override fun onItemDeleted(id: String) {
    }

    override fun onItemReordered(oldIndex: Int, newIndex: Int) {
    }

    override fun showAddDialog() {
        Toast.makeText(context, "Add Quick Action TODO", Toast.LENGTH_SHORT).show()
    }
}
