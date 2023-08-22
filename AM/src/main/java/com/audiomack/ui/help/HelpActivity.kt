package com.audiomack.ui.help

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.usecases.LoginAlertUseCase
import kotlinx.android.synthetic.main.activity_help.*
import zendesk.support.guide.HelpCenterActivity
import zendesk.support.requestlist.RequestListActivity

class HelpActivity : androidx.fragment.app.FragmentActivity() {

    private val viewModel: HelpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        viewModel.close.observe(this, Observer {
            finish()
        })
        viewModel.unreadTicketsCount.observe(this, Observer { count ->
            buttonTickets.text = if ((count ?: 0) > 0) getString(R.string.help_tickets_template, count.toString()) else getString(R.string.help_tickets_none)
        })
        viewModel.showKnowledgeBase.observe(this, Observer {
            HelpCenterActivity.builder()
                    .show(this@HelpActivity, viewModel.zendeskUIConfigs)
        })
        viewModel.showTickets.observe(this, Observer {
            RequestListActivity.builder()
                    .show(this@HelpActivity, viewModel.zendeskUIConfigs)
        })
        viewModel.showLoginAlert.observe(this, Observer {
            AMAlertFragment.show(
                this@HelpActivity,
                SpannableString(LoginAlertUseCase().getMessage(this@HelpActivity)),
                null,
                getString(R.string.login_needed_yes),
                getString(R.string.login_needed_no),
                Runnable { viewModel.onStartLoginTapped() },
                Runnable { viewModel.onCancelLoginTapped() },
                Runnable { viewModel.onCancelLoginTapped() }
            )
        })
        viewModel.showLogin.observe(this, Observer {
            it?.let { source ->
                AuthenticationActivity.show(this@HelpActivity, source)
            }
        })
        viewModel.showUnreadAlert.observe(this, Observer {
            AMAlertFragment.show(
                    this@HelpActivity,
                    SpannableString(getString(R.string.help_alert_title)),
                    null,
                    getString(R.string.help_alert_yes),
                    getString(R.string.help_alert_no),
                    Runnable { viewModel.onTicketsTapped() },
                    null,
                    null
            )
        })

        buttonBack.setOnClickListener { viewModel.onCloseTapped() }
        buttonKnowledgeBase.setOnClickListener { viewModel.onKnowledgeBaseTapped() }
        buttonTickets.setOnClickListener { viewModel.onTicketsTapped() }
    }

    override fun onResume() {
        super.onResume()

        viewModel.onUnreadTicketsCountRequested()
    }

    companion object {
        @JvmStatic
        fun show(activity: Activity?) {
            activity?.let {
                val intent = Intent(it, HelpActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(intent)
            }
        }
    }
}
