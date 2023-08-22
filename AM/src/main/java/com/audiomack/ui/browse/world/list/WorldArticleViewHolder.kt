package com.audiomack.ui.browse.world.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.R
import com.audiomack.model.WorldArticle
import com.squareup.picasso.Picasso

class WorldArticleViewHolder(
    view: View,
    private val onClickListener: (String) -> Unit
) : RecyclerView.ViewHolder(view) {

    private val tagName: TextView = view.findViewById(R.id.tvTagName)
    private val title: TextView = view.findViewById(R.id.tvPostTitle)
    private val description: TextView = view.findViewById(R.id.tvPostDescription)
    private val imageView: ImageView = view.findViewById(R.id.ivPost)

    private val picasso = Picasso.get()

    private var post: WorldArticle? = null

    init {
        view.setOnClickListener {
            onClickListener(post?.slug ?: "")
        }
    }

    fun bind(post: WorldArticle?) {
        if (post == null) {
            tagName.text = null
            title.text = null
            description.text = null
        } else {
            showPostData(post)
        }
    }

    private fun showPostData(post: WorldArticle) {
        this.post = post
        tagName.text = post.tagName
        title.text = post.title
        description.text = post.excerpt

        if (post.feature_image?.startsWith("http") == true) {
            picasso.load(post.feature_image)
                    .fit()
                    .centerCrop()
                    .into(imageView)
        } else {
            picasso.cancelRequest(imageView)
        }
    }

    companion object {
        fun create(parent: ViewGroup, onClickListener: (String) -> Unit): WorldArticleViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.row_world_article, parent, false)
            return WorldArticleViewHolder(view, onClickListener)
        }
    }
}
