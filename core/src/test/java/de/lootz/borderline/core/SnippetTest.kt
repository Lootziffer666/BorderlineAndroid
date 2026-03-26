package de.lootz.borderline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetTest {

    @Test
    fun defaultIdIsUnique() {
        val a = Snippet(title = "A", content = "text a")
        val b = Snippet(title = "B", content = "text b")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun defaultCategoryIsEmpty() {
        val snippet = Snippet(title = "T", content = "C")
        assertEquals("", snippet.category)
    }

    @Test
    fun createdAtIsNonZero() {
        val snippet = Snippet(title = "T", content = "C")
        assertTrue(snippet.createdAt > 0)
    }

    @Test
    fun copyPreservesId() {
        val original = Snippet(title = "T", content = "C")
        val updated = original.copy(title = "New Title")
        assertEquals(original.id, updated.id)
        assertEquals("New Title", updated.title)
        assertEquals("C", updated.content)
    }

    @Test
    fun copyWithCategory() {
        val snippet = Snippet(title = "T", content = "C", category = "prompt")
        assertEquals("prompt", snippet.category)
        val updated = snippet.copy(category = "code")
        assertEquals("code", updated.category)
    }

    @Test
    fun customIdIsPreserved() {
        val snippet = Snippet(id = "custom-id", title = "T", content = "C")
        assertEquals("custom-id", snippet.id)
    }
}
