package com.audiomack.ui.logviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R

class LogViewerAdapter(var data: MutableList<String>) : RecyclerView.Adapter<LogViewerViewHolder>() {

    fun updateData(freshData: List<String>) {
        data.clear()
        data.addAll(freshData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewerViewHolder {
        return LogViewerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_logviewer, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: LogViewerViewHolder, position: Int) {
        holder.setup(data[position])
    }
}

class LogViewerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvLog = itemView.findViewById<TextView>(R.id.tvLog)

    fun setup(log: String) {
        tvLog.text = log
    }
}
