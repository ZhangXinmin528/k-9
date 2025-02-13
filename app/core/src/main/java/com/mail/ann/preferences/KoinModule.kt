package com.mail.ann.preferences

import com.mail.ann.Preferences
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val preferencesModule = module {
    factory {
        SettingsExporter(
            contentResolver = get(),
            preferences = get(),
            folderSettingsProvider = get(),
            folderRepository = get(),
            notificationSettingsUpdater = get()
        )
    }
    factory { FolderSettingsProvider(folderRepository = get()) }
    factory<AccountManager> { get<Preferences>() }
    single {
        RealGeneralSettingsManager(
            preferences = get(),
            coroutineScope = get(named("AppCoroutineScope"))
        )
    } bind GeneralSettingsManager::class
}
