package com.audiomack.ui.notifications

import android.graphics.Color
import android.text.SpannableString
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.adapters.DataRecyclerViewAdapter
import com.audiomack.data.imageloader.PicassoImageLoader
import com.audiomack.model.AMNotification
import com.audiomack.model.AMResultItem
import com.audiomack.utils.AMClickableSpan
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.spannableString
import com.audiomack.utils.spannableStringWithImageAtTheEnd
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Locale
import org.ocpsoft.prettytime.PrettyTime
import timber.log.Timber

class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvTitle: AppCompatTextView = view.findViewById(R.id.tvTitle)
    private val tvComment: AppCompatTextView = view.findViewById(R.id.tvComment)
    private val tvDate: TextView = view.findViewById(R.id.tvDate)
    private val imageViewActor: CircleImageView = view.findViewById(R.id.imageViewActor)
    private val imageViewLogo: ImageView = view.findViewById(R.id.imageViewLogo)
    private val imageViewObject: ImageView = view.findViewById(R.id.imageViewObject)
    private val viewUnseen: View = view.findViewById(R.id.viewUnseen)

    fun setup(
        notification: AMNotification,
        position: Int,
        listener: DataRecyclerViewAdapter.RecyclerViewListener?
    ) {

        var title: String? = ""

        itemView.setPadding(
            itemView.paddingLeft,
            itemView.context.convertDpToPixel(
                (if (position == 0) 20 else 10).toFloat()
            ),
            itemView.paddingRight,
            itemView.paddingBottom
        )

        if (notification.verb === AMNotification.AMNotificationVerb.Benchmark) {
            imageViewActor.visibility = View.GONE
            imageViewLogo.visibility = View.VISIBLE
            (notification.`object` as? AMResultItem)?.let { music ->
                imageViewLogo.setOnClickListener {
                    when (notification.type) {
                        is AMNotification.NotificationType.UpvoteComment -> listener?.onClickNotificationCommentUpvote(music, (notification.type as AMNotification.NotificationType.UpvoteComment).data, notification.type)
                        is AMNotification.NotificationType.Benchmark -> listener?.onClickNotificationBenchmark(music, (notification.type as AMNotification.NotificationType.Benchmark).benchmark, notification.type)
                    }
                }
            }
        } else {
            imageViewActor.visibility = View.VISIBLE
            imageViewLogo.visibility = View.GONE
            notification.author?.let { author ->
                PicassoImageLoader.load(imageViewActor.context, author.image, imageViewActor)
                imageViewActor.setOnClickListener { listener?.onClickNotificationArtist(author.slug, notification.type) }
            } ?: run {
                imageViewActor.setImageDrawable(null)
            }
        }

        viewUnseen.visibility = if (notification.isSeen) View.INVISIBLE else View.VISIBLE

        tvDate.text = if ((notification.createdAt?.time ?: 0) > 0) {
            PrettyTime(Locale.US).format(notification.createdAt)
        } else ""

        if (notification.verb === AMNotification.AMNotificationVerb.Favorite || notification.verb === AMNotification.AMNotificationVerb.Repost || notification.verb === AMNotification.AMNotificationVerb.Playlist || notification.verb === AMNotification.AMNotificationVerb.FavoritePlaylist || notification.verb === AMNotification.AMNotificationVerb.Comment || notification.verb === AMNotification.AMNotificationVerb.Benchmark) {
            val item = notification.`object` as AMResultItem?
            title = if ((notification.type as? AMNotification.NotificationType.Comment)?.commentReply != null) "" else item?.title ?: ""
            PicassoImageLoader.load(
                imageViewObject.context,
                item?.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
                imageViewObject
            )
            imageViewObject.visibility = View.VISIBLE
        } else if (notification.verb === AMNotification.AMNotificationVerb.PlaylistUpdated) {
            val item = notification.target
            title = item?.title
            PicassoImageLoader.load(
                imageViewObject.context,
                item?.getImageURLWithPreset(AMResultItem.ItemImagePreset.ItemImagePresetSmall),
                imageViewObject
            )
            imageViewObject.visibility = View.VISIBLE
        } else {
            imageViewObject.setImageDrawable(null)
            imageViewObject.visibility = View.GONE
        }

        val verb = when (notification.verb) {
            AMNotification.AMNotificationVerb.Favorite -> tvTitle.resources.getString(R.string.notifications_verb_favorite)
            AMNotification.AMNotificationVerb.Repost -> tvTitle.resources.getString(R.string.notifications_verb_repost)
            AMNotification.AMNotificationVerb.Follow -> tvTitle.resources.getString(R.string.notifications_verb_follow)
            AMNotification.AMNotificationVerb.Playlist -> tvTitle.resources.getString(R.string.notifications_verb_playlist)
            AMNotification.AMNotificationVerb.FavoritePlaylist -> tvTitle.resources.getString(R.string.notifications_verb_favorite)
            AMNotification.AMNotificationVerb.PlaylistUpdated -> tvTitle.resources.getString(R.string.notifications_verb_added)
            AMNotification.AMNotificationVerb.Comment -> {
                val type = notification.type as AMNotification.NotificationType.Comment
                type.commentReply?.let {
                    tvTitle.resources.getString(R.string.notifications_verb_comment_reply)
                } ?: tvTitle.resources.getString(R.string.notifications_verb_comment)
            }
            AMNotification.AMNotificationVerb.Benchmark -> tvTitle.resources.getString(R.string.notifications_verb_benchmark)
            else -> ""
        }

        val actorSpannable = tvTitle.context.spannableString(
            fullString = notification.author?.name ?: "",
            highlightedStrings = listOf(notification.author?.name ?: ""),
            fullColor = Color.WHITE,
            highlightedColor = Color.WHITE,
            fullFont = R.font.opensans_bold,
            highlightedFont = R.font.opensans_bold,
            clickableSpans = listOf(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    notification.author?.slug?.let {
                        listener?.onClickNotificationArtist(it, notification.type)
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = Color.WHITE
                }
            })
        )

        val actorBadgeSpannable = when {
            notification.author?.verified ?: false -> tvTitle.spannableStringWithImageAtTheEnd("", R.drawable.ic_verified, 12)
            notification.author?.tastemaker ?: false -> tvTitle.spannableStringWithImageAtTheEnd("", R.drawable.ic_tastemaker, 12)
            notification.author?.authenticated ?: false -> tvTitle.spannableStringWithImageAtTheEnd("", R.drawable.ic_authenticated, 12)
            else -> SpannableString("")
        }

        val verbSpannable = tvTitle.context.spannableString(
            fullString = " $verb ",
            highlightedStrings = listOf(" $verb "),
            highlightedColor = tvTitle.context.colorCompat(R.color.gray_text),
            highlightedFont = R.font.opensans_semibold
        )

        val lastPartSpannable = tvTitle.context.spannableString(
            fullString = title ?: "",
            highlightedStrings = listOf(title ?: ""),
            fullColor = Color.WHITE,
            highlightedColor = Color.WHITE,
            fullFont = if (notification.verb == AMNotification.AMNotificationVerb.PlaylistUpdated) R.font.opensans_bold else R.font.opensans_semibold,
            highlightedFont = if (notification.verb == AMNotification.AMNotificationVerb.PlaylistUpdated) R.font.opensans_bold else R.font.opensans_semibold,
            clickableSpans = listOf(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    if (notification.verb === AMNotification.AMNotificationVerb.PlaylistUpdated) {
                        notification.target?.let {
                            listener?.onClickNotificationMusic(it, false, notification.type)
                        }
                    } else if (notification.`object` is AMResultItem) {
                        (notification.`object` as? AMResultItem)?.let {
                            listener?.onClickNotificationMusic(it, notification.verb == AMNotification.AMNotificationVerb.Comment, notification.type)
                        }
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = Color.WHITE
                }
            })
        )
        val titleSpannable = if (notification.verb === AMNotification.AMNotificationVerb.PlaylistUpdated) {
            val playlist = notification.`object` as? AMResultItem ?: return
            val playlistFullString =
                " " + tvTitle.resources.getString(R.string.notifications_verb_added_to_playlist) + " " + (playlist.title ?: "")
            val playlistSpannable = tvTitle.context.spannableString(
                fullString = playlistFullString,
                highlightedStrings = listOf(playlist.title ?: ""),
                fullColor = tvTitle.context.colorCompat(R.color.gray_text),
                highlightedColor = tvTitle.context.colorCompat(R.color.orange),
                fullFont = R.font.opensans_semibold,
                highlightedFont = R.font.opensans_semibold,
                clickableSpans = listOf(AMClickableSpan(tvTitle.context) { listener?.onClickNotificationMusic(playlist, false, notification.type) })
            )
            TextUtils.concat(actorSpannable, actorBadgeSpannable, verbSpannable, lastPartSpannable, playlistSpannable)
        } else if (notification.type is AMNotification.NotificationType.Comment) {
            val type = notification.type as AMNotification.NotificationType.Comment
            if (type.commentReply != null) {
                TextUtils.concat(actorSpannable, actorBadgeSpannable, verbSpannable.removeSuffix(" "), lastPartSpannable, ":")
            } else {
                TextUtils.concat(actorSpannable, actorBadgeSpannable, verbSpannable, lastPartSpannable, ":")
            }
        } else if (notification.type is AMNotification.NotificationType.UpvoteComment) {
            val type = notification.type as AMNotification.NotificationType.UpvoteComment
            val item = notification.`object` as? AMResultItem ?: return
            val prefixSpannable = tvTitle.context.spannableString(
                fullString = tvTitle.resources.getString(R.string.notifications_verb_benchmark_comment_upvote_prefix),
                fullFont = R.font.opensans_semibold
            )
            val musicNameSpannable = tvTitle.context.spannableString(
                fullString = item.title ?: "",
                fullColor = tvTitle.context.colorCompat(R.color.orange),
                fullFont = R.font.opensans_bold
            )
            val verbSpannable = tvTitle.context.spannableString(
                fullString = tvTitle.resources.getString(R.string.notifications_verb_benchmark_comment_upvote_verb),
                fullFont = R.font.opensans_semibold
            )
            val suffix = String.format(tvTitle.resources.getString(
                if (type.data.count == 1L) R.string.notifications_verb_benchmark_comment_one_upvote_suffix else R.string.notifications_verb_benchmark_comment_multiple_upvote_suffix
            ), type.data.count)
            val suffixSpannable = tvTitle.context.spannableString(
                fullString = suffix,
                fullColor = Color.WHITE,
                fullFont = R.font.opensans_bold
            )
            TextUtils.concat(prefixSpannable, " ", musicNameSpannable, " ", verbSpannable, " ", suffixSpannable)
        } else if (notification.type is AMNotification.NotificationType.Benchmark) {
            val type = notification.type as AMNotification.NotificationType.Benchmark
            val item = notification.`object` as? AMResultItem ?: return
            val itemString = item.title ?: ""
            val itemPrefix = when (item.type) {
                "playlist" -> R.string.notifications_verb_benchmark_your_playlist
                "album" -> R.string.notifications_verb_benchmark_your_album
                else -> R.string.notifications_verb_benchmark_your_song
            }
            val itemFullString = tvTitle.resources.getString(itemPrefix) + " " + itemString
            val itemSpannable = tvTitle.context.spannableString(
                fullString = itemFullString,
                highlightedStrings = listOf(itemString),
                fullColor = tvTitle.context.colorCompat(R.color.gray_text),
                highlightedColor = tvTitle.context.colorCompat(R.color.orange),
                fullFont = R.font.opensans_semibold,
                highlightedFont = R.font.opensans_bold,
                clickableSpans = listOf(AMClickableSpan(tvTitle.context) { listener?.onClickNotificationMusic(item, false, notification.type) })
            )
            val benchmarkFullString = type.benchmark.getPrettyMilestone(tvTitle.context) + " " + type.benchmark.type.getTitle(tvTitle.context) + "!"
            val benchmarkSpannable = tvTitle.context.spannableString(
                fullString = benchmarkFullString,
                highlightedStrings = listOf(benchmarkFullString),
                highlightedColor = tvTitle.context.colorCompat(R.color.orange),
                highlightedFont = R.font.opensans_bold,
                clickableSpans = listOf(AMClickableSpan(tvTitle.context) { listener?.onClickNotificationBenchmark(item, type.benchmark, notification.type) })
            )
            TextUtils.concat(itemSpannable, verbSpannable, benchmarkSpannable)
        } else {
            TextUtils.concat(actorSpannable, actorBadgeSpannable, verbSpannable, lastPartSpannable)
        }
        tvTitle.setTextFuture(PrecomputedTextCompat.getTextFuture(titleSpannable, tvTitle.textMetricsParamsCompat, null))
        try {
            tvTitle.movementMethod = LinkMovementMethod.getInstance()
        } catch (e: NoSuchMethodError) {
            Timber.w(e)
        }

        (notification.type as? AMNotification.NotificationType.Comment)?.let { type ->
            tvComment.visibility = View.VISIBLE
            val comment = if (type.commentReply != null) "\"${type.commentReply}\"" else type.comment
            tvComment.setTextFuture(PrecomputedTextCompat.getTextFuture(comment ?: "", tvComment.textMetricsParamsCompat, null))
        } ?: run { tvComment.visibility = View.GONE }

        imageViewActor.setOnClickListener {
            notification.author?.slug?.let {
                listener?.onClickNotificationArtist(it, notification.type)
            }
        }

        if (notification.type is AMNotification.NotificationType.UpvoteComment && notification.`object` is AMResultItem) {
            val upvoteCommentData = (notification.type as AMNotification.NotificationType.UpvoteComment).data
            tvTitle.setOnClickListener {
                listener?.onClickNotificationCommentUpvote(notification.`object` as AMResultItem, upvoteCommentData, notification.type)
            }
            itemView.setOnClickListener {
                listener?.onClickNotificationCommentUpvote(notification.`object` as AMResultItem, upvoteCommentData, notification.type)
            }
        } else if (notification.verb === AMNotification.AMNotificationVerb.Follow) {
            tvTitle.setOnClickListener(null)
            itemView.setOnClickListener {
                notification.author?.slug?.let {
                    listener?.onClickNotificationArtist(it, notification.type)
                }
            }
        } else if (notification.`object` is AMResultItem) {
            val item = notification.`object` as AMResultItem
            itemView.setOnClickListener {
                listener?.onClickNotificationMusic(item, notification.verb == AMNotification.AMNotificationVerb.Comment, notification.type)
            }
        }
    }
}
