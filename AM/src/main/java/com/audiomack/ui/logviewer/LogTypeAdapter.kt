package com.audiomack.ui.logviewer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.logviewer.LogType
import com.audiomack.utils.extensions.drawableCompat

class LogTypeAdapter(private val types: List<LogType>, val onTypeChanged: (LogType) -> Unit) : RecyclerView.Adapter<LogTypeViewHolder>() {

    private var selectedType: LogType? = null
        set(value) {
            value?.let {
                onTypeChanged(it)
            }
            field = value
            notifyDataSetChanged()
        }

    init {
        selectedType = types.firstOrNull()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogTypeViewHolder {
        return LogTypeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_log_type, parent, false))
    }

    override fun getItemCount(): Int {
        return types.size
    }

    override fun onBindViewHolder(holder: LogTypeViewHolder, position: Int) {
        holder.setup(types[position].name, types[position] == selectedType)
        holder.itemView.setOnClickListener { selectedType = types[holder.adapterPosition] }
    }
}

class LogTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvType = itemView.findViewById<TextView>(R.id.tvType)

    fun setup(name: String, selected: Boolean) {
        tvType.text = name
        tvType.setCompoundDrawablesWithIntrinsicBounds(tvType.context.drawableCompat(if (selected) R.drawable.ic_check_on else R.drawable.ic_check_off), null, null, null)
    }
}
