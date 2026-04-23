package com.borderline.feature.overlay.panels

import android.content.Context
import android.widget.Toast
import com.borderline.core.models.AccessibilityContent
import com.borderline.core.models.ShortcutContent

class ShortcutsPanel(
    context: Context,
    private val itemsProvider: () -> List<ShortcutContent>,
    private val onShortcutSelected: (ShortcutContent) -> Unit
) : SmartPanel(context, panelId = 3) {

    override fun getItems(): List<AccessibilityContent> = itemsProvider()

    override fun onItemSelected(item: AccessibilityContent) {
        (item as? ShortcutContent)?.let {
            onShortcutSelected(it)
        }
    }

    override fun onItemAdded(item: AccessibilityContent) {
    }

    override fun onItemDeleted(id: String) {
    }

    override fun onItemReordered(oldIndex: Int, newIndex: Int) {
    }

    override fun showAddDialog() {
        Toast.makeText(context, "Add Shortcut TODO", Toast.LENGTH_SHORT).show()
    }
}
