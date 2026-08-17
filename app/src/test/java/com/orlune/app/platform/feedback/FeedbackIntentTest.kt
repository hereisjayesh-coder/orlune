package com.orlune.app.platform.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Only the plain-Kotlin recipient/subject/body constants are exercised here — building
 * the actual android.content.Intent (FeedbackIntent.compose()) touches the Android
 * framework, which isn't available under plain JVM unit tests in this project (no
 * Robolectric, see AGENTS.MD conventions); that half is verified on-device instead.
 */
class FeedbackIntentTest {

    @Test
    fun `recipient is the Orlune feedback address`() {
        assertEquals("dallemahesh09@gmail.com", FeedbackIntent.RECIPIENT)
    }

    @Test
    fun `subject is the suggested Orlune Feedback subject`() {
        assertEquals("Orlune Feedback", FeedbackIntent.SUBJECT)
    }

    @Test
    fun `body is prefilled with the suggested template`() {
        val body = FeedbackIntent.BODY
        assertTrue(body.startsWith("Hello Orlune team,"))
        assertTrue(body.contains("I would like to share the following feedback:"))
        assertTrue(body.contains("[User writes here]"))
        assertTrue(body.contains("Device / Android version:"))
        assertTrue(body.contains("[optional]"))
        assertTrue(body.trimEnd().endsWith("Thank you."))
    }

    @Test
    fun `body does not contain any real device identifiers`() {
        val body = FeedbackIntent.BODY
        // The device-info line must stay an optional placeholder the user fills in
        // themselves — Orlune must never auto-populate it with real values.
        assertFalse(body.contains(Regex("android\\.os\\.Build|Build\\.MODEL|Build\\.VERSION")))
    }
}
