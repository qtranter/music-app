package com.audiomack.ui.queue

import android.view.LayoutInflater
import android.view.ViewGroup
import com.audiomack.R
import com.audiomack.adapters.viewholders.EmptyViewHolder
import com.audiomack.data.queue.QueueDataSource
import com.audiomack.model.AMResultItem
import com.audiomack.utils.ItemTouchHelperAdapter
import java.util.Collections
import timber.log.Timber

class QueueAdapter(private val listener: QueueListener, private val queueDataSource: QueueDataSource) :
    androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>(),
    ItemTouchHelperAdapter {

    private var queue: MutableList<AMResultItem> = mutableListOf()

    interface QueueListener {
        fun didTapSong(index: Int)
        fun didMoveSong(fromIndex: Int, toIndex: Int)
        fun didDeleteSong(index: Int)
        fun didTapKekab(item: AMResultItem?, index: Int)
        fun didDeleteCurrentlyPlayingSong()
    }

    override fun getItemCount(): Int {
        return queue.size
    }

    fun update(newQueue: List<AMResultItem>) {
        this.queue.clear()
        this.queue.addAll(newQueue)
        this.notifyDataSetChanged()
    }

    fun remove(index: Int) {
        this.queue.removeAt(index)
        this.notifyItemRemoved(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        try {
            return QueueViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.row_queue,
                    parent,
                    false
                ),
                queueDataSource
            )
        } catch (e: Exception) {
            Timber.w(e)
            return EmptyViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.row_empty,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        try {
            if (holder is QueueViewHolder) {
                holder.setup(queue[position])
                holder.buttonKebab.setOnClickListener {
                    listener.didTapKekab(
                        holder.item,
                        holder.getAdapterPosition()
                    )
                }
                holder.itemView.setOnClickListener { listener.didTapSong(holder.getAdapterPosition()) }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    override fun onItemMove(from: Int, to: Int) {
        Collections.swap(queue, from, to)
        notifyItemMoved(from, to)
    }

    override fun onMoveComplete(start: Int, end: Int) {
        listener.didMoveSong(start, end)
    }

    override fun onItemDismiss(position: Int) {
        val currentItem = queue[position]
        if (!queueDataSource.isCurrentItemOrParent(currentItem)) {
            remove(position)
            listener.didDeleteSong(position)
        } else {
            listener.didDeleteCurrentlyPlayingSong()
            remove(position)
        }
    }
}
