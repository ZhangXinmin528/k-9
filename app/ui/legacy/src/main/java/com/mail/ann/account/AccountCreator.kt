package com.mail.ann.account

import android.content.res.Resources
import com.mail.ann.Account.DeletePolicy
import com.mail.ann.Preferences
import com.mail.ann.mail.ConnectionSecurity
import com.mail.ann.preferences.Protocols
import com.mail.ann.ui.R
import com.mail.ann.ui.helper.MaterialColors

/**
 * Deals with logic surrounding account creation.
 *
 * TODO Move much of the code from com.fsck.k9.activity.setup.* into here
 */
class AccountCreator(private val preferences: Preferences, private val resources: Resources) {

    fun getDefaultDeletePolicy(type: String): DeletePolicy {
        return when (type) {
            Protocols.IMAP -> DeletePolicy.ON_DELETE
            Protocols.POP3 -> DeletePolicy.NEVER
            Protocols.WEBDAV -> DeletePolicy.ON_DELETE
            "demo" -> DeletePolicy.ON_DELETE
            else -> throw AssertionError("Unhandled case: $type")
        }
    }

    fun getDefaultPort(securityType: ConnectionSecurity, serverType: String): Int {
        return when (serverType) {
            Protocols.IMAP -> getImapDefaultPort(securityType)
            Protocols.WEBDAV -> getWebDavDefaultPort(securityType)
            Protocols.POP3 -> getPop3DefaultPort(securityType)
            Protocols.SMTP -> getSmtpDefaultPort(securityType)
            else -> throw AssertionError("Unhandled case: $serverType")
        }
    }

    private fun getImapDefaultPort(connectionSecurity: ConnectionSecurity): Int {
        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) 993 else 143
    }

    private fun getPop3DefaultPort(connectionSecurity: ConnectionSecurity): Int {
        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) 995 else 110
    }

    private fun getWebDavDefaultPort(connectionSecurity: ConnectionSecurity): Int {
        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) 443 else 80
    }

    private fun getSmtpDefaultPort(connectionSecurity: ConnectionSecurity): Int {
        return if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) 465 else 587
    }

    fun pickColor(): Int {
        val accounts = preferences.accounts
        val usedAccountColors = accounts.map { it.chipColor }
        val accountColors = resources.getIntArray(R.array.account_colors).toList()

        val availableColors = accountColors - usedAccountColors
        if (availableColors.isEmpty()) {
            return accountColors.random()
        }

        return availableColors.shuffled().minByOrNull { color ->
            val index = DEFAULT_COLORS.indexOf(color)
            if (index != -1) index else DEFAULT_COLORS.size
        } ?: error("availableColors must not be empty")
    }

    companion object {
        private val DEFAULT_COLORS = intArrayOf(
            MaterialColors.PINK_500,
            MaterialColors.TEAL_700,
            MaterialColors.AMBER_600
        )
    }
}
