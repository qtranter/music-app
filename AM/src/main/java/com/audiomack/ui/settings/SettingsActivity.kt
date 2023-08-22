package com.audiomack.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.format.DateUtils
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.audiomack.MainApplication
import com.audiomack.PRIVACY_POLICY_URL
import com.audiomack.R
import com.audiomack.STORE_URL
import com.audiomack.activities.BaseActivity
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.data.tracking.mixpanel.MixpanelButtonSettings
import com.audiomack.data.tracking.mixpanel.MixpanelPageSettings
import com.audiomack.data.tracking.mixpanel.SleepTimerSource
import com.audiomack.model.MixpanelSource
import com.audiomack.playback.PlayerPlayback
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.ui.authentication.changepw.ChangePasswordActivity
import com.audiomack.ui.defaultgenre.DefaultGenreActivity
import com.audiomack.ui.editaccount.EditAccountActivity
import com.audiomack.ui.help.HelpActivity
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.logviewer.LogViewerActivity
import com.audiomack.ui.notifications.preferences.NotificationsPreferencesActivity
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.audiomack.ui.sleeptimer.SleepTimerAlertFragment
import com.audiomack.ui.splash.SplashActivity
import com.audiomack.utils.Utils
import com.audiomack.utils.openUrlExcludingAudiomack
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import com.audiomack.views.AMSnackbar
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.snackbar.Snackbar
import java.util.Date
import kotlinx.android.synthetic.main.activity_settings.buttonBack
import kotlinx.android.synthetic.main.activity_settings.buttonCancelSubscription
import kotlinx.android.synthetic.main.activity_settings.buttonChangePassword
import kotlinx.android.synthetic.main.activity_settings.buttonDefaultGenre
import kotlinx.android.synthetic.main.activity_settings.buttonEqualizer
import kotlinx.android.synthetic.main.activity_settings.buttonLogViewer
import kotlinx.android.synthetic.main.activity_settings.buttonLogout
import kotlinx.android.synthetic.main.activity_settings.buttonOpenSource
import kotlinx.android.synthetic.main.activity_settings.buttonPermissions
import kotlinx.android.synthetic.main.activity_settings.buttonPrivacy
import kotlinx.android.synthetic.main.activity_settings.buttonRate
import kotlinx.android.synthetic.main.activity_settings.buttonShare
import kotlinx.android.synthetic.main.activity_settings.buttonShareAccount
import kotlinx.android.synthetic.main.activity_settings.buttonSleepTimer
import kotlinx.android.synthetic.main.activity_settings.buttonSupport
import kotlinx.android.synthetic.main.activity_settings.buttonUpgrade
import kotlinx.android.synthetic.main.activity_settings.buttonViewNotifications
import kotlinx.android.synthetic.main.activity_settings.buttonViewProfile
import kotlinx.android.synthetic.main.activity_settings.headerProfile
import kotlinx.android.synthetic.main.activity_settings.imgProfile
import kotlinx.android.synthetic.main.activity_settings.switchAdminOverride
import kotlinx.android.synthetic.main.activity_settings.switchEnvironment
import kotlinx.android.synthetic.main.activity_settings.switchGrantPremium
import kotlinx.android.synthetic.main.activity_settings.switchTrackAds
import kotlinx.android.synthetic.main.activity_settings.tvTicketsBadge
import kotlinx.android.synthetic.main.activity_settings.tvUserName
import kotlinx.android.synthetic.main.activity_settings.tvVersion
import kotlinx.android.synthetic.main.activity_settings.viewPremium
import timber.log.Timber
import zendesk.support.requestlist.RequestListActivity

class SettingsActivity : BaseActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViewModelObservers()
        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadData()
    }

    private fun initViewModelObservers() {
        viewModel.apply {

            // Events

            close.observe(this@SettingsActivity, Observer {
                finish()
            })
            unreadTicketsCount.observe(this@SettingsActivity, Observer { count ->
                tvTicketsBadge.visibility = if ((count ?: 0) > 0) View.VISIBLE else View.GONE
                tvTicketsBadge.text = (count ?: 0).toString()
            })
            upgrade.observe(this@SettingsActivity, Observer {
                InAppPurchaseActivity.show(this@SettingsActivity, it)
            })
            openExternalURLEvent.observe(this@SettingsActivity) { urlString ->
                openUrlExcludingAudiomack(urlString)
            }
            viewProfile.observe(this@SettingsActivity, Observer { urlSlug ->
                finish()
                urlSlug?.let { HomeActivity.instance?.homeViewModel?.onArtistScreenRequested(it) }
            })
            notificationsEvent.observe(this@SettingsActivity, Observer {
                startActivity(NotificationsPreferencesActivity.getLaunchIntent(this@SettingsActivity))
            })
            editAccount.observe(this@SettingsActivity, Observer {
                EditAccountActivity.show(this@SettingsActivity)
            })
            shareAccount.observe(this@SettingsActivity, Observer { artist ->
                artist.openShareSheet(this@SettingsActivity, MixpanelSource(MainApplication.currentTab, MixpanelPageSettings), MixpanelButtonSettings)
            })
            launchSleepTimerEvent.observe(this@SettingsActivity, Observer {
                SleepTimerAlertFragment.show(this@SettingsActivity, SleepTimerSource.Settings)
            })
            defaultGenre.observe(this@SettingsActivity, Observer {
                DefaultGenreActivity.show(this@SettingsActivity)
            })
            rate.observe(this@SettingsActivity, Observer {
                Utils.openAppRating(this@SettingsActivity)
            })
            share.observe(this@SettingsActivity, Observer {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, STORE_URL)
                    startActivity(Intent.createChooser(shareIntent, "Share this app"))
                } catch (e: Exception) {
                    Timber.w(e)
                }
            })
            permissions.observe(this@SettingsActivity, Observer {
                try {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", this@SettingsActivity.packageName, null)
                    startActivity(intent)
                } catch (e: Exception) {
                    Timber.w(e)
                }
            })
            privacy.observe(this@SettingsActivity, Observer {
                openUrlExcludingAudiomack(PRIVACY_POLICY_URL)
            })
            support.observe(this@SettingsActivity, Observer {
                HelpActivity.show(this@SettingsActivity)
            })
            liveEnvironment.observe(this@SettingsActivity, Observer {
                val intent = Intent(this@SettingsActivity, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            })
            showLogoutAlert.observe(this@SettingsActivity, Observer {
                AMAlertFragment.show(
                    this@SettingsActivity,
                    SpannableString(getString(R.string.logout_alert_title)),
                    getString(R.string.logout_alert_message),
                    getString(R.string.logout_alert_yes),
                    getString(R.string.logout_alert_no),
                    Runnable { viewModel.onLogoutConfirmed() },
                    null,
                    null
                )
            })
            logout.observe(this@SettingsActivity, Observer {
                startActivity(Intent(this@SettingsActivity, AuthenticationActivity::class.java))
                finish()
            })
            showUnreadAlert.observe(this@SettingsActivity, Observer {
                AMAlertFragment.show(
                    this@SettingsActivity,
                    SpannableString(getString(R.string.help_alert_title)),
                    null,
                    getString(R.string.help_alert_yes),
                    getString(R.string.help_alert_no),
                    Runnable { viewModel.onTicketsTapped() },
                    null,
                    null
                )
            })
            showTickets.observe(this@SettingsActivity, Observer { config ->
                RequestListActivity.builder().show(this@SettingsActivity, config)
            })
            logViewer.observe(this@SettingsActivity, Observer {
                LogViewerActivity.show(this@SettingsActivity)
            })
            openSource.observe(this@SettingsActivity, Observer {
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.opensource_title))
                startActivity(Intent(this@SettingsActivity, OssLicensesMenuActivity::class.java))
            })
            equalizer.observe(this@SettingsActivity, Observer {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                }

                PlayerPlayback.getInstance().audioSessionId?.let {
                    intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, it)
                }

                if ((intent.resolveActivity(packageManager) != null)) {
                    startActivityForResult(intent, 123)
                }
            })

            // Update UI

            avatar.observe(this@SettingsActivity, Observer {
                PicassoImageLoader.load(imgProfile.context, it, imgProfile)
            })
            artistWithBadge.observe(this@SettingsActivity, Observer { artistWithBadge ->
                when {
                    artistWithBadge.verified -> tvUserName.text = tvUserName.spannableStringWithImageAtTheEnd(artistWithBadge.name, R.drawable.ic_verified, 16)
                    artistWithBadge.tastemaker -> tvUserName.text = tvUserName.spannableStringWithImageAtTheEnd(artistWithBadge.name, R.drawable.ic_tastemaker, 16)
                    artistWithBadge.authenticated -> tvUserName.text = tvUserName.spannableStringWithImageAtTheEnd(artistWithBadge.name, R.drawable.ic_authenticated, 16)
                    else -> tvUserName.text = artistWithBadge.name
                }
            })
            profileHeaderVisible.observe(this@SettingsActivity, Observer {
                headerProfile.visibility = if (it) View.VISIBLE else View.GONE
            })
            premiumVisible.observe(this@SettingsActivity, Observer {
                viewPremium.visibility = if (it) View.VISIBLE else View.GONE
            })
            cancelSubscriptionVisible.observe(this@SettingsActivity, Observer {
                buttonCancelSubscription.visibility = if (it) View.VISIBLE else View.GONE
            })
            viewProfileVisible.observe(this@SettingsActivity, Observer {
                buttonViewProfile.visibility = if (it) View.VISIBLE else View.GONE
            })
            notificationsVisible.observe(this@SettingsActivity, Observer {
                buttonViewNotifications.visibility = if (it) View.VISIBLE else View.GONE
            })
            shareProfileVisible.observe(this@SettingsActivity, Observer {
                buttonShareAccount.visibility = if (it) View.VISIBLE else View.GONE
            })
            permissionsVisible.observe(this@SettingsActivity, Observer {
                buttonPermissions.visibility = if (it) View.VISIBLE else View.GONE
            })
            trackAdsVisibility.observe(this@SettingsActivity, Observer {
                switchTrackAds.visibility = if (it) View.VISIBLE else View.GONE
            })
            trackAdsChecked.observe(this@SettingsActivity, Observer {
                switchTrackAds.setCheckedProgrammatically(it)
            })
            grantPremiumVisibility.observe(this@SettingsActivity, Observer {
                switchGrantPremium.visibility = if (it) View.VISIBLE else View.GONE
            })
            grantPremiumChecked.observe(this@SettingsActivity, Observer {
                switchGrantPremium.setCheckedProgrammatically(it)
            })
            overridePremiumVisibility.observe(this@SettingsActivity, Observer {
                switchAdminOverride.visibility = if (it) View.VISIBLE else View.GONE
            })
            overridePremiumChecked.observe(this@SettingsActivity, Observer {
                switchAdminOverride.setCheckedProgrammatically(it)
            })
            switchEnvVisibility.observe(this@SettingsActivity, Observer {
                switchEnvironment.visibility = if (it) View.VISIBLE else View.GONE
            })
            switchEnvChecked.observe(this@SettingsActivity, Observer {
                switchEnvironment.setCheckedProgrammatically(it)
            })
            logViewerVisible.observe(this@SettingsActivity, Observer {
                buttonLogViewer.visibility = if (it) View.VISIBLE else View.GONE
            })
            logoutVisible.observe(this@SettingsActivity, Observer {
                buttonLogout.visibility = if (it) View.VISIBLE else View.GONE
            })
            equalizerVisible.observe(this@SettingsActivity, Observer {
                buttonEqualizer.visibility = if (it) View.VISIBLE else View.GONE
            })
            versionNameAndCode.observe(this@SettingsActivity, Observer { version ->
                tvVersion.text = getString(R.string.settings_version_template, version.name, version.code)
            })
            onSleepTimerSetEvent.observe(this@SettingsActivity) {
                onSleepTimerSet(it)
            }
            changePasswordVisible.observe(this@SettingsActivity) {
                buttonChangePassword.isVisible = it
            }
            openChangePasswordEvent.observe(this@SettingsActivity) {
                startActivity(Intent(this@SettingsActivity, ChangePasswordActivity::class.java))
            }
        }
    }

    private fun initClickListeners() {
        headerProfile.setOnClickListener { viewModel.onEditAccountTapped() }
        buttonBack.setOnClickListener { viewModel.onCloseTapped() }
        buttonUpgrade.setOnClickListener { viewModel.onUpgradeTapped() }
        buttonCancelSubscription.setOnClickListener { viewModel.onCancelSubscriptionTapped() }
        buttonViewProfile.setOnClickListener { viewModel.onViewProfileTapped() }
        buttonViewNotifications.setOnClickListener { viewModel.onNotificationsTapped() }
        buttonShareAccount.setOnClickListener { viewModel.onShareAccountTapped() }
        buttonSleepTimer.setOnClickListener { viewModel.onSleepTimerTapped() }
        buttonDefaultGenre.setOnClickListener { viewModel.onDefaultGenreTapped() }
        buttonRate.setOnClickListener { viewModel.onRateTapped() }
        buttonShare.setOnClickListener { viewModel.onShareTapped() }
        buttonPermissions.setOnClickListener { viewModel.onPermissionsTapped() }
        buttonEqualizer.setOnClickListener { viewModel.onEqualizerTapped() }
        buttonPrivacy.setOnClickListener { viewModel.onPrivacyTapped() }
        buttonSupport.setOnClickListener { viewModel.onSupportTapped() }
        switchEnvironment.setOnCheckedChangeListener { _, isChecked -> viewModel.onEnvironmentChanged(!isChecked) }
        switchTrackAds.setOnCheckedChangeListener { _, isChecked -> viewModel.onTrackAdsChanged(isChecked) }
        switchAdminOverride.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onAdminOverrideChanged(isChecked)
        }
        switchGrantPremium.setOnCheckedChangeListener { _, isChecked -> viewModel.onGrantPremiumChanged(isChecked) }
        buttonLogViewer.setOnClickListener { viewModel.onLogViewerTapped() }
        buttonOpenSource.setOnClickListener { viewModel.onOpenSourceTapped() }
        buttonLogout.setOnClickListener { viewModel.onLogoutTapped() }
        tvVersion.setOnClickListener { viewModel.onVersionTapped(this@SettingsActivity) }
        buttonChangePassword.setOnClickListener { viewModel.onChangePasswordTapped() }
    }

    override fun openOptionsFragment(optionsMenuFragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, optionsMenuFragment)
            .addToBackStack("options")
            .commitAllowingStateLoss()
    }

    override fun popFragment(): Boolean {
        supportFragmentManager.popBackStack()
        return true
    }

    private fun onSleepTimerSet(date: Date) {
        val time = date.time
        val sleepTimeString = DateUtils.formatDateTime(
            this,
            time,
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_TIME
        )
        val title = if (DateUtils.isToday(time)) {
            getString(R.string.sleep_timer_stop_today, sleepTimeString)
        } else {
            getString(R.string.sleep_timer_stop_tomorrow, sleepTimeString)
        }

        AMSnackbar.Builder(this)
            .withTitle(title)
            .withDrawable(R.drawable.ic_snackbar_timer)
            .withDuration(Snackbar.LENGTH_SHORT)
            .show()
    }

    companion object {
        @JvmStatic
        fun show(activity: Activity?) {
            activity?.let {
                val intent = Intent(it, SettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(intent)
            }
        }
    }
}
