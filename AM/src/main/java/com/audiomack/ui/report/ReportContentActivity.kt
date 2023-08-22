package com.audiomack.ui.report

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.model.ReportContentModel
import kotlinx.android.synthetic.main.activity_report_content.buttonClose

class ReportContentActivity : AppCompatActivity() {

    private val viewModel: ReportContentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_content)

        initViewClickListeners()
        initViewModelObservers()

        if (savedInstanceState == null) {
            val model = intent.extras?.getParcelable(EXTRA_REPORT_CONTENT) as? ReportContentModel
                ?: throw IllegalStateException("No model specified in arguments")

            supportFragmentManager
                .beginTransaction()
                .add(R.id.container, ReportContentFragment.newInstance(model))
                .commit()
        }
    }

    private fun initViewModelObservers() {
        val lifecycleOwner = this
        viewModel.apply {
            closeEvent.observe(lifecycleOwner, closeEventObserver)
        }
    }

    private fun initViewClickListeners() {
        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
    }

    private val closeEventObserver = Observer<Void> {
        finish()
    }

    companion object {
        private const val EXTRA_REPORT_CONTENT = "EXTRA_REPORT_CONTENT"
        fun show(context: Context?, model: ReportContentModel) {
            val intent = Intent(context, ReportContentActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(EXTRA_REPORT_CONTENT, model)
            context?.startActivity(intent)
        }
    }
}
