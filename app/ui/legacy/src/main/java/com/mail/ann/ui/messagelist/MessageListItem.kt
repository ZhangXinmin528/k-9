package com.mail.ann.ui.messagelist

import com.mail.ann.Account
import com.mail.ann.mail.Address

data class MessageListItem(
    val position: Int,
    val account: Account,
    val subject: String?,
    val threadCount: Int,
    val messageDate: Long,
    val displayName: CharSequence,
    val displayAddress: Address?,
    val toMe: Boolean,
    val ccMe: Boolean,
    val previewText: String,
    val isMessageEncrypted: Boolean,
    val isRead: Boolean,
    val isStarred: Boolean,
    val isAnswered: Boolean,
    val isForwarded: Boolean,
    val hasAttachments: Boolean,
    val uniqueId: Long,
    val folderId: Long,
    val messageUid: String,
    val databaseId: Long,
    val threadRoot: Long
)
