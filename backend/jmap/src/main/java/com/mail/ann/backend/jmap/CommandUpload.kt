package com.mail.ann.backend.jmap

import com.mail.ann.mail.Message
import com.mail.ann.mail.MessagingException
import com.squareup.moshi.Moshi
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import rs.ltt.jmap.client.JmapClient
import rs.ltt.jmap.client.http.HttpAuthentication
import rs.ltt.jmap.common.entity.EmailImport
import rs.ltt.jmap.common.method.call.email.ImportEmailMethodCall
import rs.ltt.jmap.common.method.response.email.ImportEmailMethodResponse
import timber.log.Timber

class CommandUpload(
    private val jmapClient: JmapClient,
    private val okHttpClient: OkHttpClient,
    private val httpAuthentication: HttpAuthentication,
    private val accountId: String
) {
    private val moshi = Moshi.Builder().build()

    fun uploadMessage(folderServerId: String, message: Message): String? {
        Timber.d("Uploading message to $folderServerId")

        val uploadResponse = uploadMessageAsBlob(message)
        return importEmailBlob(uploadResponse, folderServerId)
    }

    private fun uploadMessageAsBlob(message: Message): JmapUploadResponse {
        val session = jmapClient.session.get()
        val uploadUrl = session.getUploadUrl(accountId)

        val request = Request.Builder()
            .url(uploadUrl)
            .post(MessageRequestBody(message))
            .apply {
                httpAuthentication.authenticate(this)
            }
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw MessagingException("Uploading message as blob failed")
            }

            response.body!!.source().use { source ->
                val adapter = moshi.adapter(JmapUploadResponse::class.java)
                val uploadResponse = adapter.fromJson(source)
                uploadResponse ?: throw MessagingException("Error reading upload response")
            }
        }
    }

    private fun importEmailBlob(uploadResponse: JmapUploadResponse, folderServerId: String): String? {
        val importEmailRequest = ImportEmailMethodCall.builder()
            .accountId(accountId)
            .email(
                LOCAL_EMAIL_ID,
                EmailImport.builder()
                    .blobId(uploadResponse.blobId)
                    .keywords(mapOf("\$seen" to true))
                    .mailboxIds(mapOf(folderServerId to true))
                    .build()
            )
            .build()

        val importEmailCall = jmapClient.call(importEmailRequest)
        val importEmailResponse = importEmailCall.getMainResponseBlocking<ImportEmailMethodResponse>()

        return importEmailResponse.serverEmailId
    }

    private val ImportEmailMethodResponse.serverEmailId
        get() = created?.get(LOCAL_EMAIL_ID)?.id

    companion object {
        private const val LOCAL_EMAIL_ID = "t1"
    }
}

private class MessageRequestBody(private val message: Message) : RequestBody() {
    override fun contentType(): MediaType? {
        return "message/rfc822".toMediaType()
    }

    override fun contentLength(): Long {
        return message.calculateSize()
    }

    override fun writeTo(sink: BufferedSink) {
        message.writeTo(sink.outputStream())
    }
}
