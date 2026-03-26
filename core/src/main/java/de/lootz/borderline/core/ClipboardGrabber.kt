package de.lootz.borderline.core

import android.content.ClipboardManager
import android.content.Context

/**
 * Reads the current system clipboard content on demand.
 * Returns `null` when the clipboard is empty or inaccessible.
 */
object ClipboardGrabber {

    private const val PREVIEW_MAX_LENGTH = 200

    fun grab(context: Context): TransferItem? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        if (!clipboard.hasPrimaryClip()) return null

        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null

        val item = clip.getItemAt(0)
        val text = item.coerceToText(context)?.toString()
        if (text.isNullOrBlank()) return null

        val label = clip.description?.label?.toString() ?: ""
        return TransferItem(
            kind = TransferItem.Kind.TEXT,
            label = label,
            preview = text.take(PREVIEW_MAX_LENGTH)
        )
    }
}
