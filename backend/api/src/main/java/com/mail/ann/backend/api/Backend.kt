package com.mail.ann.backend.api

import com.mail.ann.mail.BodyFactory
import com.mail.ann.mail.Flag
import com.mail.ann.mail.Message
import com.mail.ann.mail.MessagingException
import com.mail.ann.mail.Part

interface Backend {
    val supportsFlags: Boolean
    val supportsExpunge: Boolean
    val supportsMove: Boolean
    val supportsCopy: Boolean
    val supportsUpload: Boolean
    val supportsTrashFolder: Boolean
    val supportsSearchByDate: Boolean
    val isPushCapable: Boolean
    val isDeleteMoveToTrash: Boolean

    @Throws(MessagingException::class)
    fun refreshFolderList()

    // TODO: Add a way to cancel the sync process
    fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener)

    @Throws(MessagingException::class)
    fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String)

    @Throws(MessagingException::class)
    fun downloadMessageStructure(folderServerId: String, messageServerId: String)

    @Throws(MessagingException::class)
    fun downloadCompleteMessage(folderServerId: String, messageServerId: String)

    @Throws(MessagingException::class)
    fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean)

    @Throws(MessagingException::class)
    fun markAllAsRead(folderServerId: String)

    @Throws(MessagingException::class)
    fun expunge(folderServerId: String)

    @Throws(MessagingException::class)
    fun expungeMessages(folderServerId: String, messageServerIds: List<String>)

    @Throws(MessagingException::class)
    fun deleteMessages(folderServerId: String, messageServerIds: List<String>)

    @Throws(MessagingException::class)
    fun deleteAllMessages(folderServerId: String)

    @Throws(MessagingException::class)
    fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>
    ): Map<String, String>?

    @Throws(MessagingException::class)
    fun moveMessagesAndMarkAsRead(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>
    ): Map<String, String>?

    @Throws(MessagingException::class)
    fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>
    ): Map<String, String>?

    @Throws(MessagingException::class)
    fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean
    ): List<String>

    @Throws(MessagingException::class)
    fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory)

    @Throws(MessagingException::class)
    fun findByMessageId(folderServerId: String, messageId: String): String?

    @Throws(MessagingException::class)
    fun uploadMessage(folderServerId: String, message: Message): String?

    @Throws(MessagingException::class)
    fun checkIncomingServerSettings()

    @Throws(MessagingException::class)
    fun sendMessage(message: Message)

    @Throws(MessagingException::class)
    fun checkOutgoingServerSettings()

    fun createPusher(callback: BackendPusherCallback): BackendPusher
}
