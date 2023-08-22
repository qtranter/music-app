package com.audiomack.ui.premium

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.viewModels
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.model.LoginSignupSource
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.views.AMProgressHUD
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_inapppurchase_2019.buttonClose
import kotlinx.android.synthetic.main.activity_inapppurchase_2019.buttonRestore
import kotlinx.android.synthetic.main.activity_inapppurchase_2019.buttonUpgrade
import kotlinx.android.synthetic.main.activity_inapppurchase_2019.imageViewBackground
import kotlinx.android.synthetic.main.activity_inapppurchase_2019.tvFooter
import kotlinx.android.synthetic.main.activity_inapppurchase_2019.tvHint

class InAppPurchaseActivity : BaseActivity() {

    private val viewModel: InAppPurchaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.mode = intent.getSerializableExtra("mode") as InAppPurchaseMode

        setContentView(R.layout.activity_inapppurchase_2019)

        initViewModelObservers()
        initClickListeners()
        initView()

        viewModel.onCreate()

        if (intent.getBooleanExtra(EXTRA_START_TRIAL, false)) {
            viewModel.onUpgradeTapped(this)
        }
    }

    private fun initView() {
        Picasso.get()
            .load(R.drawable.premium_2019_header)
            .config(Bitmap.Config.RGB_565).into(imageViewBackground)
    }

    private fun initViewModelObservers() {
        viewModel.closeEvent.observe(this) { finish() }

        viewModel.subscriptionPriceString.observe(this) { subscriptionPrice ->
            tvHint.text = getString(R.string.premium_price_template, subscriptionPrice)
            tvFooter.text = getString(R.string.premium_desc_template, subscriptionPrice)
        }

        viewModel.startLoginFlowEvent.observe(this) {
            AuthenticationActivity.show(this@InAppPurchaseActivity, LoginSignupSource.Premium)
        }

        viewModel.showRestoreLoadingEvent.observe(this) {
            AMProgressHUD.showWithStatus(this@InAppPurchaseActivity)
        }

        viewModel.hideRestoreLoadingEvent.observe(this) {
            AMProgressHUD.dismiss()
        }

        viewModel.showRestoreFailureNoSubscriptionsEvent.observe(this) {
            AMProgressHUD.showWithError(this@InAppPurchaseActivity, getString(R.string.premium_no_active_subscriptions))
        }

        viewModel.showRestoreFailureErrorEvent.observe(this) {
            AMProgressHUD.showWithError(this@InAppPurchaseActivity, getString(R.string.premium_unable_restore))
        }

        viewModel.requestUpgradeEvent.observe(this) {
            viewModel.onUpgradeTapped(this)
        }
    }

    private fun initClickListeners() {
        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
        buttonUpgrade.setOnClickListener { viewModel.onUpgradeTapped(this@InAppPurchaseActivity) }
        buttonRestore.setOnClickListener { viewModel.onRestoreTapped() }
    }

    companion object {
        private const val EXTRA_START_TRIAL = "extra_start_trial"

        @JvmStatic
        @JvmOverloads
        fun show(activity: Activity?, mode: InAppPurchaseMode, startTrial: Boolean = false) {
            activity?.let {
                val intent = Intent(it, InAppPurchaseActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("mode", mode)
                intent.putExtra(EXTRA_START_TRIAL, startTrial)
                it.startActivity(intent)
            }
        }
    }
}
