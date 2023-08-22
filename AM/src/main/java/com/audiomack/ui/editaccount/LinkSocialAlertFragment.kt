package com.audiomack.ui.editaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.audiomack.R
import com.audiomack.model.SocialNetwork
import com.audiomack.utils.extensions.drawableCompat
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.lang.Exception
import kotlinx.android.synthetic.main.fragment_link_social.*
import timber.log.Timber

class LinkSocialAlertFragment : androidx.fragment.app.DialogFragment() {

    private lateinit var subject: BehaviorSubject<String?>
    private lateinit var socialNetwork: SocialNetwork
    private var value: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_link_social, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::socialNetwork.isInitialized) {
            close()
            return
        }

        when (socialNetwork) {
            SocialNetwork.Facebook -> {
                tvTitle.text = getString(R.string.connect_social_popup_title_facebook)
                tvTitle.setCompoundDrawablesWithIntrinsicBounds(null, tvTitle.context.drawableCompat(R.drawable.social_link_facebook_popup), null, null)
                etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                etUsername.hint = getString(R.string.connect_social_popup_placeholder_facebook)
            }
            SocialNetwork.YouTube -> {
                tvTitle.text = getString(R.string.connect_social_popup_title_youtube)
                tvTitle.setCompoundDrawablesWithIntrinsicBounds(null, tvTitle.context.drawableCompat(R.drawable.social_link_youtube_popup), null, null)
                etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                etUsername.hint = getString(R.string.connect_social_popup_placeholder_youtube)
            }
            else -> { }
        }

        etUsername.setText(value, TextView.BufferType.EDITABLE)
        etUsername.setSelection(etUsername.length())

        view.setOnClickListener { cancel() }

        buttonClose.setOnClickListener { cancel() }

        buttonSave.setOnClickListener {
            subject.onNext(etUsername.text.toString().trim())
            subject.onComplete()
            close()
        }

        buttonClear.setOnClickListener {
            subject.onNext("")
            subject.onComplete()
            close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AudiomackDialogFragment)
    }

    private fun cancel() {
        subject.onError(Exception("Value not set"))
        subject.onComplete()
        close()
    }

    private fun close() {
        try {
            dismiss()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    companion object {
        @JvmStatic
        fun show(activity: FragmentActivity, socialNetwork: SocialNetwork, value: String?): Observable<String?> {
            val subject: BehaviorSubject<String?> = BehaviorSubject.create()
            val fragment = LinkSocialAlertFragment()
            fragment.subject = subject
            fragment.socialNetwork = socialNetwork
            fragment.value = value
            try {
                fragment.show(activity.supportFragmentManager, fragment.javaClass.simpleName)
            } catch (e: IllegalStateException) {
                Timber.w(e)
            }
            return subject
        }
    }
}
