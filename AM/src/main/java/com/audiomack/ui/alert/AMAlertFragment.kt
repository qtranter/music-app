package com.audiomack.ui.alert

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.audiomack.R
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.drawableCompat
import kotlinx.android.synthetic.main.fragment_alert.*
import timber.log.Timber

class AMAlertFragment : DialogFragment() {

    private lateinit var config: AlertConfig

    private var closingViaAnAction = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_alert, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle.text = config.title
        tvTitle.visibility = if (config.title.isNullOrBlank()) View.GONE else View.VISIBLE

        tvMessage.text = config.message
        tvMessage.visibility = if (config.message.isNullOrBlank()) View.GONE else View.VISIBLE

        buttonSolid.text = config.solidButton?.title
        buttonSolid.visibility = if (config.solidButton?.title.isNullOrBlank()) View.GONE else View.VISIBLE
        buttonSolid.setOnClickListener {
            closingViaAnAction = true
            close()
            config.solidButton?.handler?.run()
        }

        buttonOutline.text = config.outlineButton?.title
        buttonOutline.visibility = if (config.outlineButton?.title.isNullOrBlank()) View.GONE else View.VISIBLE
        buttonOutline.setOnClickListener {
            closingViaAnAction = true
            close()
            config.outlineButton?.handler?.run()
        }

        buttonPlain1.text = config.plain1Button?.title
        buttonPlain1.visibility = if (config.plain1Button?.title.isNullOrBlank()) View.GONE else View.VISIBLE
        buttonPlain1.setOnClickListener {
            closingViaAnAction = true
            close()
            config.plain1Button?.handler?.run()
        }

        buttonPlain2.text = config.plain2Button?.title
        buttonPlain2.visibility = if (config.plain2Button?.title.isNullOrBlank()) View.GONE else View.VISIBLE
        buttonPlain2.setOnClickListener {
            closingViaAnAction = true
            close()
            config.plain2Button?.handler?.run()
        }

        config.drawableResId?.let {
            ivIcon.visibility = View.VISIBLE
            ivIcon.setImageDrawable(ivIcon.context.drawableCompat(it))
            (tvTitle.layoutParams as? FrameLayout.LayoutParams)?.topMargin = (tvTitle.layoutParams as? FrameLayout.LayoutParams)?.topMargin?.plus(tvTitle.context.convertDpToPixel(20f))
        } ?: run {
            ivIcon.visibility = View.GONE
        }

        if (config.dismissOnTouchOutside) {
            view.setOnClickListener { close() }
        }

        buttonClose.setOnClickListener { close() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AudiomackDialogFragment)
        if (savedInstanceState != null && !::config.isInitialized) {
            dismiss()
            return
        }
        isCancelable = config.cancellable
    }

    private fun close() {
        try {
            dismiss()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    override fun dismiss() {
        if (!closingViaAnAction && ::config.isInitialized) {
            config.dismissWithoutSelectionHandler?.run()
        }
        super.dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        config.dismissHandler?.run()
        super.onDismiss(dialog)
    }

    override fun onCancel(dialog: DialogInterface) {
        config.cancelHandler?.run()
    }

    companion object {

        const val TAG = "AMAlertFragment"

        @Deprecated(message = "Use [Builder] instead")
        @JvmStatic
        @JvmOverloads
        fun show(
            activity: FragmentActivity,
            title: SpannableString,
            message: String?,
            positive: String,
            negative: String?,
            positiveHandler: Runnable?,
            negativeHandler: Runnable?,
            dismissWithoutSelectionHandler: Runnable? = null,
            drawableResId: Int? = null,
            neutral: String? = null,
            neutralHandler: Runnable? = null,
            dismissOnTouchOutside: Boolean = true,
            cancellable: Boolean = true,
            cancelHandler: Runnable? = null
        ) {

            Builder(activity).apply {
                title(title)
                message?.let { message(SpannableString(it)) }
                solidButton(SpannableString(positive), positiveHandler)
                neutral?.let { outlineButton(SpannableString(it), neutralHandler) }
                negative?.let { plain1Button(SpannableString(it), negativeHandler) }
                dismissOnTouchOutside(dismissOnTouchOutside)
                cancellable(cancellable)
                dismissWithoutSelectionHandler?.let { dismissWithoutSelectionHandler(it) }
                cancelHandler?.let { cancelHandler(it) }
                drawableResId?.let { drawableResId(it) }
                show(activity.supportFragmentManager)
            }
        }
    }

    class AlertConfig(
        val title: SpannableString? = null,
        val message: SpannableString? = null,
        val solidButton: AlertButton? = null,
        val outlineButton: AlertButton? = null,
        val plain1Button: AlertButton? = null,
        val plain2Button: AlertButton? = null,
        val dismissOnTouchOutside: Boolean = true,
        val cancellable: Boolean = true,
        val dismissWithoutSelectionHandler: Runnable? = null,
        val cancelHandler: Runnable? = null,
        val dismissHandler: Runnable? = null,
        @DrawableRes val drawableResId: Int? = null
    )

    class AlertButton(
        val title: SpannableString,
        val handler: Runnable? = null
    )

    @Suppress("unused")
    class Builder(private val context: Context) {
        private var title: SpannableString? = null
        private var message: SpannableString? = null
        private var solidButton: AlertButton? = null
        private var outlineButton: AlertButton? = null
        private var plain1Button: AlertButton? = null
        private var plain2Button: AlertButton? = null
        private var dismissOnTouchOutside: Boolean = true
        private var cancellable: Boolean = true
        private var dismissWithoutSelectionHandler: Runnable? = null
        private var cancelHandler: Runnable? = null
        private var dismissHandler: Runnable? = null
        @DrawableRes private var drawableResId: Int? = null

        fun title(title: SpannableString) = apply { this.title = title }
        fun title(title: String) = apply { this.title = SpannableString(title) }
        fun title(@StringRes titleRes: Int) = apply { this.title = SpannableString(context.getString(titleRes)) }
        fun message(message: SpannableString) = apply { this.message = message }
        fun message(message: String) = apply { this.message = SpannableString(message) }
        fun message(@StringRes messageRes: Int) = apply { this.message = SpannableString(context.getString(messageRes)) }
        fun solidButton(title: SpannableString, handler: Runnable? = null) = apply { this.solidButton = AlertButton(title, handler) }
        fun solidButton(title: String, handler: Runnable? = null) = apply { this.solidButton = AlertButton(SpannableString(title), handler) }
        fun solidButton(@StringRes titleRes: Int, handler: Runnable? = null) = apply { this.solidButton = AlertButton(SpannableString(context.getString(titleRes)), handler) }
        fun outlineButton(title: SpannableString, handler: Runnable? = null) = apply { this.outlineButton = AlertButton(title, handler) }
        fun plain1Button(title: SpannableString, handler: Runnable? = null) = apply { this.plain1Button = AlertButton(title, handler) }
        fun plain1Button(title: String, handler: Runnable? = null) = apply { this.plain1Button = AlertButton(SpannableString(title), handler) }
        fun plain1Button(@StringRes titleRes: Int, handler: Runnable? = null) = apply { this.plain1Button = AlertButton(SpannableString(context.getString(titleRes)), handler) }
        fun plain2Button(title: SpannableString, handler: Runnable? = null) = apply { this.plain2Button = AlertButton(title, handler) }
        fun dismissOnTouchOutside(dismissOnTouchOutside: Boolean) = apply { this.dismissOnTouchOutside = dismissOnTouchOutside }
        fun cancellable(cancellable: Boolean) = apply { this.cancellable = cancellable }
        fun dismissWithoutSelectionHandler(handler: Runnable) = apply { this.dismissWithoutSelectionHandler = handler }
        fun cancelHandler(handler: Runnable) = apply { this.cancelHandler = handler }
        fun dismissHandler(handler: Runnable) = apply { this.dismissHandler = handler }
        fun drawableResId(@DrawableRes resId: Int) = apply { this.drawableResId = resId }

        // TODO Use arguments
        private fun build() = AMAlertFragment().apply {
            config = AlertConfig(
                title = title,
                message = message,
                solidButton = solidButton,
                outlineButton = outlineButton,
                plain1Button = plain1Button,
                plain2Button = plain2Button,
                dismissOnTouchOutside = dismissOnTouchOutside,
                cancellable = cancellable,
                dismissWithoutSelectionHandler = dismissWithoutSelectionHandler,
                cancelHandler = cancelHandler,
                drawableResId = drawableResId,
                dismissHandler = dismissHandler
            )
        }

        fun show(fragmentManager: FragmentManager) {
            try {
                fragmentManager
                    .beginTransaction()
                    .add(build(), TAG)
                    .commitAllowingStateLoss()
            } catch (e: IllegalStateException) {
                Timber.w(e)
            }
        }
    }
}
