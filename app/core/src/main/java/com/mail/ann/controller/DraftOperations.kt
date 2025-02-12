package com.mail.ann.controller

import com.mail.ann.Account
import com.mail.ann.Account.Expunge
import com.mail.ann.Ann
import com.mail.ann.backend.api.Backend
import com.mail.ann.controller.MessagingControllerCommands.PendingAppend
import com.mail.ann.controller.MessagingControllerCommands.PendingReplace
import com.mail.ann.mail.FetchProfile
import com.mail.ann.mail.Message
import com.mail.ann.mail.MessageDownloadState
import com.mail.ann.mail.MessagingException
import com.mail.ann.mailstore.LocalFolder
import com.mail.ann.mailstore.LocalMessage
import com.mail.ann.mailstore.MessageStoreManager
import com.mail.ann.mailstore.SaveMessageData
import com.mail.ann.mailstore.SaveMessageDataCreator
import org.jetbrains.annotations.NotNull
import timber.log.Timber

internal class DraftOperations(
    private val messagingController: @NotNull MessagingController,
    private val messageStoreManager: @NotNull MessageStoreManager,
    private val saveMessageDataCreator: SaveMessageDataCreator
) {

    fun saveDraft(
        account: Account,
        message: Message,
        existingDraftId: Long?,
        plaintextSubject: String?
    ): Long? {
        return try {
            val draftsFolderId = account.draftsFolderId ?: error("No Drafts folder configured")

            val messageId = if (messagingController.supportsUpload(account)) {
                saveAndUploadDraft(account, message, draftsFolderId, existingDraftId, plaintextSubject)
            } else {
                saveDraftLocally(account, message, draftsFolderId, existingDraftId, plaintextSubject)
            }

            messageId
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to save message as draft.")
            null
        }
    }

    private fun saveAndUploadDraft(
        account: Account,
        message: Message,
        folderId: Long,
        existingDraftId: Long?,
        subject: String?
    ): Long {
        val messageStore = messageStoreManager.getMessageStore(account)

        val messageId = messageStore.saveLocalMessage(folderId, message.toSaveMessageData(subject))

        val previousDraftMessage = if (existingDraftId != null) {
            val localStore = messagingController.getLocalStoreOrThrow(account)
            val localFolder = localStore.getFolder(folderId)
            localFolder.open()

            localFolder.getMessage(existingDraftId)
        } else {
            null
        }

        if (previousDraftMessage != null) {
            previousDraftMessage.delete()

            val deleteMessageId = previousDraftMessage.databaseId
            val command = PendingReplace.create(folderId, messageId, deleteMessageId)
            messagingController.queuePendingCommand(account, command)
        } else {
            val fakeMessageServerId = messageStore.getMessageServerId(messageId)
            val command = PendingAppend.create(folderId, fakeMessageServerId)
            messagingController.queuePendingCommand(account, command)
        }

        messagingController.processPendingCommands(account)

        return messageId
    }

    private fun saveDraftLocally(
        account: Account,
        message: Message,
        folderId: Long,
        existingDraftId: Long?,
        plaintextSubject: String?
    ): Long {
        val messageStore = messageStoreManager.getMessageStore(account)
        val messageData = message.toSaveMessageData(plaintextSubject)

        return messageStore.saveLocalMessage(folderId, messageData, existingDraftId)
    }

    fun processPendingReplace(command: PendingReplace, account: Account) {
        val localStore = messagingController.getLocalStoreOrThrow(account)
        val localFolder = localStore.getFolder(command.folderId)
        localFolder.open()

        val backend = messagingController.getBackend(account)

        val uploadMessageId = command.uploadMessageId
        val localMessage = localFolder.getMessage(uploadMessageId)
        if (localMessage == null) {
            Timber.w("Couldn't find local copy of message to upload [ID: %d]", uploadMessageId)
            return
        } else if (!localMessage.uid.startsWith(Ann.LOCAL_UID_PREFIX)) {
            Timber.i("Message [ID: %d] to be uploaded already has a server ID set. Skipping upload.", uploadMessageId)
        } else {
            uploadMessage(backend, account, localFolder, localMessage)
        }

        deleteMessage(backend, account, localFolder, command.deleteMessageId)
    }

    private fun uploadMessage(
        backend: Backend,
        account: Account,
        localFolder: LocalFolder,
        localMessage: LocalMessage
    ) {
        val folderServerId = localFolder.serverId
        Timber.d("Uploading message [ID: %d] to remote folder '%s'", localMessage.databaseId, folderServerId)

        val fetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.BODY)
        }
        localFolder.fetch(listOf(localMessage), fetchProfile, null)

        val messageServerId = backend.uploadMessage(folderServerId, localMessage)

        if (messageServerId == null) {
            Timber.w(
                "Failed to get a server ID for the uploaded message. Removing local copy [ID: %d]",
                localMessage.databaseId
            )
            localMessage.destroy()
        } else {
            val oldUid = localMessage.uid

            localMessage.uid = messageServerId
            localFolder.changeUid(localMessage)

            for (listener in messagingController.listeners) {
                listener.messageUidChanged(account, localFolder.databaseId, oldUid, localMessage.uid)
            }
        }
    }

    private fun deleteMessage(backend: Backend, account: Account, localFolder: LocalFolder, messageId: Long) {
        val messageServerId = localFolder.getMessageUidById(messageId) ?: run {
            Timber.i("Couldn't find local copy of message [ID: %d] to be deleted. Skipping delete.", messageId)
            return
        }

        val messageServerIds = listOf(messageServerId)
        val folderServerId = localFolder.serverId
        backend.deleteMessages(folderServerId, messageServerIds)

        if (backend.supportsExpunge && account.expungePolicy == Expunge.EXPUNGE_IMMEDIATELY) {
            backend.expungeMessages(folderServerId, messageServerIds)
        }

        messagingController.destroyPlaceholderMessages(localFolder, messageServerIds)
    }

    private fun Message.toSaveMessageData(subject: String?): SaveMessageData {
        return saveMessageDataCreator.createSaveMessageData(this, MessageDownloadState.FULL, subject)
    }
}
