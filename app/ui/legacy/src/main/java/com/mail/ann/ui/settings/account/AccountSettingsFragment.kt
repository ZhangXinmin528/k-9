package com.mail.ann.ui.settings.account

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.mail.ann.Account
import com.mail.ann.account.BackgroundAccountRemover
import com.mail.ann.activity.ManageIdentities
import com.mail.ann.activity.setup.AccountSetupComposition
import com.mail.ann.activity.setup.AccountSetupIncoming
import com.mail.ann.activity.setup.AccountSetupOutgoing
import com.mail.ann.controller.MessagingController
import com.mail.ann.crypto.OpenPgpApiHelper
import com.mail.ann.fragment.ConfirmationDialogFragment
import com.mail.ann.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.mail.ann.mailstore.FolderType
import com.mail.ann.mailstore.RemoteFolder
import com.mail.ann.notification.NotificationChannelManager
import com.mail.ann.notification.NotificationChannelManager.ChannelType
import com.mail.ann.notification.NotificationSettingsUpdater
import com.mail.ann.ui.R
import com.mail.ann.ui.endtoend.AutocryptKeyTransferActivity
import com.mail.ann.ui.settings.onClick
import com.mail.ann.ui.settings.oneTimeClickListener
import com.mail.ann.ui.settings.remove
import com.mail.ann.ui.settings.removeEntry
import com.mail.ann.ui.withArguments
import com.takisoft.preferencex.PreferenceFragmentCompat
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf
import org.openintents.openpgp.OpenPgpApiManager
import org.openintents.openpgp.util.OpenPgpKeyPreference
import org.openintents.openpgp.util.OpenPgpProviderUtil

class AccountSettingsFragment : PreferenceFragmentCompat(), ConfirmationDialogFragmentListener {
    private val viewModel: AccountSettingsViewModel by sharedViewModel()
    private val dataStoreFactory: AccountSettingsDataStoreFactory by inject()
    private val openPgpApiManager: OpenPgpApiManager by inject { parametersOf(this) }
    private val messagingController: MessagingController by inject()
    private val accountRemover: BackgroundAccountRemover by inject()
    private val notificationChannelManager: NotificationChannelManager by inject()
    private val notificationSettingsUpdater: NotificationSettingsUpdater by inject()

    private val vibrator by lazy { requireContext().getSystemService<Vibrator>() }
    private lateinit var dataStore: AccountSettingsDataStore

    private var notificationSoundPreference: NotificationSoundPreference? = null
    private var notificationLightPreference: ListPreference? = null
    private var notificationVibrationPreference: VibrationPreference? = null

    private val accountUuid: String by lazy {
        checkNotNull(arguments?.getString(ARG_ACCOUNT_UUID)) { "$ARG_ACCOUNT_UUID == null" }
    }
    private var title: CharSequence? = null

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        val account = getAccount()
        dataStore = dataStoreFactory.create(account)

        preferenceManager.preferenceDataStore = dataStore
        setPreferencesFromResource(R.xml.account_settings, rootKey)
        title = preferenceScreen.title
        setHasOptionsMenu(true)

        initializeIncomingServer()
        initializeComposition()
        initializeManageIdentities()
        initializeUploadSentMessages(account)
        initializeOutgoingServer()
        initializeQuoteStyle()
        initializeDeletePolicy(account)
        initializeExpungePolicy(account)
        initializeMessageAge(account)
        initializeAdvancedPushSettings(account)
        initializeCryptoSettings(account)
        initializeFolderSettings(account)
        initializeNotifications(account)

        findPreference<Preference>("openpgp")?.let {
            preferenceScreen.removePreference(it)
        }
        findPreference<Preference>("search")?.let {
            preferenceScreen.removePreference(it)
        }
        findPreference<Preference>("notifications")?.let {
            preferenceScreen.removePreference(it)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().title = title
    }

    override fun onResume() {
        super.onResume()

        // we might be returning from OpenPgpAppSelectDialog, make sure settings are up to date
        val account = getAccount()
        initializeCryptoSettings(account)

        // Don't update the notification preferences when resuming after the user has selected a new notification sound
        // via NotificationSoundPreference. Otherwise we race the background thread and might read data from the old
        // NotificationChannel, overwriting the notification sound with the previous value.
        notificationSoundPreference?.let { notificationSoundPreference ->
            if (notificationSoundPreference.receivedActivityResultJustNow) {
                notificationSoundPreference.receivedActivityResultJustNow = false
            } else {
                maybeUpdateNotificationPreferences(account)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.account_settings_option, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_account -> {
                onDeleteAccount()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    //配置收件服务器
    private fun initializeIncomingServer() {
        findPreference<Preference>(PREFERENCE_INCOMING_SERVER)?.onClick {
            AccountSetupIncoming.actionEditIncomingSettings(requireActivity(), accountUuid)
        }
    }

    //撰写新邮件信息
    private fun initializeComposition() {
        findPreference<Preference>(PREFERENCE_COMPOSITION)?.onClick {
            AccountSetupComposition.actionEditCompositionSettings(requireActivity(), accountUuid)
        }
    }

    //管理身份标识
    private fun initializeManageIdentities() {
        findPreference<Preference>(PREFERENCE_MANAGE_IDENTITIES)?.onClick {
            ManageIdentities.start(requireActivity(), accountUuid)
        }
    }

    //上传发送邮件->已发送
    private fun initializeUploadSentMessages(account: Account) {
        findPreference<Preference>(PREFERENCE_UPLOAD_SENT_MESSAGES)?.apply {
            if (!messagingController.supportsUpload(account)) {
                remove()
            }
        }
    }

    //配置发件服务器
    private fun initializeOutgoingServer() {
        findPreference<Preference>(PREFERENCE_OUTGOING_SERVER)?.onClick {
            AccountSetupOutgoing.actionEditOutgoingSettings(requireActivity(), accountUuid)
        }
    }

    //邮件回复时引用格式
    private fun initializeQuoteStyle() {
        findPreference<Preference>(PREFERENCE_QUOTE_STYLE)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val quoteStyle = Account.QuoteStyle.valueOf(newValue.toString())
                notifyDependencyChange(quoteStyle == Account.QuoteStyle.HEADER)
                true
            }
        }
    }

    //当我删除邮件时的策略
    private fun initializeDeletePolicy(account: Account) {
        (findPreference(PREFERENCE_DELETE_POLICY) as? ListPreference)?.apply {
            if (!messagingController.supportsFlags(account)) {
                removeEntry(DELETE_POLICY_MARK_AS_READ)
            }
        }
    }

    //抹掉删除的邮件
    private fun initializeExpungePolicy(account: Account) {
        findPreference<Preference>(PREFERENCE_EXPUNGE_POLICY)?.apply {
            if (!messagingController.supportsExpunge(account)) {
                remove()
            }
        }
    }

    //同步改时间段内邮件
    private fun initializeMessageAge(account: Account) {
        findPreference<Preference>(PREFERENCE_MESSAGE_AGE)?.apply {
            if (!messagingController.supportsSearchByDate(account)) {
                remove()
            }
        }
    }

    //推送功能：1.推送文件夹；2.高级
    private fun initializeAdvancedPushSettings(account: Account) {
        if (!messagingController.isPushCapable(account)) {
            findPreference<Preference>(PREFERENCE_PUSH_MODE)?.remove()
            findPreference<Preference>(PREFERENCE_ADVANCED_PUSH_SETTINGS)?.remove()
        }
    }

    private fun initializeNotifications(account: Account) {
        //震动
        if (vibrator?.hasVibrator() != true) {
            findPreference<Preference>(PREFERENCE_NOTIFICATION_VIBRATION)?.remove()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //通知声音
            findPreference<NotificationSoundPreference>(PREFERENCE_NOTIFICATION_SOUND)?.let { preference ->
                notificationSoundPreference = preference
                preference.isEnabled = false
            }

            //通知灯
            findPreference<ListPreference>(PREFERENCE_NOTIFICATION_LIGHT)?.let { preference ->
                notificationLightPreference = preference
                preference.isEnabled = false
            }

            //通知震动
            findPreference<VibrationPreference>(PREFERENCE_NOTIFICATION_VIBRATION)?.let { preference ->
                notificationVibrationPreference = preference
                preference.isEnabled = false
            }

            //通知类别：配置新消息通知
            findPreference<NotificationsPreference>(PREFERENCE_NOTIFICATION_SETTINGS_MESSAGES)?.let {
                it.notificationChannelIdProvider = {
                    notificationChannelManager.getChannelIdFor(account, ChannelType.MESSAGES)
                }
            }
            //通知类别：配置错误和状态通知
            findPreference<NotificationsPreference>(PREFERENCE_NOTIFICATION_SETTINGS_MISCELLANEOUS)?.let {
                it.notificationChannelIdProvider = {
                    notificationChannelManager.getChannelIdFor(account, ChannelType.MISCELLANEOUS)
                }
            }
        } else {
            findPreference<PreferenceCategory>(PREFERENCE_NOTIFICATION_CHANNELS).remove()
        }
    }

    private fun maybeUpdateNotificationPreferences(account: Account) {
        if (notificationSoundPreference != null ||
            notificationLightPreference != null ||
            notificationVibrationPreference != null
        ) {
            updateNotificationPreferences(account)
        }
    }

    @SuppressLint("NewApi")
    private fun updateNotificationPreferences(account: Account) {
        notificationSettingsUpdater.updateNotificationSettings(account)
        val notificationSettings = account.notificationSettings

        notificationSoundPreference?.let { preference ->
            preference.setNotificationSound(notificationSettings.ringtone?.toUri())
            preference.isEnabled = true
        }

        notificationLightPreference?.let { preference ->
            preference.value = notificationSettings.light.name
            preference.isEnabled = true
        }

        notificationVibrationPreference?.let { preference ->
            val notificationVibration = notificationSettings.vibration
            preference.setVibration(
                isVibrationEnabled = notificationVibration.isEnabled,
                vibratePattern = notificationVibration.pattern,
                vibrationTimes = notificationVibration.repeatCount
            )
            preference.isEnabled = true
        }
    }

    //端到端加密
    private fun initializeCryptoSettings(account: Account) {
        findPreference<Preference>(PREFERENCE_OPENPGP)?.let {
            configureCryptoPreferences(account)
        }
    }

    private fun configureCryptoPreferences(account: Account) {
        var pgpProviderName: String? = null
        var pgpProvider = account.openPgpProvider
        val isPgpConfigured = pgpProvider != null

        if (isPgpConfigured) {
            pgpProviderName = getOpenPgpProviderName(pgpProvider)
            if (pgpProviderName == null) {
                Toast.makeText(requireContext(), R.string.account_settings_openpgp_missing, Toast.LENGTH_LONG).show()

                pgpProvider = null
                removeOpenPgpProvider(account)
            }
        }

        configureEnablePgpSupport(account, isPgpConfigured, pgpProviderName)
        configurePgpKey(account, pgpProvider)
        configureAutocryptTransfer(account)
    }

    private fun getOpenPgpProviderName(pgpProvider: String?): String? {
        val packageManager = requireActivity().packageManager
        return OpenPgpProviderUtil.getOpenPgpProviderName(packageManager, pgpProvider)
    }

    private fun configureEnablePgpSupport(account: Account, isPgpConfigured: Boolean, pgpProviderName: String?) {
        (findPreference<Preference>(PREFERENCE_OPENPGP_ENABLE) as SwitchPreference).apply {
            if (!isPgpConfigured) {
                isChecked = false
                setSummary(R.string.account_settings_crypto_summary_off)
                oneTimeClickListener(clickHandled = false) {
                    val context = requireContext().applicationContext
                    val openPgpProviderPackages = OpenPgpProviderUtil.getOpenPgpProviderPackages(context)
                    if (openPgpProviderPackages.size == 1) {
                        setOpenPgpProvider(account, openPgpProviderPackages[0])
                        configureCryptoPreferences(account)
                    } else {
                        summary = getString(R.string.account_settings_crypto_summary_config)
                        OpenPgpAppSelectDialog.startOpenPgpChooserActivity(requireActivity(), account)
                    }
                }
            } else {
                isChecked = true
                summary = getString(R.string.account_settings_crypto_summary_on, pgpProviderName)
                oneTimeClickListener {
                    removeOpenPgpProvider(account)
                    configureCryptoPreferences(account)
                }
            }
        }
    }

    //设置端到端秘钥
    private fun configurePgpKey(account: Account, pgpProvider: String?) {
        (findPreference<Preference>(PREFERENCE_OPENPGP_KEY) as OpenPgpKeyPreference).apply {
            value = account.openPgpKey
            setOpenPgpProvider(openPgpApiManager, pgpProvider)
            setIntentSenderFragment(this@AccountSettingsFragment)
            setDefaultUserId(OpenPgpApiHelper.buildUserId(account.getIdentity(0)))
            setShowAutocryptHint(true)
        }
    }

    //与其他设备共享端到端配置
    private fun configureAutocryptTransfer(account: Account) {
        findPreference<Preference>(PREFERENCE_AUTOCRYPT_TRANSFER)!!.onClick {
            val intent = AutocryptKeyTransferActivity.createIntent(requireContext(), account.uuid)
            startActivity(intent)
        }
    }

    //文件夹模块
    private fun initializeFolderSettings(account: Account) {
        findPreference<Preference>(PREFERENCE_FOLDERS)?.let {
            if (!messagingController.isMoveCapable(account)) {
                findPreference<Preference>(PREFERENCE_ARCHIVE_FOLDER).remove()
                findPreference<Preference>(PREFERENCE_DRAFTS_FOLDER).remove()
                findPreference<Preference>(PREFERENCE_SENT_FOLDER).remove()
                findPreference<Preference>(PREFERENCE_SPAM_FOLDER).remove()
                findPreference<Preference>(PREFERENCE_TRASH_FOLDER).remove()
            }

            loadFolders(account)
        }
    }

    private fun loadFolders(account: Account) {
        viewModel.getFolders(account).observe(this@AccountSettingsFragment) { remoteFolderInfo ->
            if (remoteFolderInfo != null) {
                setFolders(PREFERENCE_AUTO_EXPAND_FOLDER, remoteFolderInfo.folders)
                setFolders(PREFERENCE_ARCHIVE_FOLDER, remoteFolderInfo, FolderType.ARCHIVE)
                setFolders(PREFERENCE_DRAFTS_FOLDER, remoteFolderInfo, FolderType.DRAFTS)
                setFolders(PREFERENCE_SENT_FOLDER, remoteFolderInfo, FolderType.SENT)
                setFolders(PREFERENCE_SPAM_FOLDER, remoteFolderInfo, FolderType.SPAM)
                setFolders(PREFERENCE_TRASH_FOLDER, remoteFolderInfo, FolderType.TRASH)
            }
        }
    }

    private fun setFolders(preferenceKey: String, folders: List<RemoteFolder>) {
        val folderListPreference = findPreference(preferenceKey) as? FolderListPreference ?: return
        folderListPreference.setFolders(folders)
    }

    private fun setFolders(preferenceKey: String, remoteFolderInfo: RemoteFolderInfo, type: FolderType?) {
        val folderListPreference = findPreference(preferenceKey) as? FolderListPreference ?: return

        val automaticFolder = remoteFolderInfo.automaticSpecialFolders[type]
        folderListPreference.setFolders(remoteFolderInfo.folders, automaticFolder)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val openPgpKeyPreference = findPreference(PREFERENCE_OPENPGP_KEY) as? OpenPgpKeyPreference
        if (openPgpKeyPreference?.handleOnActivityResult(requestCode, resultCode, data) == true) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getAccount(): Account {
        return viewModel.getAccountBlocking(accountUuid)
    }

    private fun onDeleteAccount() {
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            DIALOG_DELETE_ACCOUNT,
            getString(R.string.account_delete_dlg_title),
            getString(R.string.account_delete_dlg_instructions_fmt, getAccount().displayName),
            getString(R.string.okay_action),
            getString(R.string.cancel_action)
        )
        dialogFragment.setTargetFragment(this, REQUEST_DELETE_ACCOUNT)
        dialogFragment.show(requireFragmentManager(), TAG_DELETE_ACCOUNT_CONFIRMATION)
    }

    override fun doPositiveClick(dialogId: Int) {
        closeAccountSettings()
        accountRemover.removeAccountAsync(accountUuid)
    }

    override fun doNegativeClick(dialogId: Int) = Unit

    override fun dialogCancelled(dialogId: Int) = Unit

    private fun closeAccountSettings() {
        requireActivity().finish()
    }

    private fun setOpenPgpProvider(account: Account, openPgpProviderPackage: String) {
        account.openPgpProvider = openPgpProviderPackage
        dataStore.saveSettingsInBackground()
    }

    private fun removeOpenPgpProvider(account: Account) {
        account.openPgpProvider = null
        account.openPgpKey = Account.NO_OPENPGP_KEY
        dataStore.saveSettingsInBackground()
    }

    companion object {
        internal const val PREFERENCE_OPENPGP = "openpgp"
        private const val ARG_ACCOUNT_UUID = "accountUuid"
        private const val PREFERENCE_INCOMING_SERVER = "incoming"
        private const val PREFERENCE_COMPOSITION = "composition"
        private const val PREFERENCE_MANAGE_IDENTITIES = "manage_identities"
        private const val PREFERENCE_OUTGOING_SERVER = "outgoing"
        private const val PREFERENCE_UPLOAD_SENT_MESSAGES = "upload_sent_messages"
        private const val PREFERENCE_QUOTE_STYLE = "quote_style"
        private const val PREFERENCE_DELETE_POLICY = "delete_policy"
        private const val PREFERENCE_EXPUNGE_POLICY = "expunge_policy"
        private const val PREFERENCE_MESSAGE_AGE = "account_message_age"
        private const val PREFERENCE_PUSH_MODE = "folder_push_mode"
        private const val PREFERENCE_ADVANCED_PUSH_SETTINGS = "push_advanced"
        private const val PREFERENCE_OPENPGP_ENABLE = "openpgp_provider"
        private const val PREFERENCE_OPENPGP_KEY = "openpgp_key"
        private const val PREFERENCE_AUTOCRYPT_TRANSFER = "autocrypt_transfer"
        private const val PREFERENCE_FOLDERS = "folders"
        private const val PREFERENCE_AUTO_EXPAND_FOLDER = "account_setup_auto_expand_folder"
        private const val PREFERENCE_ARCHIVE_FOLDER = "archive_folder"
        private const val PREFERENCE_DRAFTS_FOLDER = "drafts_folder"
        private const val PREFERENCE_SENT_FOLDER = "sent_folder"
        private const val PREFERENCE_SPAM_FOLDER = "spam_folder"
        private const val PREFERENCE_TRASH_FOLDER = "trash_folder"
        private const val PREFERENCE_NOTIFICATION_SOUND = "account_ringtone"
        private const val PREFERENCE_NOTIFICATION_LIGHT = "notification_light"
        private const val PREFERENCE_NOTIFICATION_VIBRATION = "account_combined_vibration"
        private const val PREFERENCE_NOTIFICATION_CHANNELS = "notification_channels"
        private const val PREFERENCE_NOTIFICATION_SETTINGS_MESSAGES = "open_notification_settings_messages"
        private const val PREFERENCE_NOTIFICATION_SETTINGS_MISCELLANEOUS = "open_notification_settings_miscellaneous"
        private const val DELETE_POLICY_MARK_AS_READ = "MARK_AS_READ"

        private const val DIALOG_DELETE_ACCOUNT = 1
        private const val REQUEST_DELETE_ACCOUNT = 1
        private const val TAG_DELETE_ACCOUNT_CONFIRMATION = "delete_account_confirmation"

        fun create(accountUuid: String, rootKey: String?) = AccountSettingsFragment().withArguments(
            ARG_ACCOUNT_UUID to accountUuid,
            PreferenceFragmentCompat.ARG_PREFERENCE_ROOT to rootKey
        )
    }
}
