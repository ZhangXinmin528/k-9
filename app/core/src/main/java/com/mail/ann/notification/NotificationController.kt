package com.mail.ann.notification

import com.mail.ann.Account
import com.mail.ann.controller.MessageReference
import com.mail.ann.mailstore.LocalFolder
import com.mail.ann.mailstore.LocalMessage

class NotificationController internal constructor(
    private val certificateErrorNotificationController: CertificateErrorNotificationController,
    private val authenticationErrorNotificationController: AuthenticationErrorNotificationController,
    private val syncNotificationController: SyncNotificationController,
    private val sendFailedNotificationController: SendFailedNotificationController,
    private val newMailNotificationController: NewMailNotificationController
) {
    fun showCertificateErrorNotification(account: Account, incoming: Boolean) {
        certificateErrorNotificationController.showCertificateErrorNotification(account, incoming)
    }

    fun clearCertificateErrorNotifications(account: Account, incoming: Boolean) {
        certificateErrorNotificationController.clearCertificateErrorNotifications(account, incoming)
    }

    fun showAuthenticationErrorNotification(account: Account, incoming: Boolean) {
        authenticationErrorNotificationController.showAuthenticationErrorNotification(account, incoming)
    }

    fun clearAuthenticationErrorNotification(account: Account, incoming: Boolean) {
        authenticationErrorNotificationController.clearAuthenticationErrorNotification(account, incoming)
    }

    fun showSendingNotification(account: Account) {
        syncNotificationController.showSendingNotification(account)
    }

    fun clearSendingNotification(account: Account) {
        syncNotificationController.clearSendingNotification(account)
    }

    fun showSendFailedNotification(account: Account, exception: Exception) {
        sendFailedNotificationController.showSendFailedNotification(account, exception)
    }

    fun clearSendFailedNotification(account: Account) {
        sendFailedNotificationController.clearSendFailedNotification(account)
    }

    fun showFetchingMailNotification(account: Account, folder: LocalFolder) {
        syncNotificationController.showFetchingMailNotification(account, folder)
    }

    fun showEmptyFetchingMailNotification(account: Account) {
        syncNotificationController.showEmptyFetchingMailNotification(account)
    }

    fun clearFetchingMailNotification(account: Account) {
        syncNotificationController.clearFetchingMailNotification(account)
    }

    fun restoreNewMailNotifications(accounts: List<Account>) {
        newMailNotificationController.restoreNewMailNotifications(accounts)
    }

    fun addNewMailNotification(account: Account, message: LocalMessage, silent: Boolean) {
        newMailNotificationController.addNewMailNotification(account, message, silent)
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference) {
        newMailNotificationController.removeNewMailNotifications(account, clearNewMessageState = true) {
            listOf(messageReference)
        }
    }

    fun clearNewMailNotifications(account: Account, selector: (List<MessageReference>) -> List<MessageReference>) {
        newMailNotificationController.removeNewMailNotifications(account, clearNewMessageState = false, selector)
    }

    fun clearNewMailNotifications(account: Account, clearNewMessageState: Boolean) {
        newMailNotificationController.clearNewMailNotifications(account, clearNewMessageState)
    }
}
