package com.mail.ann.job

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.mail.ann.Preferences
import com.mail.ann.controller.MessagingController

class K9WorkerFactory(
    private val messagingController: MessagingController,
    private val preferences: Preferences
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            MailSyncWorker::class.java.canonicalName -> {
                MailSyncWorker(messagingController, preferences, appContext, workerParameters)
            }
            else -> null
        }
    }
}
