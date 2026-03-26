package de.lootz.borderline.core

import java.util.UUID

/**
 * A clipboard transfer item captured via auto-grab.
 */
data class TransferItem(
    val id: String = UUID.randomUUID().toString(),
    val kind: Kind = Kind.TEXT,
    val label: String = "",
    val preview: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val pinned: Boolean = false
) {
    enum class Kind { TEXT, URI }
}
