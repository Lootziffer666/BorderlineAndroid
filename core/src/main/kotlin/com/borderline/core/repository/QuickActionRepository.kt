package com.borderline.core.repository

import android.content.Context
import com.borderline.core.models.ContextType
import com.borderline.core.models.QuickActionContent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

interface QuickActionRepository {
    fun getAll(): List<QuickActionContent>
    fun getSuggestedForContext(context: ContextType): List<QuickActionContent>
    fun add(action: QuickActionContent)
    fun update(action: QuickActionContent)
    fun delete(id: String)
    fun recordUsage(id: String)
}

class JsonQuickActionRepository(
    private val context: Context
) : QuickActionRepository {

    private val prefs = context.getSharedPreferences("quick_actions", Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun getAll(): List<QuickActionContent> {
        return try {
            val json = prefs.getString("actions", "[]").orEmpty()
            val type = object : TypeToken<List<QuickActionContent>>() {}.type
            gson.fromJson<List<QuickActionContent>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun getSuggestedForContext(context: ContextType): List<QuickActionContent> {
        return getAll()
            .filter {
                context == ContextType.UNKNOWN ||
                    it.contextTypes.contains(context) ||
                    it.contextTypes.contains(ContextType.UNKNOWN)
            }
            .sortedWith(
                compareByDescending<QuickActionContent> { it.priority }
                    .thenByDescending { it.triggeredCount }
                    .thenByDescending { it.lastUsedAt ?: 0L }
            )
            .take(4)
    }

    override fun add(action: QuickActionContent) {
        val actions = getAll().toMutableList()
        actions.add(action)
        save(actions)
    }

    override fun update(action: QuickActionContent) {
        val actions = getAll().toMutableList()
        val index = actions.indexOfFirst { it.id == action.id }
        if (index >= 0) {
            actions[index] = action
            save(actions)
        }
    }

    override fun delete(id: String) {
        val actions = getAll().toMutableList()
        actions.removeAll { it.id == id }
        save(actions)
    }

    override fun recordUsage(id: String) {
        val actions = getAll().toMutableList()
        val index = actions.indexOfFirst { it.id == id }
        if (index >= 0) {
            val action = actions[index]
            actions[index] = action.copy(
                triggeredCount = action.triggeredCount + 1,
                lastUsedAt = System.currentTimeMillis()
            )
            save(actions)
        }
    }

    private fun save(actions: List<QuickActionContent>) {
        prefs.edit().putString("actions", gson.toJson(actions)).apply()
    }
}
