package com.audiomack.ui.screenshot

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.BenchmarkModel
import com.audiomack.model.BenchmarkType
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.extensions.drawableCompat
import com.audiomack.views.AMCustomFontTextView
import de.hdodenhof.circleimageview.CircleImageView

class BenchmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val mainLayout: FrameLayout = itemView.findViewById(R.id.mainLayout)
    private val ivBenchmark: ImageView = itemView.findViewById(R.id.ivBenchmark)
    private val ivBenchmarkOverlay: ImageView = itemView.findViewById(R.id.ivBenchmarkOverlay)
    private val ivBenchmarkBorder: CircleImageView = itemView.findViewById(R.id.ivBenchmarkBorder)
    private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
    private val tvTitle: AMCustomFontTextView = itemView.findViewById(R.id.tvTitle)
    private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
    private val tvNext: TextView = itemView.findViewById(R.id.tvNext)
    private val ivArtistIcon: ImageView = itemView.findViewById(R.id.ivArtistIcon)

    fun setup(
        benchmark: BenchmarkModel,
        listener: BenchmarkAdapter.BenchmarkListener
    ) {

        mainLayout.setOnClickListener { listener.onBenchmarkTapped(benchmark) }

        if (benchmark.type == BenchmarkType.VERIFIED || benchmark.type == BenchmarkType.AUTHENTICATED || benchmark.type == BenchmarkType.TASTEMAKER || benchmark.type == BenchmarkType.ON_AUDIOMACK) {
            PicassoImageLoader.load(ivBenchmark.context, benchmark.imageUrl, ivBenchmark)
            ivIcon.visibility = View.GONE
            ivArtistIcon.visibility = View.VISIBLE
            ivBenchmarkOverlay.visibility = View.GONE
        } else {
            PicassoImageLoader.load(ivBenchmark.context, benchmark.imageUrl, ivBenchmark)
            ivIcon.visibility = View.VISIBLE
            ivArtistIcon.visibility = View.GONE
            ivBenchmarkOverlay.visibility = View.VISIBLE
        }

        if (benchmark.selected) {
            ivBenchmarkBorder.borderWidth = ivBenchmarkBorder.context.convertDpToPixel(3f)
            ivBenchmarkBorder.borderColor = ivBenchmarkBorder.context.colorCompat(R.color.orange)
        } else {
            ivBenchmarkBorder.borderWidth = ivBenchmarkBorder.context.convertDpToPixel(1f)
            ivBenchmarkBorder.borderColor = ivBenchmarkBorder.context.colorCompat(R.color.white_alpha10)
        }

        when (benchmark.type) {
            BenchmarkType.NONE -> {
                ivIcon.setImageDrawable(ivIcon.context.drawableCompat(R.drawable.ic_share_image_80))
                tvTitle.text = tvTitle.context.resources.getString(R.string.benchmark_now)
                tvSubtitle.text = benchmark.type.getTitle(tvSubtitle.context)
            }
            BenchmarkType.PLAY -> {
                ivIcon.setImageDrawable(ivIcon.context.drawableCompat(R.drawable.ic_benchmark_small_play))
                tvTitle.text = benchmark.getPrettyMilestone(tvTitle.context)
                tvSubtitle.text = benchmark.type.getTitle(tvSubtitle.context)
            }
            BenchmarkType.FAVORITE -> {
                ivIcon.setImageDrawable(ivIcon.context.drawableCompat(R.drawable.ic_benchmark_small_favorite))
                tvTitle.text = benchmark.getPrettyMilestone(tvTitle.context)
                tvSubtitle.text = benchmark.type.getTitle(tvSubtitle.context)
            }
            BenchmarkType.PLAYLIST -> {
                ivIcon.setImageDrawable(ivIcon.context.drawableCompat(R.drawable.ic_benchmark_small_playlist))
                tvTitle.text = benchmark.getPrettyMilestone(tvTitle.context)
                tvSubtitle.text = benchmark.type.getTitle(tvSubtitle.context)
            }
            BenchmarkType.REPOST -> {
                ivIcon.setImageDrawable(ivIcon.context.drawableCompat(R.drawable.ic_benchmark_small_repost))
                tvTitle.text = benchmark.getPrettyMilestone(tvTitle.context)
                tvSubtitle.text = benchmark.type.getTitle(tvSubtitle.context)
            }
            BenchmarkType.VERIFIED -> {
                ivArtistIcon.setImageDrawable(ivArtistIcon.context.drawableCompat(R.drawable.ic_verified))
                tvTitle.text = tvTitle.context.resources.getString(R.string.benchmark_verified)
                tvSubtitle.text = tvSubtitle.context.resources.getString(R.string.benchmark_artist)
            }
            BenchmarkType.TASTEMAKER -> {
                ivArtistIcon.setImageDrawable(ivArtistIcon.context.drawableCompat(R.drawable.ic_tastemaker))
                tvTitle.text = tvTitle.context.resources.getString(R.string.benchmark_tastemaker)
                tvSubtitle.text = tvSubtitle.context.resources.getString(R.string.benchmark_artist)
            }
            BenchmarkType.AUTHENTICATED -> {
                ivArtistIcon.setImageDrawable(ivArtistIcon.context.drawableCompat(R.drawable.ic_authenticated))
                tvTitle.text = tvTitle.context.resources.getString(R.string.benchmark_authenticated)
                tvSubtitle.text = tvSubtitle.context.resources.getString(R.string.benchmark_artist)
            }
            BenchmarkType.ON_AUDIOMACK -> {
                benchmark.badgeIconId?.let { resId ->
                    ivArtistIcon.setImageDrawable(ivArtistIcon.context.drawableCompat(resId))
                } ?: ivArtistIcon.setImageDrawable(null)
                tvTitle.text = tvTitle.context.resources.getString(R.string.benchmark_now)
                tvSubtitle.text = tvSubtitle.context.resources.getString(R.string.benchmark_on_audiomack)
            }
        }

        tvNext.text = benchmark.nextMilestone()?.let { nextMilestone ->
            String.format(tvNext.context.getString(R.string.benchmark_next_milestone), nextMilestone)
        }

        tvTitle.applyGradient()
    }
}
