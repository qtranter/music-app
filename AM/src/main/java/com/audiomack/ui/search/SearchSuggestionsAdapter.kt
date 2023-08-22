package com.audiomack.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R

class SearchSuggestionsAdapter(private val tapHandler: (String) -> Unit) : RecyclerView.Adapter<SearchSuggestionsViewHolder>() {

    private val suggestions: MutableList<String> = mutableListOf()
    private var highlight: String = ""

    fun updateSuggestions(newSuggestions: List<String>, query: String) {
        suggestions.clear()
        suggestions.addAll(newSuggestions)
        highlight = query
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchSuggestionsViewHolder {
        return SearchSuggestionsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_search_suggestion, parent, false))
    }

    override fun getItemCount(): Int {
        return suggestions.size
    }

    override fun onBindViewHolder(holder: SearchSuggestionsViewHolder, position: Int) {
        holder.setup(suggestions[position], highlight)
        holder.itemView.setOnClickListener {
            if (holder.adapterPosition >= 0 && holder.adapterPosition < suggestions.size) {
                tapHandler(suggestions[holder.adapterPosition])
            }
        }
    }
}
