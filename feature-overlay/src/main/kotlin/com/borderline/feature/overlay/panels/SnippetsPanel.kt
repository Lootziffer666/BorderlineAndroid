package com.borderline.feature.overlay.panels

import android.content.Context
import android.widget.Toast
import com.borderline.core.models.AccessibilityContent
import com.borderline.core.models.SnippetContent

class SnippetsPanel(
    context: Context,
    private val itemsProvider: () -> List<SnippetContent>,
    private val onSnippetSelected: (SnippetContent) -> Unit
) : SmartPanel(context, panelId = 1) {

    override fun getItems(): List<AccessibilityContent> = itemsProvider()

    override fun onItemSelected(item: AccessibilityContent) {
        (item as? SnippetContent)?.let {
            onSnippetSelected(it)
        }
    }

    override fun onItemAdded(item: AccessibilityContent) {
    }

    override fun onItemDeleted(id: String) {
    }

    override fun onItemReordered(oldIndex: Int, newIndex: Int) {
    }

    override fun showAddDialog() {
        Toast.makeText(context, "Add Snippet TODO", Toast.LENGTH_SHORT).show()
    }
}
