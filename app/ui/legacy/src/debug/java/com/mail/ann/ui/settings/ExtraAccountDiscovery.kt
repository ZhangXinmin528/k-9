package com.mail.ann.ui.settings

import com.mail.ann.mail.AuthType
import com.mail.ann.mail.ConnectionSecurity
import com.mail.ann.mail.ServerSettings
import com.mail.ann.ui.ConnectionSettings

object ExtraAccountDiscovery {
    @JvmStatic
    fun discover(email: String): ConnectionSettings? {
        return if (email.endsWith("@anmail.example")) {
            val serverSettings = ServerSettings(
                type = "demo",
                host = "irrelevant",
                port = 23,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.AUTOMATIC,
                username = "irrelevant",
                password = "irrelevant",
                clientCertificateAlias = null
            )
            ConnectionSettings(incoming = serverSettings, outgoing = serverSettings)
        } else {
            null
        }
    }
}
