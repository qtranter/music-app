package com.audiomack.ui.comments.add

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.audiomack.R
import com.audiomack.model.AMResultItem
import com.audiomack.ui.alert.AMCommentIntroAlertFragment
import com.audiomack.utils.spannableString
import com.audiomack.views.AMProgressHUD
import com.audiomack.views.AMSnackbar
import kotlinx.android.synthetic.main.fragment_comment_add.buttonSend
import kotlinx.android.synthetic.main.fragment_comment_add.edtComment
import kotlinx.android.synthetic.main.fragment_comment_add.imageViewUserProfile
import kotlinx.android.synthetic.main.fragment_comment_add.parentLayout
import kotlinx.android.synthetic.main.fragment_comment_add.tvCommentingOn

class AddCommentFragment : Fragment() {

    private lateinit var viewModel: AddCommentViewModel
    private var entity: AMResultItem? = null
    private var threadId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_comment_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, AddCommentViewModelFactory(entity, threadId)).get(AddCommentViewModel::class.java)

        initClickListeners()
        initViewModelObservers()
    }

    private fun initClickListeners() {
        buttonSend.setOnClickListener { viewModel.onSendTapped() }
        parentLayout.setOnClickListener { viewModel.onBackgroundTapped() }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            close.observe(viewLifecycleOwner, closeObserver)
            buttonSendEvent.observe(viewLifecycleOwner, sendObserver)
            showKeyboardEvent.observe(viewLifecycleOwner, showKeyboardObserver)
            hideKeyboardEvent.observe(viewLifecycleOwner, hideKeyboardObserver)
            showLoadingEvent.observe(viewLifecycleOwner, showLoaderObserver)
            hideLoadingEvent.observe(viewLifecycleOwner, hideLoaderObserver)
            showErrorMessageEvent.observe(viewLifecycleOwner, showErrorMessageObserver)
            showCommentIntroEvent.observe(viewLifecycleOwner, showCommentIntroObserver)
            avatar.observe(viewLifecycleOwner, updateAvatarObserver)
            songName.observe(viewLifecycleOwner, songNameObserver)
        }
    }

    private val closeObserver: Observer<Void> = Observer {
        activity?.onBackPressed()
    }

    private val sendObserver: Observer<Void> = Observer {
        viewModel.buttonSendTapped(edtComment.text.toString())
    }

    private val showKeyboardObserver: Observer<Void> = Observer {
        edtComment.postDelayed({
            edtComment?.also {
                it.requestFocus()
                (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(it, 0)
            }
        }, 50)
    }

    private val hideKeyboardObserver = Observer<Void> {
        (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(edtComment.windowToken, 0)
    }

    private val showLoaderObserver = Observer<Void> {
        AMProgressHUD.showWithStatus(activity, null)
    }

    private val hideLoaderObserver = Observer<Void> {
        AMProgressHUD.dismiss()
    }

    private val showErrorMessageObserver = Observer<Void> {
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.generic_error_occurred))
            .withSubtitle(getString(R.string.comments_try_add_later))
            .withDrawable(R.drawable.ic_snackbar_error)
            .withSecondary(R.drawable.ic_snackbar_comment_grey)
            .show()
    }

    private val showCommentIntroObserver = Observer<Void> {
        activity?.let { AMCommentIntroAlertFragment.show(it) }
    }

    private val updateAvatarObserver = Observer<String?> {
        if (it.isNullOrBlank()) {
            imageViewUserProfile.setImageResource(R.drawable.profile_placeholder)
        } else {
            viewModel.imageLoader.load(imageViewUserProfile.context, it, imageViewUserProfile)
        }
    }

    private val songNameObserver = Observer<String> {
        tvCommentingOn.text = tvCommentingOn.context.spannableString(
            fullString = getString(R.string.comments_commenting_on, it),
            highlightedStrings = listOf(it),
            highlightedColor = Color.WHITE,
            highlightedFont = R.font.opensans_bold
        )
    }

    companion object {
        @JvmStatic
        fun newInstance(item: AMResultItem, threadId: String?): AddCommentFragment {
            return AddCommentFragment().apply {
                this.entity = item
                this.threadId = threadId
            }
        }
    }
}
