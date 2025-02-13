package com.mail.ann.view

import com.mail.ann.RobolectricTest
import com.mail.ann.mail.Address
import com.mail.ann.mail.internet.MimeMessage
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MessageHeaderTest : RobolectricTest() {

    @Test
    fun shouldShowSender_withSender_shouldReturnTrue() {
        val message = createMessage("from@example1.com", "sender@example2.com")

        val showSender = MessageHeader.shouldShowSender(message)

        assertThat(showSender).isTrue()
    }

    @Test
    fun shouldShowSender_withoutSender_shouldReturnFalse() {
        val message = createMessage("from@example1.com")

        val showSender = MessageHeader.shouldShowSender(message)

        assertThat(showSender).isFalse()
    }

    private fun createMessage(from: String, sender: String? = null) = MimeMessage().apply {
        setFrom(from.toAddress())
        setSender(sender?.toAddress())
    }

    private fun String.toAddress() = Address.parse(this)[0]
}
