package de.lootz.borderline.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists [TransferItem] entries via SharedPreferences JSON serialisation.
 * Keeps at most [MAX_ITEMS] unpinned items (pinned items are always retained).
 */
class JsonTransferItemRepository(context: Context) : TransferItemRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _items = MutableStateFlow(loadAll())
    override val items: StateFlow<List<TransferItem>> = _items.asStateFlow()

    override suspend fun add(item: TransferItem) {
        val list = _items.value.toMutableList()
        // avoid exact duplicates (same preview, different id)
        list.removeAll { it.id != item.id && it.preview == item.preview && !it.pinned }
        list.add(0, item)
        // trim unpinned items beyond MAX_ITEMS
        val pinned = list.filter { it.pinned }
        val unpinned = list.filter { !it.pinned }.take(MAX_ITEMS)
        persist(pinned + unpinned)
    }

    override suspend fun pin(id: String, pinned: Boolean) {
        val list = _items.value.map { if (it.id == id) it.copy(pinned = pinned) else it }
        persist(list)
    }

    override suspend fun delete(id: String) {
        val list = _items.value.filter { it.id != id }
        persist(list)
    }

    override suspend fun clear() {
        persist(_items.value.filter { it.pinned })
    }

    /* ── JSON persistence ────────────────────────────── */

    private fun persist(list: List<TransferItem>) {
        val json = JSONArray()
        list.forEach { t ->
            json.put(JSONObject().apply {
                put(KEY_ID, t.id)
                put(KEY_KIND, t.kind.name)
                put(KEY_LABEL, t.label)
                put(KEY_PREVIEW, t.preview)
                put(KEY_TS, t.timestamp)
                put(KEY_PINNED, t.pinned)
            })
        }
        prefs.edit().putString(KEY_DATA, json.toString()).apply()
        _items.value = list
    }

    private fun loadAll(): List<TransferItem> {
        val raw = prefs.getString(KEY_DATA, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TransferItem(
                    id = obj.getString(KEY_ID),
                    kind = TransferItem.Kind.valueOf(obj.optString(KEY_KIND, "TEXT")),
                    label = obj.optString(KEY_LABEL, ""),
                    preview = obj.optString(KEY_PREVIEW, ""),
                    timestamp = obj.optLong(KEY_TS, System.currentTimeMillis()),
                    pinned = obj.optBoolean(KEY_PINNED, false)
                )
            }
        } catch (e: Exception) {
            BorderlineLogger.w("Failed to load transfer items: ${e.message}")
            emptyList()
        }
    }

    companion object {
        private const val PREFS_NAME = "borderline_transfer_items"
        private const val KEY_DATA = "items_json"
        private const val KEY_ID = "id"
        private const val KEY_KIND = "kind"
        private const val KEY_LABEL = "label"
        private const val KEY_PREVIEW = "preview"
        private const val KEY_TS = "ts"
        private const val KEY_PINNED = "pinned"
        private const val MAX_ITEMS = 20
    }
}
