package com.mail.ann.mail.store.imap

internal class TestImapStore(private val folder: ImapFolder) : ImapStore, ImapConnectionProvider {
    override fun checkSettings() {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getFolder(name: String): ImapFolder {
        return folder
    }

    override fun getFolders(): List<FolderListItem> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getConnection(folder: ImapFolder): ImapConnection {
        if (folder !is TestImapFolder) throw AssertionError("getConnection() called with unknown ImapFolder instance")
        return folder.connection
    }

    override fun closeAllConnections() {
        throw UnsupportedOperationException("not implemented")
    }
}
