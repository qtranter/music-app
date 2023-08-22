package com.audiomack.ui.highlights

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.audiomack.R
import com.audiomack.model.AMResultItem
import com.audiomack.model.EventHighlightsUpdated
import com.audiomack.utils.ReorderRecyclerViewItemTouchHelper
import com.audiomack.views.AMProgressHUD
import kotlinx.android.synthetic.main.activity_edit_highlights.*
import org.greenrobot.eventbus.EventBus

class EditHighlightsActivity : FragmentActivity() {

    private val viewModel: EditHighlightsViewModel by viewModels()
    private lateinit var adapter: EditHighlightsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_highlights)

        viewModel.close.observe(this, Observer {
            finish()
        })
        viewModel.saveResult.observe(this, Observer {
            it?.let { status ->
                when (status) {
                    EditHighlightsStatus.InProgress -> AMProgressHUD.showWithStatus(this@EditHighlightsActivity)
                    EditHighlightsStatus.Failed -> AMProgressHUD.showWithError(this@EditHighlightsActivity, getString(R.string.generic_api_error))
                    EditHighlightsStatus.Succeeded -> {
                        AMProgressHUD.dismiss()
                        EventBus.getDefault().post(EventHighlightsUpdated())
                        finish()
                    }
                }
            }
        })
        viewModel.highlightsReady.observe(this, Observer {
            it?.let { highlights ->
                if (!::adapter.isInitialized) {
                    val mutableHighlights = mutableListOf<AMResultItem>().apply { addAll(highlights) }
                    adapter = EditHighlightsAdapter(mutableHighlights)
                    recyclerView.layoutManager = LinearLayoutManager(
                        recyclerView.context,
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                    recyclerView.adapter = adapter
                    recyclerView.setHasFixedSize(true)
                    val itemTouchHelper = ReorderRecyclerViewItemTouchHelper(adapter)
                    ItemTouchHelper(itemTouchHelper).attachToRecyclerView(recyclerView)
                }
            }
        })
        viewModel.loadingStatus.observe(this, Observer {
            if (it) animationView.show()
            else animationView.hide()
        })

        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
        buttonSave.setOnClickListener {
            if (!::adapter.isInitialized) {
                viewModel.onCloseTapped()
            } else {
                viewModel.onSaveTapped(adapter.items)
            }
        }

        viewModel.onHighlightsRequested()
    }

    companion object {
        @JvmStatic
        fun show(activity: Activity?) {
            activity?.let {
                val intent = Intent(it, EditHighlightsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(intent)
            }
        }
    }
}
