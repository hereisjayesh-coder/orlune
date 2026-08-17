package com.orlune.app.feature.privacy.legal

import org.junit.Assert.assertEquals
import org.junit.Test

class LegalMarkdownTest {

    @Test
    fun `title line becomes a Title block`() {
        val blocks = parseLegalBlocks("# Privacy Policy")
        assertEquals(listOf(LegalBlock.Title("Privacy Policy")), blocks)
    }

    @Test
    fun `heading immediately followed by its paragraph, no blank line, parses as two separate blocks`() {
        val blocks = parseLegalBlocks(
            """
            ## Status
            This is a draft.
            """.trimIndent()
        )
        assertEquals(
            listOf(
                LegalBlock.Heading("Status"),
                LegalBlock.Paragraph("This is a draft.")
            ),
            blocks
        )
    }

    @Test
    fun `consecutive plain lines join into one wrapped paragraph`() {
        val blocks = parseLegalBlocks(
            """
            ## Section
            Line one of the paragraph.
            Line two continues it.
            """.trimIndent()
        )
        assertEquals(
            listOf(
                LegalBlock.Heading("Section"),
                LegalBlock.Paragraph("Line one of the paragraph. Line two continues it.")
            ),
            blocks
        )
    }

    @Test
    fun `bullet lines accumulate into one BulletList block`() {
        val blocks = parseLegalBlocks(
            """
            ## What Orlune does not do
            - No account
            - No cloud service
            - No analytics SDK
            """.trimIndent()
        )
        assertEquals(
            listOf(
                LegalBlock.Heading("What Orlune does not do"),
                LegalBlock.BulletList(listOf("No account", "No cloud service", "No analytics SDK"))
            ),
            blocks
        )
    }

    @Test
    fun `blank line ends a paragraph before the next heading`() {
        val blocks = parseLegalBlocks(
            """
            # Title

            ## First
            First body.

            ## Second
            Second body.
            """.trimIndent()
        )
        assertEquals(
            listOf(
                LegalBlock.Title("Title"),
                LegalBlock.Heading("First"),
                LegalBlock.Paragraph("First body."),
                LegalBlock.Heading("Second"),
                LegalBlock.Paragraph("Second body.")
            ),
            blocks
        )
    }

    @Test
    fun `subheading is distinct from heading`() {
        val blocks = parseLegalBlocks("### Usage Access (PACKAGE_USAGE_STATS)")
        assertEquals(listOf(LegalBlock.SubHeading("Usage Access (PACKAGE_USAGE_STATS)")), blocks)
    }

    @Test
    fun `a bullet list followed by a paragraph flushes the bullets first`() {
        val blocks = parseLegalBlocks(
            """
            - First
            - Second
            A trailing paragraph.
            """.trimIndent()
        )
        assertEquals(
            listOf(
                LegalBlock.BulletList(listOf("First", "Second")),
                LegalBlock.Paragraph("A trailing paragraph.")
            ),
            blocks
        )
    }
}
