package com.mail.ann.notification

import com.mail.ann.Account
import com.mail.ann.Ann
import com.mail.ann.Ann.LockScreenNotificationVisibility

private const val MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION = 5

internal class BaseNotificationDataCreator {

    fun createBaseNotificationData(notificationData: NotificationData): BaseNotificationData {
        val account = notificationData.account
        return BaseNotificationData(
            account = account,
            groupKey = NotificationGroupKeys.getGroupKey(account),
            accountName = account.displayName,
            color = account.chipColor,
            newMessagesCount = notificationData.newMessagesCount,
            lockScreenNotificationData = createLockScreenNotificationData(notificationData),
            appearance = createNotificationAppearance(account)
        )
    }

    private fun createLockScreenNotificationData(data: NotificationData): LockScreenNotificationData {
        return when (Ann.lockScreenNotificationVisibility) {
            LockScreenNotificationVisibility.NOTHING -> LockScreenNotificationData.None
            LockScreenNotificationVisibility.APP_NAME -> LockScreenNotificationData.AppName
            LockScreenNotificationVisibility.EVERYTHING -> LockScreenNotificationData.Public
            LockScreenNotificationVisibility.MESSAGE_COUNT -> LockScreenNotificationData.MessageCount
            LockScreenNotificationVisibility.SENDERS -> LockScreenNotificationData.SenderNames(getSenderNames(data))
        }
    }

    private fun getSenderNames(data: NotificationData): String {
        return data.activeNotifications.asSequence()
            .map { it.content.sender }
            .distinct()
            .take(MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION)
            .joinToString()
    }

    private fun createNotificationAppearance(account: Account): NotificationAppearance {
        return with(account.notificationSettings) {
            val vibrationPattern = vibration.systemPattern.takeIf { vibration.isEnabled }
            NotificationAppearance(ringtone, vibrationPattern, account.notificationSettings.light.toColor(account))
        }
    }
}
