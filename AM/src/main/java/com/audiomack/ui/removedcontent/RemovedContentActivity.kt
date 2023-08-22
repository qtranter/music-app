package com.audiomack.ui.removedcontent

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.utils.spannableString
import kotlinx.android.synthetic.main.activity_removedcontent.*

class RemovedContentActivity : androidx.fragment.app.FragmentActivity() {

    private val viewModel: RemovedContentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_removedcontent)

        viewModel.close.observe(this, Observer {
            finish()
        })
        viewModel.ok.observe(this, Observer {
            finish()
        })

        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
        buttonOK.setOnClickListener { viewModel.onOkTapped() }

        val items = viewModel.items

        items.forEach {
            val title = " - ${it.title}"
            val artistAndTitle = "${it.artist} $title"
            val artistAndTitleSpannable = container.context.spannableString(
                fullString = artistAndTitle,
                highlightedStrings = listOf(title),
                highlightedColor = Color.WHITE,
                highlightedFont = R.font.opensans_bold
            )
            val row = LayoutInflater.from(container.context).inflate(R.layout.row_removedcontent, container, false)
            row.findViewById<TextView>(R.id.tvContent).text = artistAndTitleSpannable
            container.addView(row)
        }

        viewModel.clearItems()

        viewModel.trackScreen()
    }

    companion object {
        @JvmStatic
        fun show(activity: Activity?) {
            activity?.let {
                val intent = Intent(it, RemovedContentActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(intent)
            }
        }
    }
}
