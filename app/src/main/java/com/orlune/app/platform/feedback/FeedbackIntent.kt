package com.orlune.app.platform.feedback

import android.content.Intent
import android.net.Uri

/**
 * Feedback is handed off entirely to the user's own email app — Orlune never
 * collects, stores, transmits, or even reads feedback content itself (Section 10,
 * Privacy Architecture: no backend, no INTERNET permission, no analytics). ACTION_SENDTO
 * with a bare "mailto:" URI (rather than ACTION_SEND) scopes intent resolution to email
 * composers specifically, not the general share sheet. The body is a fixed template with
 * placeholder lines the user fills in by hand; nothing about the device is read or
 * appended automatically.
 */
object FeedbackIntent {

    const val RECIPIENT = "dallemahesh09@gmail.com"
    const val SUBJECT = "Orlune Feedback"

    val BODY = """
        Hello Orlune team,

        I would like to share the following feedback:

        [User writes here]

        Device / Android version:
        [optional]

        Thank you.
    """.trimIndent()

    fun compose(): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(RECIPIENT))
            putExtra(Intent.EXTRA_SUBJECT, SUBJECT)
            putExtra(Intent.EXTRA_TEXT, BODY)
        }
}
