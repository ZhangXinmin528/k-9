package com.mail.ann.autocrypt

import com.mail.ann.Ann
import com.mail.ann.mail.Address
import com.mail.ann.mail.Flag
import com.mail.ann.mail.Message
import com.mail.ann.mail.MessagingException
import com.mail.ann.mail.internet.MimeBodyPart
import com.mail.ann.mail.internet.MimeHeader
import com.mail.ann.mail.internet.MimeMessage
import com.mail.ann.mail.internet.MimeMessageHelper
import com.mail.ann.mail.internet.MimeMultipart
import com.mail.ann.mail.internet.TextBody
import com.mail.ann.mailstore.BinaryMemoryBody
import java.util.Date

class AutocryptTransferMessageCreator(private val stringProvider: AutocryptStringProvider) {
    fun createAutocryptTransferMessage(data: ByteArray, address: Address): Message {
        try {
            val subjectText = stringProvider.transferMessageSubject()
            val messageText = stringProvider.transferMessageBody()

            val textBodyPart = MimeBodyPart.create(TextBody(messageText))
            val dataBodyPart = MimeBodyPart.create(BinaryMemoryBody(data, "7bit"))
            dataBodyPart.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "application/autocrypt-setup")
            dataBodyPart.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, "attachment; filename=\"autocrypt-setup-message\"")

            val messageBody = MimeMultipart.newInstance()
            messageBody.addBodyPart(textBodyPart)
            messageBody.addBodyPart(dataBodyPart)

            val message = MimeMessage.create()
            MimeMessageHelper.setBody(message, messageBody)

            val nowDate = Date()

            message.setFlag(Flag.X_DOWNLOADED_FULL, true)
            message.subject = subjectText
            message.setHeader("Autocrypt-Setup-Message", "v1")
            message.internalDate = nowDate
            message.addSentDate(nowDate, Ann.isHideTimeZone)
            message.setFrom(address)
            message.setHeader("To", address.toEncodedString())

            return message
        } catch (e: MessagingException) {
            throw AssertionError(e)
        }
    }
}
