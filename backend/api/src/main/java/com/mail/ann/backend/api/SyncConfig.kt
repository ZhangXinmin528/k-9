package com.mail.ann.backend.api

import com.mail.ann.mail.Flag
import java.util.Date

data class SyncConfig(
    val expungePolicy: ExpungePolicy,
    val earliestPollDate: Date?,
    val syncRemoteDeletions: Boolean,
    val maximumAutoDownloadMessageSize: Int,
    val defaultVisibleLimit: Int,
    val syncFlags: Set<Flag>
) {
    enum class ExpungePolicy {
        IMMEDIATELY,
        MANUALLY,
        ON_POLL
    }
}
