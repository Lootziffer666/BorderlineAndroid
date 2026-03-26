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
}
