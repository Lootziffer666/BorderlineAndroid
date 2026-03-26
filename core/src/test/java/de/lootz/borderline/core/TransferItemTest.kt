package de.lootz.borderline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferItemTest {

    @Test
    fun defaultKindIsText() {
        val item = TransferItem(label = "lab", preview = "prev")
        assertEquals(TransferItem.Kind.TEXT, item.kind)
    }

    @Test
    fun defaultPinnedIsFalse() {
        val item = TransferItem(label = "lab", preview = "prev")
        assertFalse(item.pinned)
    }

    @Test
    fun defaultIdIsUnique() {
        val a = TransferItem(label = "A", preview = "a")
        val b = TransferItem(label = "B", preview = "b")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun copyWithPinnedRetainsOtherFields() {
        val item = TransferItem(label = "L", preview = "P", kind = TransferItem.Kind.URI)
        val pinned = item.copy(pinned = true)
        assertTrue(pinned.pinned)
        assertEquals(item.id, pinned.id)
        assertEquals(TransferItem.Kind.URI, pinned.kind)
    }

    @Test
    fun timestampIsNonZero() {
        val item = TransferItem(label = "L", preview = "P")
        assertTrue(item.timestamp > 0)
    }

    @Test
    fun customIdIsPreserved() {
        val item = TransferItem(id = "custom-id", label = "L", preview = "P")
        assertEquals("custom-id", item.id)
    }

    @Test
    fun kindEnumValues() {
        assertEquals(2, TransferItem.Kind.entries.size)
        assertEquals(TransferItem.Kind.TEXT, TransferItem.Kind.valueOf("TEXT"))
        assertEquals(TransferItem.Kind.URI, TransferItem.Kind.valueOf("URI"))
    }

    @Test
    fun unpinRetainsContent() {
        val item = TransferItem(label = "L", preview = "P", pinned = true)
        val unpinned = item.copy(pinned = false)
        assertFalse(unpinned.pinned)
        assertEquals(item.id, unpinned.id)
        assertEquals("L", unpinned.label)
        assertEquals("P", unpinned.preview)
    }
}
