package com.audiomack.ui.alert

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.audiomack.R
import com.audiomack.model.EventCommentIntroDismissed
import kotlinx.android.synthetic.main.fragment_alert_comment_intro.*
import org.greenrobot.eventbus.EventBus
import timber.log.Timber

class AMCommentIntroAlertFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_alert_comment_intro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle.text = getString(R.string.comments_be_nice_title)
        listOf(getString(R.string.comments_be_nice_bullet_1), getString(R.string.comments_be_nice_bullet_2), getString(R.string.comments_be_nice_bullet_3)).forEach { bulletPointString ->
            val bulletPointView = LayoutInflater.from(context).inflate(R.layout.row_comment_intro, layoutBulletPoints, false)
            val tvMessage = bulletPointView.findViewById<TextView>(R.id.tvMessage)
            tvMessage.text = bulletPointString
            layoutBulletPoints.addView(bulletPointView)
        }
        tvSubtitle.text = getString(R.string.comments_be_nice_subtitle)
        buttonPositive.text = getString(R.string.comments_be_nice_continue)

        view.setOnClickListener { close() }

        buttonClose.setOnClickListener { close() }

        buttonPositive.setOnClickListener { close() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AudiomackDialogFragment)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        EventBus.getDefault().post(EventCommentIntroDismissed())
    }

    private fun close() {
        try {
            dismiss()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    companion object {
        fun show(activity: FragmentActivity) {
            val fragment = AMCommentIntroAlertFragment()
            try {
                fragment.show(activity.supportFragmentManager, fragment.javaClass.simpleName)
            } catch (e: IllegalStateException) {
                Timber.w(e)
            }
        }
    }
}
