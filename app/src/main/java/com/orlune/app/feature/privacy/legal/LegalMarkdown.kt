package com.orlune.app.feature.privacy.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Parsed representation of LegalDocument.body's small markdown subset. Pure Kotlin
 * so the parsing logic (parseLegalBlocks) is unit-testable without Compose. */
sealed class LegalBlock {
    data class Title(val text: String) : LegalBlock()
    data class Heading(val text: String) : LegalBlock()
    data class SubHeading(val text: String) : LegalBlock()
    data class Paragraph(val text: String) : LegalBlock()
    data class BulletList(val items: List<String>) : LegalBlock()
}

/**
 * Line-based parser: "# " is the document title, "## "/"### " are section headers,
 * "- " lines accumulate into a bullet list, blank lines end the current paragraph
 * or bullet list, and consecutive plain lines join into one wrapped paragraph.
 * Deliberately line-based rather than blank-line-block-based — a header is never
 * followed by a blank line before its own body text in LegalDocument.body, so a
 * block-based split would merge a header and its paragraph into one mis-styled run.
 */
fun parseLegalBlocks(markdown: String): List<LegalBlock> {
    val blocks = mutableListOf<LegalBlock>()
    val paragraphLines = mutableListOf<String>()
    val bulletItems = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isNotEmpty()) {
            blocks += LegalBlock.Paragraph(paragraphLines.joinToString(" "))
            paragraphLines.clear()
        }
    }

    fun flushBullets() {
        if (bulletItems.isNotEmpty()) {
            blocks += LegalBlock.BulletList(bulletItems.toList())
            bulletItems.clear()
        }
    }

    markdown.trimIndent().lines().forEach { rawLine ->
        val line = rawLine.trim()
        when {
            line.isEmpty() -> {
                flushParagraph()
                flushBullets()
            }
            line.startsWith("# ") -> {
                flushParagraph(); flushBullets()
                blocks += LegalBlock.Title(line.removePrefix("# "))
            }
            line.startsWith("### ") -> {
                flushParagraph(); flushBullets()
                blocks += LegalBlock.SubHeading(line.removePrefix("### "))
            }
            line.startsWith("## ") -> {
                flushParagraph(); flushBullets()
                blocks += LegalBlock.Heading(line.removePrefix("## "))
            }
            line.startsWith("- ") -> {
                flushParagraph()
                bulletItems += line.removePrefix("- ")
            }
            else -> {
                flushBullets()
                paragraphLines += line
            }
        }
    }
    flushParagraph()
    flushBullets()
    return blocks
}

@Composable
fun LegalDocumentBody(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseLegalBlocks(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is LegalBlock.Title -> Text(
                    block.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                is LegalBlock.Heading -> Text(
                    block.text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                is LegalBlock.SubHeading -> Text(
                    block.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                is LegalBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
                is LegalBlock.BulletList -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.items.forEach { item ->
                        Text("•  $item", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
