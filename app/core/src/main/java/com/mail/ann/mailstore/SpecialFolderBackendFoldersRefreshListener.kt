package com.mail.ann.mailstore

/**
 * Update special folders when folders are added, removed, or changed.
 */
class SpecialFolderBackendFoldersRefreshListener(
    private val specialFolderUpdater: SpecialFolderUpdater
) : BackendFoldersRefreshListener {

    override fun onBeforeFolderListRefresh() = Unit

    override fun onAfterFolderListRefresh() {
        specialFolderUpdater.updateSpecialFolders()
    }
}
