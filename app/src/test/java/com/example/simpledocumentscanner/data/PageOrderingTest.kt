package com.example.simpledocumentscanner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PageOrderingTest {
    @Test
    fun movesPageToRequestedPosition() {
        assertEquals(listOf("B", "C", "A"), listOf("A", "B", "C").moved(0, 2))
    }

    @Test
    fun ignoresPositionOutsideList() {
        val pages = listOf("A", "B")

        assertSame(pages, pages.moved(0, 2))
        assertSame(pages, pages.moved(-1, 1))
    }
}
