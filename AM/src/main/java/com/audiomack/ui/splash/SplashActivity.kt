package com.audiomack.ui.splash

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.openUrlExcludingAudiomack
import io.branch.referral.Branch
import io.embrace.android.embracesdk.annotation.StartupActivity
import kotlinx.android.synthetic.main.activity_splash.*
import timber.log.Timber

@StartupActivity
class SplashActivity : BaseActivity() {

    private var amDeeplink: String? = null

    private val viewModel: SplashViewModel by viewModels()

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip when resuming
        if (!isTaskRoot) {
            finish()
            return
        }

        amDeeplink = intent.extras?.getString("am_deeplink")

        setContentView(R.layout.activity_splash)

        initClickListeners()
        initViewModelObservers()

        animationView.apply {
            playAnimation()
        }

        viewModel.onCreate(this@SplashActivity, viewModel.fakeWait)
    }

    override fun onStart() {
        super.onStart()

        val branch = Branch.getInstance(applicationContext)
        branch.initSession({ referringParams, error ->
            if (error == null && referringParams != null) {
                Timber.tag("Branch").i(referringParams.toString())

                if (referringParams.optBoolean("+clicked_branch_link") || referringParams.optBoolean("+is_first_session")) {

                    var branchDeeplink: String? = null

                    if (Branch.getInstance().latestReferringParams.has("\$deeplink_path")) {
                        try {
                            val deeplinkPath = Branch.getInstance().latestReferringParams.getString("\$deeplink_path")

                            var key = ""
                            try {
                                if (Branch.getInstance().latestReferringParams.has("key")) {
                                    key = Branch.getInstance().latestReferringParams.getString("key")
                                }
                            } catch (e: Exception) {
                                Timber.w(e)
                            }

                            branchDeeplink = deeplinkPath + if (TextUtils.isEmpty(key)) "" else "?key=$key"
                            Timber.tag("Branch").i("\$deeplink_path: $deeplinkPath")
                        } catch (e: Exception) {
                            Timber.w(e)
                        }
                    } else {
                        Timber.tag("Branch").w("\$deeplink_path not found")
                    }

                    viewModel.onBranchDeeplinkDetected(this@SplashActivity, branchDeeplink)
                }
            } else {
                Timber.tag("Branch").e(error?.message)
            }
        }, this.intent.data, this)

        animationView.resumeAnimation()
    }

    override fun onPause() {
        animationView.pauseAnimation()
        super.onPause()
    }

    private fun initClickListeners() {
        buttonTryAgain.setOnClickListener { viewModel.onTryAgainTapped() }
        buttonGoToDownloads.setOnClickListener { viewModel.onGoToDownloadsTapped() }
        buttonOK.setOnClickListener { viewModel.onGrantPermissionsTapped(this@SplashActivity) }
        buttonPrivacy.setOnClickListener { viewModel.onPrivacyPolicyTapped() }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            showLoadingUIEvent.observe(this@SplashActivity, showLoadingUIObserver)
            goToDownloadsEvent.observe(this@SplashActivity, goToDownloadsObserver)
            grantPermissionsEvent.observe(this@SplashActivity, grantPermissionsObserver)
            deleteNotificationEvent.observe(this@SplashActivity, deleteNotificationObserver)
            goHomeEvent.observe(this@SplashActivity, goHomeObserver)
            showPermissionsViewEvent.observe(this@SplashActivity, showPermissionsViewObserver)
            runAutologinEvent.observe(this@SplashActivity, runAutologinObserver)
            showRetryLoginEvent.observe(this@SplashActivity, showRetryLoginObserver)
            openURLEvent.observe(this@SplashActivity, openURLObserver)
        }
    }

    private val showLoadingUIObserver: Observer<Void> = Observer {
        buttonTryAgain.visibility = View.GONE
        tvOffline.visibility = View.GONE
        buttonGoToDownloads.visibility = View.GONE
    }

    private val goToDownloadsObserver: Observer<Void> = Observer {
        finish()

        val intent = Intent(this@SplashActivity, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(HomeActivity.EXTRA_OFFLINE, true)
            amDeeplink?.let {
                putExtra("am_deeplink", it)
            }
        }
        startActivity(intent)
    }

    private val grantPermissionsObserver: Observer<Void> = Observer {
        val locationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

        if (ContextCompat.checkSelfPermission(this@SplashActivity, locationPermission) != PackageManager.PERMISSION_GRANTED) {
            viewModel.onRequestedLocationPermission()
            ActivityCompat.requestPermissions(
                    this@SplashActivity,
                    arrayOf(locationPermission),
                    viewModel.reqCodePermissions
            )
        } else {
            viewModel.onPermissionsAlreadyGranted()
        }
    }

    private val deleteNotificationObserver: Observer<Void> = Observer {
        (MainApplication.context as MainApplication).deleteNotifications()
    }

    private val goHomeObserver: Observer<Void> = Observer {
        finish()
        overridePendingTransition(0, 0)
        startActivity(Intent(this@SplashActivity, HomeActivity::class.java).apply {
            amDeeplink?.let {
                putExtra("am_deeplink", it)
            }
        })
    }

    private val showPermissionsViewObserver: Observer<Void> = Observer {
        ivAudiomack.alpha = 0.0f
        tvPermissionsMessage.alpha = 0.0f
        layoutPermissionsBox.alpha = 0.0f
        buttonOK.alpha = 0.0f
        buttonPrivacy.alpha = 0.0f
        ivAudiomack.visibility = View.VISIBLE
        tvPermissionsMessage.visibility = View.VISIBLE
        layoutPermissionsBox.visibility = View.VISIBLE
        buttonOK.visibility = View.VISIBLE
        buttonPrivacy.visibility = View.VISIBLE

        val lp = animationView.layoutParams as ConstraintLayout.LayoutParams

        val animator = ValueAnimator.ofFloat(0.5f, 0.15f)
        animator.addUpdateListener { animation ->
            lp.verticalBias = animation.animatedValue as Float
            animationView.layoutParams = lp
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {

                val alphaAnimator = ValueAnimator.ofFloat(0.toFloat(), 1.toFloat())
                alphaAnimator.addUpdateListener { alphaAnimation ->
                    val value = alphaAnimation.animatedValue as Float
                    layoutPermissionsBox.alpha = value
                    ivAudiomack.alpha = value
                    tvPermissionsMessage.alpha = value
                    buttonOK.alpha = value
                    buttonPrivacy.alpha = value
                }
                alphaAnimator.duration = 350
                alphaAnimator.interpolator = DecelerateInterpolator()
                alphaAnimator.start()
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
        animator.duration = 500
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    private val runAutologinObserver: Observer<Void> = Observer {
        viewModel.autologin(this@SplashActivity)
    }

    private val showRetryLoginObserver: Observer<Void> = Observer {
        buttonTryAgain.visibility = View.VISIBLE
        tvOffline.visibility = View.VISIBLE
        buttonGoToDownloads.visibility = View.VISIBLE
    }

    private val openURLObserver: Observer<String> = Observer { urlString ->
        openUrlExcludingAudiomack(urlString)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    private fun launchHomeWithExtras(extras: Bundle) {
        val intent = Intent(this@SplashActivity, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtras(extras)
        startActivity(intent)
    }
}
