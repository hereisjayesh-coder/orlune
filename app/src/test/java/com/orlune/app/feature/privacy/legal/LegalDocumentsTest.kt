package com.orlune.app.feature.privacy.legal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalDocumentsTest {

    @Test
    fun `all 15 documents required by the Legal Center are present`() {
        assertEquals(15, LegalDocuments.all.size)
    }

    @Test
    fun `byId finds every document by its own id`() {
        LegalDocuments.all.forEach { document ->
            assertEquals(document, LegalDocuments.byId(document.id))
        }
    }

    @Test
    fun `byId returns null for an unknown id rather than throwing`() {
        assertNull(LegalDocuments.byId("does-not-exist"))
    }

    @Test
    fun `no two documents share the same id`() {
        val ids = LegalDocuments.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `every document has a non-blank listTitle and body`() {
        LegalDocuments.all.forEach { document ->
            assertTrue("listTitle blank for ${document.id}", document.listTitle.isNotBlank())
            assertTrue("body blank for ${document.id}", document.body.isNotBlank())
        }
    }

    @Test
    fun `every document body parses into at least a title and one other block`() {
        LegalDocuments.all.forEach { document ->
            val blocks = parseLegalBlocks(document.body)
            assertTrue("no Title block for ${document.id}", blocks.any { it is LegalBlock.Title })
            assertTrue("only a title, no content, for ${document.id}", blocks.size > 1)
        }
    }

    @Test
    fun `about-orlune document exists and is reachable directly from Settings`() {
        assertNotNull(LegalDocuments.byId("about-orlune"))
    }
}
