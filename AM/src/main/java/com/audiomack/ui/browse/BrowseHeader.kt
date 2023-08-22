package com.audiomack.ui.browse

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.AMGenre
import com.audiomack.ui.filter.FilterData
import com.audiomack.utils.extensions.drawableCompat

class BrowseHeader : LinearLayout {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutBanner: ViewGroup
    private lateinit var tvBannerMessage: TextView
    private lateinit var buttonBannerClose: ImageButton

    private var adapter: BrowseGenresAdapter? = null

    private val allGenres = listOf(
        AMGenre.All, AMGenre.Rap, AMGenre.Afrobeats, AMGenre.Latin, AMGenre.Dancehall, AMGenre.Rnb, AMGenre.Pop, AMGenre.Electronic, AMGenre.Rock, AMGenre.Instrumental, AMGenre.Podcast
    )

    var filter: FilterData? = null
    set(value) {
        field = value
        updateView()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.header_browse, this)
        recyclerView = findViewById(R.id.recyclerView)
        layoutBanner = findViewById(R.id.layoutBanner)
        tvBannerMessage = findViewById(R.id.tvBannerMessage)
        buttonBannerClose = findViewById(R.id.buttonBannerClose)
    }

    private fun updateView() {
        filter?.let { filterData ->

            adapter?.let { genresAdapter ->
                genresAdapter.selectedGenre = filterData.selection.genre ?: genresAdapter.selectedGenre
                genresAdapter.notifyDataSetChanged()
            } ?: run {
                val genres = allGenres.filterNot { filterData.excludedGenres.contains(it) }
                adapter = BrowseGenresAdapter(genres, filterData.selection.genre ?: genres.first()).also {
                    recyclerView.adapter = it
                }
            }
        }
    }

    fun setupBanner(message: String, clickListener: () -> Unit, dismissListener: () -> Unit) {
        layoutBanner.visibility = View.VISIBLE
        tvBannerMessage.text = message
        layoutBanner.setOnClickListener {
            clickListener.invoke()
            dismissListener.invoke()
            layoutBanner.visibility = View.GONE
        }
        buttonBannerClose.setOnClickListener {
            dismissListener.invoke()
            layoutBanner.visibility = View.GONE
        }
    }

    fun onGenreClick(listener: (FilterData) -> Unit) {
        adapter?.listener = { genre ->
            filter?.selection?.genre = genre
            filter?.let { listener.invoke(it) }
        }
    }
}

private class BrowseGenresAdapter(val genres: List<AMGenre>, var selectedGenre: AMGenre = genres.first()) : RecyclerView.Adapter<BrowseGenreViewHolder>() {

    var listener: ((AMGenre) -> Unit)? = null

    override fun getItemCount(): Int = genres.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowseGenreViewHolder =
        BrowseGenreViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_brose_genre_pill, parent, false))

    override fun onBindViewHolder(holder: BrowseGenreViewHolder, position: Int) {
        holder.setup(genres[position].humanValue(holder.itemView.context), genres[position] == selectedGenre)
        holder.itemView.setOnClickListener {
            holder.adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                selectedGenre = genres[position]
                listener?.invoke(selectedGenre)
                notifyDataSetChanged()
            }
        }
    }
}

private class BrowseGenreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvPill = itemView.findViewById<TextView>(R.id.tvPill)

    fun setup(text: String, selected: Boolean) {
        tvPill.text = text
        tvPill.background = tvPill.context.drawableCompat(if (selected) R.drawable.browse_genre_pill_selected else R.drawable.browse_genre_pill_normal)
    }
}
