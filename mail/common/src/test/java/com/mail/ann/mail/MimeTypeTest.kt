package com.mail.ann.mail

import com.mail.ann.mail.MimeType.Companion.toMimeType
import com.mail.ann.mail.MimeType.Companion.toMimeTypeOrNull
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test

class MimeTypeTest {
    @Test
    fun commonTypes() {
        assertParsedMimeType("text/plain", type = "text", subtype = "plain")
        assertParsedMimeType("text/html", type = "text", subtype = "html")
        assertParsedMimeType("application/octet-stream", type = "application", subtype = "octet-stream")
        assertParsedMimeType("message/rfc822", type = "message", subtype = "rfc822")
        assertParsedMimeType("message/global", type = "message", subtype = "global")
        assertParsedMimeType("multipart/alternative", type = "multipart", subtype = "alternative")
        assertParsedMimeType("multipart/mixed", type = "multipart", subtype = "mixed")
        assertParsedMimeType("multipart/encrypted", type = "multipart", subtype = "encrypted")
    }

    @Test
    fun checkListOfMimeTypes() {
        // TODO: Try to parse all IANA-registered media types
        //  https://www.iana.org/assignments/media-types/media-types.xhtml
    }

    @Test
    fun lowerCasing() {
        assertParsedMimeType("text/plain", type = "text", subtype = "plain")
        assertParsedMimeType("text/PLAIN", type = "text", subtype = "plain")
        assertParsedMimeType("TEXT/plain", type = "text", subtype = "plain")
        assertParsedMimeType("TEXT/PLAIN", type = "text", subtype = "plain")
        assertParsedMimeType("TeXt/pLaIn", type = "text", subtype = "plain")
        assertParsedMimeType("APPLICATION/OCTET-STREAM", type = "application", subtype = "octet-stream")
    }

    @Test
    fun invalidMimeTypes() {
        assertInvalidMimeType("")
        assertInvalidMimeType("text")
        assertInvalidMimeType("text plain")
        assertInvalidMimeType("image/ png")
        assertInvalidMimeType("message /rfc822")
        assertInvalidMimeType("application/something(odd)")
    }

    @Test
    fun invalidMimeTypesReturnNull() {
        assertInvalidMimeTypeReturnsNull("")
        assertInvalidMimeTypeReturnsNull("text")
        assertInvalidMimeTypeReturnsNull("text plain")
        assertInvalidMimeTypeReturnsNull("image/ png")
        assertInvalidMimeTypeReturnsNull("message /rfc822")
        assertInvalidMimeTypeReturnsNull("application/something(odd)")
    }

    private fun assertParsedMimeType(input: String, type: String, subtype: String) {
        val mimeType = input.toMimeType()

        assertThat(mimeType.type).isEqualTo(type)
        assertThat(mimeType.subtype).isEqualTo(subtype)
    }

    private fun assertInvalidMimeType(input: String) {
        try {
            input.toMimeType()
            fail("Expected exception")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).startsWith("Invalid MIME type: ")
        }
    }

    private fun assertInvalidMimeTypeReturnsNull(input: String) {
        assertThat(input.toMimeTypeOrNull()).isNull()
    }
}
