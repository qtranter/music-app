package com.audiomack.ui.queue

import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.activities.BaseActivity
import com.audiomack.data.tracking.mixpanel.MixpanelPageQueue
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMResultItem
import com.audiomack.model.MixpanelSource
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.mylibrary.offline.local.menu.SlideUpMenuLocalMediaFragment
import com.audiomack.ui.slideupmenu.music.SlideUpMenuMusicFragment
import com.audiomack.ui.tooltip.TooltipCorner
import com.audiomack.ui.tooltip.TooltipFragment
import com.audiomack.utils.ReorderRecyclerViewItemTouchHelper
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.gt
import kotlinx.android.synthetic.main.fragment_queue.btnOverflow
import kotlinx.android.synthetic.main.fragment_queue.buttonBack
import kotlinx.android.synthetic.main.fragment_queue.recyclerView
import timber.log.Timber

class QueueFragment : TrackedFragment(R.layout.fragment_queue, TAG) {

    private val viewModel: QueueViewModel by viewModels()
    private lateinit var adapter: QueueAdapter

    private val rowHeight: Int = context?.convertDpToPixel(60.0F) ?: 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.backEvent.observe(viewLifecycleOwner, Observer {
            activity?.onBackPressed()
        })
        viewModel.refreshData.observe(viewLifecycleOwner, Observer {
            adapter.notifyDataSetChanged()
        })
        viewModel.queue.observe(viewLifecycleOwner, Observer {
            adapter.update(it)
        })
        viewModel.showOptionsEvent.observe(viewLifecycleOwner, Observer { (track, index) ->
            val moreThanOneItem = viewModel.queue.value?.size.gt(1)
            val fragment = if (track.isLocal) {
                SlideUpMenuLocalMediaFragment.newInstance(track.itemId.toLong(), index)
            } else {
                SlideUpMenuMusicFragment.newInstance(track, mixpanelSource,
                    removeFromDownloadsEnabled = false,
                    removeFromQueueEnabled = moreThanOneItem,
                    removeFromQueueIndex = index
                )
            }
            (activity as? BaseActivity)?.openOptionsFragment(fragment)
        })
        viewModel.showTooltip.observe(viewLifecycleOwner, Observer {
            try {
                val rect = Rect()
                btnOverflow.getGlobalVisibleRect(rect)
                val targetCenter = Point(rect.left + btnOverflow!!.width / 2, rect.top)
                val tooltipFragment = TooltipFragment.newInstance(
                    getString(R.string.tooltip_queue_add),
                    R.drawable.tooltip_queue_add,
                    TooltipCorner.BOTTOMRIGHT,
                    arrayListOf(targetCenter),
                    Runnable { viewModel.onTooltipClosed() })
                (activity as? HomeActivity)?.openTooltipFragment(tooltipFragment)
            } catch (e: Exception) {
                Timber.w(e)
            }
        })
        viewModel.setCurrentSongEvent.observe(viewLifecycleOwner, Observer { currentIndex ->
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(currentIndex, rowHeight / 2)
        })

        buttonBack.setOnClickListener { viewModel.onBackTapped() }

        adapter = QueueAdapter(object : QueueAdapter.QueueListener {
            override fun didTapSong(index: Int) {
                viewModel.onSongTapped(index)
            }

            override fun didMoveSong(fromIndex: Int, toIndex: Int) {
                viewModel.onSongMoved(fromIndex, toIndex)
            }

            override fun didDeleteSong(index: Int) {
                viewModel.onSongDeleted(index)
            }

            override fun didTapKekab(item: AMResultItem?, index: Int) {
                viewModel.didTapKebab(item, index)
            }

            override fun didDeleteCurrentlyPlayingSong() {
                viewModel.didDeleteCurrentlyPlayingSong()
            }
        }, viewModel.queueDataSource)

        recyclerView.adapter = adapter
        recyclerView.layoutManager?.isItemPrefetchEnabled = true
        recyclerView.setHasFixedSize(true)

        val itemTouchHelper = ReorderRecyclerViewItemTouchHelper(adapter)
        ItemTouchHelper(itemTouchHelper).attachToRecyclerView(recyclerView)

        activity?.let { viewModel.onCreate(it) }

        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                viewModel.scrollToCurrentlyPlayingSong()
            }
        })

        btnOverflow.setOnClickListener {
            childFragmentManager.commit {
                add(R.id.container, SlideUpMenuQueueFragment.newInstance())
                addToBackStack(SlideUpMenuQueueFragment.TAG)
            }
        }
    }

    private val mixpanelSource: MixpanelSource
        get() = MixpanelSource(MainApplication.currentTab, MixpanelPageQueue, emptyList())

    companion object {
        const val TAG = "QueueFragment"
        fun newInstance(): QueueFragment = QueueFragment()
    }
}
