package de.lootz.borderline.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists [Snippet] items via SharedPreferences JSON serialisation.
 * Intended as a first persistence layer that can later migrate to Room.
 */
class JsonSnippetRepository(context: Context) : SnippetRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _snippets = MutableStateFlow(loadAll())
    override val snippets: StateFlow<List<Snippet>> = _snippets.asStateFlow()

    override suspend fun add(snippet: Snippet) {
        val list = _snippets.value.toMutableList()
        list.add(snippet)
        persist(list)
    }

    override suspend fun update(snippet: Snippet) {
        val list = _snippets.value.map { if (it.id == snippet.id) snippet else it }
        persist(list)
    }

    override suspend fun delete(id: String) {
        val list = _snippets.value.filter { it.id != id }
        persist(list)
    }

    override suspend fun getById(id: String): Snippet? = _snippets.value.find { it.id == id }

    override suspend fun search(query: String): List<Snippet> {
        val q = query.lowercase()
        return _snippets.value.filter {
            it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
        }
    }

    /* ── seed helpers ────────────────────────────────── */

    fun seedDefaults(defaults: List<Snippet>) {
        if (_snippets.value.isEmpty()) {
            persist(defaults)
        }
    }

    /* ── JSON persistence ────────────────────────────── */

    private fun persist(list: List<Snippet>) {
        val json = JSONArray()
        list.forEach { s ->
            json.put(JSONObject().apply {
                put(KEY_ID, s.id)
                put(KEY_TITLE, s.title)
                put(KEY_CONTENT, s.content)
                put(KEY_CATEGORY, s.category)
                put(KEY_CREATED, s.createdAt)
            })
        }
        prefs.edit().putString(KEY_DATA, json.toString()).apply()
        _snippets.value = list
    }

    private fun loadAll(): List<Snippet> {
        val raw = prefs.getString(KEY_DATA, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Snippet(
                    id = obj.getString(KEY_ID),
                    title = obj.getString(KEY_TITLE),
                    content = obj.getString(KEY_CONTENT),
                    category = obj.optString(KEY_CATEGORY, ""),
                    createdAt = obj.optLong(KEY_CREATED, System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            BorderlineLogger.w("Failed to load snippets: ${e.message}")
            emptyList()
        }
    }

    companion object {
        private const val PREFS_NAME = "borderline_snippets"
        private const val KEY_DATA = "snippets_json"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_CONTENT = "content"
        private const val KEY_CATEGORY = "category"
        private const val KEY_CREATED = "created"
    }
}
