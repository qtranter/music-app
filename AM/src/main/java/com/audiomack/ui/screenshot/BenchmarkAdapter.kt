package com.audiomack.ui.screenshot

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.BenchmarkModel
import timber.log.Timber

class BenchmarkAdapter(private val listener: BenchmarkListener) : RecyclerView.Adapter<BenchmarkViewHolder>() {

    private val benchmarks: MutableList<BenchmarkModel> = mutableListOf()

    interface BenchmarkListener {
        fun onBenchmarkTapped(benchmark: BenchmarkModel)
    }

    fun update(list: List<BenchmarkModel>) {
        this.benchmarks.clear()
        this.benchmarks.addAll(list)
        this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return benchmarks.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BenchmarkViewHolder {
        return BenchmarkViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_benchmark_grid, parent, false))
    }

    override fun onBindViewHolder(holder: BenchmarkViewHolder, position: Int) {
        try {
            val benchmark = benchmarks[position]
            holder.setup(benchmark, listener)
        } catch (e: Exception) {
            Timber.w(e)
        }
    }
}
