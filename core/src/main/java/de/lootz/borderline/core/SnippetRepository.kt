package de.lootz.borderline.core

import kotlinx.coroutines.flow.StateFlow

/**
 * CRUD repository for [Snippet] items.
 */
interface SnippetRepository {
    val snippets: StateFlow<List<Snippet>>
    suspend fun add(snippet: Snippet)
    suspend fun update(snippet: Snippet)
    suspend fun delete(id: String)
    suspend fun getById(id: String): Snippet?
    suspend fun search(query: String): List<Snippet>
}
