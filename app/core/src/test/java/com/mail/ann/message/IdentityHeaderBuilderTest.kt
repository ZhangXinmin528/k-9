package com.mail.ann.message

import com.mail.ann.Account.QuoteStyle
import com.mail.ann.Identity
import com.mail.ann.RobolectricTest
import com.mail.ann.mail.internet.MimeHeaderChecker
import com.mail.ann.mail.internet.TextBody
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val IDENTITY_HEADER = "X-K9mail-Identity"

class IdentityHeaderBuilderTest : RobolectricTest() {
    @Test
    fun `valid unstructured header field value`() {
        val signature = "a".repeat(1000)

        val identityHeader = IdentityHeaderBuilder()
            .setCursorPosition(0)
            .setIdentity(createIdentity(signatureUse = true))
            .setIdentityChanged(false)
            .setMessageFormat(SimpleMessageFormat.TEXT)
            .setMessageReference(null)
            .setQuotedHtmlContent(null)
            .setQuoteStyle(QuoteStyle.PREFIX)
            .setQuoteTextMode(QuotedTextMode.NONE)
            .setSignature(signature)
            .setSignatureChanged(true)
            .setBody(TextBody("irrelevant"))
            .setBodyPlain(null)
            .build()

        assertThat(identityHeader.length).isGreaterThan(1000)
        assertIsValidHeader(identityHeader)
    }

    private fun assertIsValidHeader(identityHeader: String) {
        try {
            MimeHeaderChecker.checkHeader(IDENTITY_HEADER, identityHeader)
        } catch (e: Exception) {
            println("$IDENTITY_HEADER: $identityHeader")
            throw e
        }
    }

    private fun createIdentity(
        description: String? = null,
        name: String? = null,
        email: String? = null,
        signature: String? = null,
        signatureUse: Boolean = false,
        replyTo: String? = null
    ): Identity {
        return Identity(description, name, email, signature, signatureUse, replyTo)
    }
}
