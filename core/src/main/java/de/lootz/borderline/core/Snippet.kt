package de.lootz.borderline.core

import java.util.UUID

/**
 * A reusable text snippet that the user can copy to the clipboard with one tap.
 */
data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
