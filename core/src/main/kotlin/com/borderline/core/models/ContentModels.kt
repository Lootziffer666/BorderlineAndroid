package com.borderline.core.models

import java.io.Serializable

sealed class AccessibilityContent : Serializable {
    abstract val id: String
    abstract val label: String
    abstract val createdAt: Long
    abstract val isFavorite: Boolean
    abstract fun getThumbnail(): String?
}

data class SnippetContent(
    override val id: String,
    override val label: String,
    val content: String,
    val category: String = "general",
    val icon: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null
) : AccessibilityContent() {
    override fun getThumbnail(): String? = null
}

enum class MediaType {
    TEXT,
    IMAGE,
    FILE,
    URI,
    HTML
}

data class ClipboardContent(
    override val id: String,
    override val label: String,
    val type: MediaType,
    val rawContent: String,
    val preview: String,
    val mimeType: String,
    override val createdAt: Long = System.currentTimeMillis(),
    override val isFavorite: Boolean = false,
    val source: String? = null,
    val size: Long? = null
) : AccessibilityContent() {
    override fun getThumbnail(): String? {
        return when (type) {
            MediaType.IMAGE -> rawContent
            MediaType.TEXT, MediaType.FILE, MediaType.URI, MediaType.HTML -> null
        }
    }
}

enum class ShortcutActionType {
    OPEN_APP,
    SEND_TEXT,
    CALL,
    WEB_LINK,
    CUSTOM
}

data class ShortcutContent(
    override val id: String,
    override val label: String,
    val actionType: ShortcutActionType,
    val target: String,
    val icon: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val isFavorite: Boolean = false,
    val appPackageName: String? = null
) : AccessibilityContent() {
    override fun getThumbnail(): String? = null
}

enum class QuickActionType {
    UNDO,
    REDO,
    SCREENSHOT,
    DICTATION,
    CUSTOM
}

enum class ContextType {
    TEXT_EDITING,
    READING,
    NAVIGATION,
    MEDIA_VIEWING,
    UNKNOWN
}

data class QuickActionContent(
    override val id: String,
    override val label: String,
    val actionType: QuickActionType,
    val contextTypes: Set<ContextType> = setOf(ContextType.UNKNOWN),
    val priority: Int = 50,
    val customAction: String? = null,
    override val createdAt: Long = System.currentTimeMillis(),
    override val isFavorite: Boolean = false,
    val triggeredCount: Int = 0,
    val lastUsedAt: Long? = null
) : AccessibilityContent() {
    override fun getThumbnail(): String? = null
}
