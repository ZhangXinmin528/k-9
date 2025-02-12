package com.mail.ann.backend.pop3

import com.mail.ann.backend.api.Backend
import com.mail.ann.backend.api.BackendPusher
import com.mail.ann.backend.api.BackendPusherCallback
import com.mail.ann.backend.api.BackendStorage
import com.mail.ann.backend.api.SyncConfig
import com.mail.ann.backend.api.SyncListener
import com.mail.ann.mail.BodyFactory
import com.mail.ann.mail.Flag
import com.mail.ann.mail.Message
import com.mail.ann.mail.Part
import com.mail.ann.mail.store.pop3.Pop3Store
import com.mail.ann.mail.transport.smtp.SmtpTransport

class Pop3Backend(
    accountName: String,
    backendStorage: BackendStorage,
    private val pop3Store: Pop3Store,
    private val smtpTransport: SmtpTransport
) : Backend {
    private val pop3Sync: Pop3Sync = Pop3Sync(accountName, backendStorage, pop3Store)
    private val commandRefreshFolderList = CommandRefreshFolderList(backendStorage)
    private val commandSetFlag = CommandSetFlag(pop3Store)
    private val commandDownloadMessage = CommandDownloadMessage(backendStorage, pop3Store)

    override val supportsFlags = false
    override val supportsExpunge = false
    override val supportsMove = false
    override val supportsCopy = false
    override val supportsUpload = false
    override val supportsTrashFolder = false
    override val supportsSearchByDate = false
    override val isPushCapable = false
    override val isDeleteMoveToTrash = false

    override fun refreshFolderList() {
        commandRefreshFolderList.refreshFolderList()
    }

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener) {
        pop3Sync.sync(folder, syncConfig, listener)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadMessageStructure(folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        commandDownloadMessage.downloadCompleteMessage(folderServerId, messageServerId)
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        commandSetFlag.setFlag(folderServerId, messageServerIds, flag, newState)
    }

    override fun markAllAsRead(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        throw UnsupportedOperationException("not supported")
    }

    override fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        commandSetFlag.setFlag(folderServerId, messageServerIds, Flag.DELETED, true)
    }

    override fun deleteAllMessages(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun moveMessagesAndMarkAsRead(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun search(
        folderServerId: String,
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean
    ): List<String> {
        throw UnsupportedOperationException("not supported")
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        throw UnsupportedOperationException("not supported")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        return null
    }

    override fun uploadMessage(folderServerId: String, message: Message): String? {
        throw UnsupportedOperationException("not supported")
    }

    override fun checkIncomingServerSettings() {
        pop3Store.checkSettings()
    }

    override fun sendMessage(message: Message) {
        smtpTransport.sendMessage(message)
    }

    override fun checkOutgoingServerSettings() {
        smtpTransport.checkSettings()
    }

    override fun createPusher(callback: BackendPusherCallback): BackendPusher {
        throw UnsupportedOperationException("not implemented")
    }
}
