package com.audiomack.ui.screenshot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.model.ScreenshotModel

class ScreenshotActivity : AppCompatActivity() {

    private val viewModel: ScreenshotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screenshot)

        initViewModelObservers()

        if (savedInstanceState == null) {
            val screenshot = intent.extras?.getParcelable(EXTRA_SHARE_SCREENSHOT) as? ScreenshotModel
                    ?: throw IllegalStateException("No screenshot specified in bundled extras")

            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.container, ScreenshotFragment.newInstance(screenshot))
                    .commit()
        }
    }

    override fun onBackPressed() {
        viewModel.swipeDownEvent.call()
    }

    private fun initViewModelObservers() {
        viewModel.closeEvent.observe(this, closeEventObserver)
    }

    private val closeEventObserver = Observer<Void> {
        finish()
        this.overridePendingTransition(0, 0)
    }

    companion object {
        private const val EXTRA_SHARE_SCREENSHOT = "EXTRA_SHARE_SCREENSHOT"

        fun show(context: Context?, model: ScreenshotModel) {
            val intent = Intent(context, ScreenshotActivity()::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(EXTRA_SHARE_SCREENSHOT, model)
            context?.startActivity(intent)
        }
    }
}
