package com.audiomack.ui.logviewer

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.audiomack.R
import com.audiomack.data.logviewer.LogType
import com.audiomack.utils.extensions.colorCompat
import kotlinx.android.synthetic.main.activity_logviewer.*
import timber.log.Timber

class LogViewerActivity : FragmentActivity() {

    private val viewModel: LogViewerViewModel by viewModels()
    private lateinit var logsAdapter: LogViewerAdapter
    private lateinit var typeAdapter: LogTypeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logviewer)

        initViewModelObservers()

        logsAdapter = LogViewerAdapter(mutableListOf())
        recyclerView.adapter = logsAdapter
        recyclerView.setHasFixedSize(true)

        typeAdapter = LogTypeAdapter(LogType.values().toList(), onTypeChanged = {
            viewModel.onTypeChanged(it)
        })
        recyclerViewType.adapter = typeAdapter
        recyclerViewType.setHasFixedSize(true)

        buttonBack.setOnClickListener { viewModel.onBackTapped() }
        buttonShare.setOnClickListener { viewModel.onShareTapped() }

        swipeRefreshLayout.setColorSchemeColors(swipeRefreshLayout.context.colorCompat(R.color.orange))
        swipeRefreshLayout.isHapticFeedbackEnabled = true
        swipeRefreshLayout.setOnRefreshListener { viewModel.onRefreshTriggered() }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            closeEvent.observe(this@LogViewerActivity) {
                finish()
            }
            shareEvent.observe(this@LogViewerActivity) {
                try {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, it)
                    startActivity(Intent.createChooser(intent, "Share"))
                } catch (e: ActivityNotFoundException) {
                    Timber.w(e)
                }
            }
            logs.observe(this@LogViewerActivity) {
                logsAdapter.updateData(it)
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    companion object {
        fun show(activity: Activity?) {
            activity?.let {
                val intent = Intent(it, LogViewerActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(intent)
            }
        }
    }
}
