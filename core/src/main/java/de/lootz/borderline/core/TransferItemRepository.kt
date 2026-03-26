package de.lootz.borderline.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for clipboard [TransferItem] entries.
 */
interface TransferItemRepository {
    val items: StateFlow<List<TransferItem>>
    suspend fun add(item: TransferItem)
    suspend fun pin(id: String, pinned: Boolean)
    suspend fun delete(id: String)
    suspend fun clear()
}
