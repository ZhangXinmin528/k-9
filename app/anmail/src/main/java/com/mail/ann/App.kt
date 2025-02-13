package com.mail.ann

import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import com.mail.ann.activity.MessageCompose
import com.mail.ann.controller.MessagingController
import com.mail.ann.external.MessageProvider
import com.mail.ann.notification.NotificationChannelManager
import com.mail.ann.ui.base.AppLanguageManager
import com.mail.ann.ui.base.ThemeManager
import com.mail.ann.ui.base.extensions.currentLocale
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.koin.android.ext.android.inject
import timber.log.Timber

class App : Application() {
    private val messagingController: MessagingController by inject()
    private val messagingListenerProvider: MessagingListenerProvider by inject()
    private val themeManager: ThemeManager by inject()
    private val appLanguageManager: AppLanguageManager by inject()
    private val notificationChannelManager: NotificationChannelManager by inject()
    private val appCoroutineScope: CoroutineScope = GlobalScope + Dispatchers.Main
    private var appLanguageManagerInitialized = false

    override fun onCreate() {
        Core.earlyInit()

        super.onCreate()

        DI.start(this, coreModules + uiModules + appModules)

        Ann.init(this)
        Core.init(this)
        MessageProvider.init()
        initializeAppLanguage()
        updateNotificationChannelsOnAppLanguageChanges()
        themeManager.init()

        messagingListenerProvider.listeners.forEach { listener ->
            messagingController.addListener(listener)
        }
    }

    private fun initializeAppLanguage() {
        appLanguageManager.init()
        applyOverrideLocaleToConfiguration()
        appLanguageManagerInitialized = true
        listenForAppLanguageChanges()
    }

    private fun applyOverrideLocaleToConfiguration() {
        appLanguageManager.getOverrideLocale()?.let { overrideLocale ->
            updateConfigurationWithLocale(superResources.configuration, overrideLocale)
        }
    }

    private fun listenForAppLanguageChanges() {
        appLanguageManager.overrideLocale
            .drop(1) // We already applied the initial value
            .onEach { overrideLocale ->
                val locale = overrideLocale ?: Locale.getDefault()
                updateConfigurationWithLocale(superResources.configuration, locale)
            }
            .launchIn(appCoroutineScope)
    }

    override fun onConfigurationChanged(newConfiguration: Configuration) {
        applyOverrideLocaleToConfiguration()
        super.onConfigurationChanged(superResources.configuration)
    }

    private fun updateConfigurationWithLocale(configuration: Configuration, locale: Locale) {
        Timber.d("Updating application configuration with locale '$locale'")

        val newConfiguration = Configuration(configuration).apply {
            currentLocale = locale
        }

        @Suppress("DEPRECATION")
        superResources.updateConfiguration(newConfiguration, superResources.displayMetrics)
    }

    private val superResources: Resources
        get() = super.getResources()

    // Creating a WebView instance triggers something that will cause the configuration of the Application's Resources
    // instance to be reset to the default, i.e. not containing our locale override. Unfortunately, we're not notified
    // about this event. So we're checking each time someone asks for the Resources instance whether we need to change
    // the configuration again. Luckily, right now (Android 11), the platform is calling this method right after
    // resetting the configuration.
    override fun getResources(): Resources {
        val resources = super.getResources()

        if (appLanguageManagerInitialized) {
            appLanguageManager.getOverrideLocale()?.let { overrideLocale ->
                if (resources.configuration.currentLocale != overrideLocale) {
                    Timber.w("Resources configuration was reset. Re-applying locale override.")
                    appLanguageManager.applyOverrideLocale()
                    applyOverrideLocaleToConfiguration()
                }
            }
        }

        return resources
    }

    private fun updateNotificationChannelsOnAppLanguageChanges() {
        appLanguageManager.appLocale
            .distinctUntilChanged()
            .onEach { notificationChannelManager.updateChannels() }
            .launchIn(appCoroutineScope)
    }

    companion object {
        val appConfig = AppConfig(
            componentsToDisable = listOf(MessageCompose::class.java)
        )
    }
}
