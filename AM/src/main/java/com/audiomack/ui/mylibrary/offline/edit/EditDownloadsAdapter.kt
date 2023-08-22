package com.audiomack.ui.mylibrary.offline.edit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMResultItem
import com.audiomack.utils.ItemTouchHelperAdapter
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString

class EditDownloadsAdapter(
    private val removeListener: (AMResultItem) -> Unit,
    private val selectionChangedListener: (List<AMResultItem>) -> Unit,
    private val noMoreItemsListener: () -> Unit
) : RecyclerView.Adapter<EditDownloadsViewHolder>(), ItemTouchHelperAdapter {

    private val musicList = mutableListOf<AMResultItem>()
    private val selectedMusicList = mutableListOf<AMResultItem>()

    fun setMusicList(musicList: List<AMResultItem>) {
        this.musicList.clear()
        this.musicList.addAll(musicList)
        notifyDataSetChanged()
    }

    fun removeSelectedMusic() {
        musicList.removeAll { selectedMusicList.contains(it) }
        selectedMusicList.clear()
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = EditDownloadsViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.row_multi_select, parent, false)
    )

    override fun getItemCount() = musicList.size

    override fun onBindViewHolder(holder: EditDownloadsViewHolder, position: Int) {
        val music = musicList[position]
        holder.setup(music, selectedMusicList.contains(music))
        holder.itemView.setOnClickListener {
            val index = holder.adapterPosition.takeIf { it != -1 } ?: return@setOnClickListener
            val thisMusic = musicList[index]
            selectedMusicList.indexOf(thisMusic).takeIf { it != -1 }?.let {
                selectedMusicList.removeAt(it)
            } ?: selectedMusicList.add(thisMusic)
            selectionChangedListener(selectedMusicList)
            notifyItemChanged(index)
        }
    }

    override fun onItemDismiss(position: Int) {
        val music = musicList[position]
        removeListener(music)
        selectedMusicList.remove(music)
        musicList.removeAt(position)
        notifyItemRemoved(position)
        notifySelectionChanged()
    }

    override fun onItemMove(from: Int, to: Int) { /* no-op */
    }

    override fun onMoveComplete(start: Int, end: Int) { /* no-op */
    }

    private fun notifySelectionChanged() {
        selectionChangedListener(selectedMusicList)
        if (musicList.isEmpty()) {
            noMoreItemsListener()
        }
    }
}

class EditDownloadsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val imageViewMultiSelect = itemView.findViewById<ImageView>(R.id.imageViewMultiSelect)
    private val imageView = itemView.findViewById<ImageView>(R.id.imageView)
    private val imageViewPremium = itemView.findViewById<ImageView>(R.id.imageViewPremium)
    private val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
    private val tvArtist = itemView.findViewById<TextView>(R.id.tvArtist)
    private val bgAlbum = itemView.findViewById<View>(R.id.bgAlbum)
    private val ivLocalFile = itemView.findViewById<View>(R.id.ivLocalFile)

    fun setup(music: AMResultItem, selected: Boolean) {
        imageViewMultiSelect.setImageResource(if (selected) R.drawable.ic_multiselect_on else R.drawable.ic_multiselect_off)
        PicassoImageLoader.load(
            imageView.context,
            music.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
            imageView,
            R.drawable.ic_artwork
        )
        imageViewPremium.visibility =
            if (music.downloadType == AMResultItem.MusicDownloadType.Free) View.GONE else View.VISIBLE
        val featString = if (music.featured.isNullOrBlank()) "" else String.format(
            " %s %s",
            tvTitle.resources.getString(R.string.feat_inline),
            music.featured
        )
        tvTitle.text = tvTitle.context.spannableString(
            fullString = String.format("%s%s", music.title, featString),
            highlightedStrings = listOf(featString),
            highlightedColor = tvTitle.context.colorCompat(R.color.orange),
            highlightedFont = R.font.opensans_semibold
        )
        tvArtist.text = music.artist
        bgAlbum.isVisible = music.isAlbum
        ivLocalFile.isVisible = music.isLocal
    }
}
