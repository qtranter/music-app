package com.audiomack.ui.report

import android.os.Bundle
import android.text.SpannableString
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.ReportContentModel
import com.audiomack.model.ReportContentReason
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.utils.extensions.drawableCompat
import kotlinx.android.synthetic.main.fragment_report_content.buttonSave
import kotlinx.android.synthetic.main.fragment_report_content.ivSaveOverlay
import kotlinx.android.synthetic.main.fragment_report_content.tvBroken
import kotlinx.android.synthetic.main.fragment_report_content.tvInfringement
import kotlinx.android.synthetic.main.fragment_report_content.tvMisleading
import kotlinx.android.synthetic.main.fragment_report_content.tvSpam
import kotlinx.android.synthetic.main.fragment_report_content.tvViolent

class ReportContentFragment : TrackedFragment(R.layout.fragment_report_content, TAG) {

    private val viewModel: ReportContentViewModel by activityViewModels()
    private lateinit var model: ReportContentModel

    private fun initClickListeners() {
        buttonSave.setOnClickListener { viewModel.onSubmitTapped() }
        tvViolent.setOnClickListener { viewModel.onReasonSelected(ReportContentReason.Violent) }
        tvBroken.setOnClickListener { viewModel.onReasonSelected(ReportContentReason.Broken) }
        tvMisleading.setOnClickListener { viewModel.onReasonSelected(ReportContentReason.Misleading) }
        tvSpam.setOnClickListener { viewModel.onReasonSelected(ReportContentReason.Spam) }
        tvInfringement.setOnClickListener { viewModel.onReasonSelected(ReportContentReason.Infringement) }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            showConfirmationEvent.observe(viewLifecycleOwner, showConfirmationObserver)
            reportReason.observe(viewLifecycleOwner, reportReasonObserver)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model = arguments?.getParcelable(EXTRA_REPORT_CONTENT) as? ReportContentModel
            ?: throw IllegalStateException("No model specified in arguments")

        initClickListeners()
        initViewModelObservers()
    }

    private val reportReasonObserver: Observer<ReportContentReason> = Observer { selectedReason ->
        model.reportReason?.let { previousReason ->
            configureReasonTextView(previousReason, false)
            if (previousReason != selectedReason) {
                configureReasonTextView(selectedReason, true)
            }
        } ?: run {
            configureReasonTextView(selectedReason, true)
        }
        val enableSave = model.reportReason != null
        buttonSave.isEnabled = enableSave
        ivSaveOverlay.visibility = if (enableSave) View.GONE else View.VISIBLE
    }

    private val showConfirmationObserver: Observer<Void> = Observer {
        val contentReason = model.reportReason
        val notNullActivity = activity
        if (contentReason != null && notNullActivity != null) {
            AMAlertFragment.show(
                notNullActivity,
                SpannableString(getString(R.string.confirm_bis_report_alert_title)),
                null,
                getString(R.string.confirm_bis_report_alert_cancel),
                getString(R.string.confirm_bis_report_alert_yes),
                null,
                Runnable { viewModel.onSendReport(model.reportType, model.contentId, model.contentType, contentReason) },
                null
            )
        }
    }

    private fun configureReasonTextView(reason: ReportContentReason, isSelected: Boolean) {
        if (isSelected) model.reportReason = reason else model.reportReason = null
        val tvReason = when (reason) {
            ReportContentReason.Violent -> tvViolent
            ReportContentReason.Broken -> tvBroken
            ReportContentReason.Misleading -> tvMisleading
            ReportContentReason.Spam -> tvSpam
            ReportContentReason.Infringement -> tvInfringement
        }
        tvReason.setCompoundDrawablesWithIntrinsicBounds(
            tvReason.context.drawableCompat(if (isSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
            null, null, null
        )
    }

    companion object {
        private const val TAG = "ReportContentFragment"
        private const val EXTRA_REPORT_CONTENT = "EXTRA_REPORT_CONTENT"
        fun newInstance(model: ReportContentModel): ReportContentFragment {
            return ReportContentFragment().apply {
                arguments = Bundle().apply { putParcelable(EXTRA_REPORT_CONTENT, model) }
            }
        }
    }
}
