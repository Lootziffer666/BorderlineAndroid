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
}
