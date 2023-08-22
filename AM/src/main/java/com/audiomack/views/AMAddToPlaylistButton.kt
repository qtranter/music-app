package com.audiomack.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import com.audiomack.R
import com.audiomack.model.AMMusicButtonModel
import com.audiomack.model.AMResultItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class AMAddToPlaylistButton : FrameLayout {

    private lateinit var progressBar: AMProgressBar
    lateinit var button: ImageButton

    private var model: AMMusicButtonModel? = null

    private var disposable: Disposable? = null
    private var disabledAnimations = false

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
        View.inflate(context, R.layout.view_add_to_playlist_button, this)
        this.progressBar = findViewById(R.id.progress)
        this.button = findViewById(R.id.button)
    }

    fun set(model: AMMusicButtonModel) {
        this.model = model

        disposable?.dispose()
        disabledAnimations = true

        val subject = model.item.addToPlaylistSubject

        if (subject != null) {
            disposable = subject
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ updateView() }, { Timber.d(it) })
        }
    }

    private fun updateView() {

        val model = model ?: return

        val status = model.item.addToPlaylistStatus
        button.setImageResource(if (status == AMResultItem.ItemAPIStatus.Off) R.drawable.playlist_unselected else R.drawable.playlist_selected)
        button.setOnClickListener(if (status == AMResultItem.ItemAPIStatus.Loading) null else model.onClickListener)
        button.isClickable = status != AMResultItem.ItemAPIStatus.Loading
        progressBar.visibility =
            if (status != AMResultItem.ItemAPIStatus.Loading) View.GONE else View.VISIBLE
        button.visibility =
            if (status == AMResultItem.ItemAPIStatus.Loading) View.GONE else View.VISIBLE

        disabledAnimations = false
    }
}
