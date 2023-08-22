package com.audiomack.ui.comments.view

import android.os.Bundle
import android.text.SpannableString
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.audiomack.MainApplication
import com.audiomack.R
import com.audiomack.data.sizes.SizesRepository
import com.audiomack.fragments.TrackedFragment
import com.audiomack.model.AMComment
import com.audiomack.model.AMExpandComment
import com.audiomack.model.AMResultItem
import com.audiomack.model.AMShowMoreComments
import com.audiomack.model.Action
import com.audiomack.model.CommentSort
import com.audiomack.model.Credentials
import com.audiomack.model.EventCommentAdded
import com.audiomack.model.LoginSignupSource
import com.audiomack.model.MixpanelSource
import com.audiomack.ui.alert.AMAlertFragment
import com.audiomack.ui.authentication.AuthenticationActivity
import com.audiomack.ui.comments.add.AddCommentFragment
import com.audiomack.ui.home.HomeActivity
import com.audiomack.ui.settings.OptionsMenuFragment
import com.audiomack.usecases.LoginAlertUseCase
import com.audiomack.utils.convertDpToPixel
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.views.AMSnackbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_comments.*
import kotlinx.android.synthetic.main.view_placeholder.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class CommentsFragment : TrackedFragment(R.layout.fragment_comments, TAG) {

    enum class Mode {
        Standalone, Player, Single
    }

    private lateinit var viewModel: CommentsViewModel

    private lateinit var mode: Mode
    private var entity: AMResultItem? = null
    private var comment: AMComment? = null
    private lateinit var commentsAdapter: CommentsAdapter
    private var comments: ArrayList<AMComment>? = null

    private var isPaginating: Boolean = false
    private var scrollToTop: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, CommentViewModelFactory(mode, mixpanelSource))
            .get(CommentsViewModel::class.java)

        initClickListeners()
        initViewModelObservers()

        entity?.let { notNullEntity -> viewModel.updateEntity(notNullEntity) }
        comment?.let { notNullComment -> viewModel.updateSingleComment(notNullComment) }
        comments?.let { notNullComments -> viewModel.updateCommentList(notNullComments) }

        commentsAdapter = CommentsAdapter(viewModel)
        recyclerView.adapter = commentsAdapter

        swipeRefreshLayout.setColorSchemeColors(swipeRefreshLayout.context.colorCompat(R.color.orange))
        swipeRefreshLayout.setOnRefreshListener { viewModel.onRefreshTriggered() }

        val padding = if (viewModel.adsVisible) {
            (resources.getDimension(R.dimen.minified_player_height) + resources.getDimension(R.dimen.ad_height)).toInt() + if (mode == Mode.Player) resources.getDimension(R.dimen.minified_player_height).toInt() else 0
        } else {
            resources.getDimension(R.dimen.minified_player_height).toInt() + if (mode == Mode.Player) resources.getDimension(R.dimen.minified_player_height).toInt() else 0
        }
        recyclerView.setPadding(recyclerView.paddingLeft, recyclerView.paddingTop, recyclerView.paddingRight, padding)
        val horizontalPadding = if (mode == Mode.Player)
            context?.convertDpToPixel(10F) ?: 0
            else view.paddingLeft
        view.setPadding(horizontalPadding, view.paddingTop, horizontalPadding, view.paddingBottom)

        isPaginating = true

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val itemCount = recyclerView.layoutManager!!.itemCount
                if ((viewModel.commentCount.value ?: 0) > itemCount) {
                    val lastVisibleItemPosition: Int = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    if (!viewModel.isEndOfComments && !isPaginating && (itemCount == lastVisibleItemPosition + 1)) {
                        isPaginating = true
                        commentsAdapter.showLoading()
                        viewModel.onLoadMore(itemCount)
                    }
                }
            }
        })

        view.setBackgroundColor(view.context.colorCompat(if (mode == Mode.Standalone) R.color.background_color else R.color.black))

        placeholderNoConnection.findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.ic_empty_offline)
        placeholderNoConnection.findViewById<TextView>(R.id.tvMessage).text = getString(R.string.noconnection_placeholder)
        placeholderNoConnection.findViewById<Button>(R.id.cta).text = getString(R.string.noconnection_highlighted_placeholder)

        placeholderNoComments.findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.ic_comments_placeholder)
        placeholderNoComments.findViewById<TextView>(R.id.tvMessage).visibility = View.GONE
        placeholderNoComments.findViewById<Button>(R.id.cta).setText(R.string.comments_placeholder_cta)

        viewModel.onCreate()
    }

    private fun initClickListeners() {
        buttonBack.setOnClickListener { viewModel.onCloseTapped() }
        buttonSort.setOnClickListener { viewModel.onSortButtonTapped() }
        buttonSortBis.setOnClickListener { viewModel.onSortButtonTapped() }
        linWriteComment.setOnClickListener { viewModel.onWriteCommentTapped() }
        linWriteCommentBis.setOnClickListener { viewModel.onWriteCommentTapped() }
        viewTitle.setOnClickListener { viewModel.onTitleClicked() }
        // No connection placeholder
        tvMessage.setOnClickListener { viewModel.onRefreshTriggered() }
        cta.setOnClickListener { viewModel.onRefreshTriggered() }
        imageView.setOnClickListener { viewModel.onRefreshTriggered() }
        placeholderNoComments.findViewById<Button>(R.id.cta).setOnClickListener { viewModel.onWriteCommentTapped() }
        buttonViewAll.setOnClickListener { viewModel.onViewAllTapped() }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            updateCommentListEvent.observe(viewLifecycleOwner, updateCommentListObserver)
            showLoadingEvent.observe(viewLifecycleOwner, showLoadingObserver)
            hideLoadingEvent.observe(viewLifecycleOwner, hideLoadingObserver)
            showErrorToastEvent.observe(viewLifecycleOwner, showErrorToastObserver)
            closeEvent.observe(viewLifecycleOwner, closeObserver)
            closeOptionsEvent.observe(viewLifecycleOwner, closeOptionsObserver)
            showCommenterEvent.observe(viewLifecycleOwner, showCommenterObserver)
            showAddCommentEvent.observe(viewLifecycleOwner, showAddCommentObserver)
            showAddReplyEvent.observe(viewLifecycleOwner, showAddReplyObserver)
            showDeleteAlertViewEvent.observe(viewLifecycleOwner, showDeleteAlertViewObserver)
            showReportAlertViewEvent.observe(viewLifecycleOwner, showReportAlertViewObserver)
            showSortViewEvent.observe(viewLifecycleOwner, showSortViewObserver)
            showMoreCommentsEvent.observe(viewLifecycleOwner, showMoreCommentsObserver)
            showOptionsEvent.observe(viewLifecycleOwner, showOptionsObserver)
            showLoginAlertEvent.observe(viewLifecycleOwner, showLoginAlertObserver)
            showLoggedInEvent.observe(viewLifecycleOwner, showLoggedInObserver)
            showLoadErrorToastEvent.observe(viewLifecycleOwner, showLoadErrorToastObserver)
            showConnectionErrorToastEvent.observe(viewLifecycleOwner, showConnectionErrorToastObserver)
            stopInfiniteScrollEvent.observe(viewLifecycleOwner, stopInfiniteScrollObserver)
            expandCommentEvent.observe(viewLifecycleOwner, expandCommentObserver)
            commentCount.observe(viewLifecycleOwner, updateCommentCountObserver)
            avatar.observe(viewLifecycleOwner, avatarObserver)
            standaloneHeaderVisible.observe(viewLifecycleOwner, standaloneHeaderVisibleObserver)
            playerHeaderVisible.observe(viewLifecycleOwner, playerHeaderVisibleObserver)
            noDataPlaceholderVisible.observe(viewLifecycleOwner, noDataPlaceholderVisibleObserver)
            noConnectionPlaceholderVisible.observe(viewLifecycleOwner, noConnectionPlaceholderVisibleObserver)
            scrollViewNestedScrollEnabled.observe(viewLifecycleOwner, scrollViewNestedScrollEnabledObserver)
            singleCommentModeVisible.observe(viewLifecycleOwner, singleCommentModeVisibleObserver)
            updateTitleEvent.observe(viewLifecycleOwner, updateTitleObserver)
            showViewAllEvent.observe(viewLifecycleOwner, showViewAllObserver)
        }
    }

    private val showViewAllObserver: Observer<Void> = Observer {
        (activity as? HomeActivity)?.openComments(entity, null, null)
    }

    private val updateTitleObserver: Observer<Pair<String?, String?>> = Observer {
        tvCommentTitle.text = it.first
        tvCommentSubtitle.text = it.second
    }

    private val updateCommentListObserver: Observer<Pair<ArrayList<AMComment>, String?>> = Observer { (comments, uploaderSlug) ->
        updateCommentList(comments, uploaderSlug)
        swipeRefreshLayout.isRefreshing = false
        isPaginating = false
        if (scrollToTop) {
            recyclerView.smoothScrollToPosition(0)
            scrollToTop = false
        }
    }

    private val updateCommentCountObserver: Observer<Int> = Observer { count ->
        val string = when (mode) {
            Mode.Standalone -> {
                String.format("%d %s", count, if (count == 1) {
                    getString(R.string.comments_comment_title)
                } else {
                    getString(R.string.comments_header_title)
                })
            }
            Mode.Player -> {
                getString(if (count == 1) R.string.player_extra_comments_count_singular
                else R.string.player_extra_comments_count_plural, count)
            }
            Mode.Single -> ""
        }
        tvCommentCount.text = string
        tvCommentTitleBis.text = string
    }

    private val showLoadingObserver: Observer<Void> = Observer {
        if (commentsAdapter.itemCount <= 1) {
            animationView.show()
        }
    }

    private val hideLoadingObserver: Observer<Void> = Observer {
        animationView.hide()
    }

    private val showErrorToastObserver: Observer<Void> = Observer {
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.generic_error_occurred))
            .withSubtitle(getString(R.string.please_try_again_later))
            .withDrawable(R.drawable.ic_snackbar_error)
            .withDuration(Snackbar.LENGTH_SHORT)
            .show()
    }

    private val closeObserver: Observer<Void> = Observer {
        (activity as? HomeActivity)?.popFragment()
    }

    private val closeOptionsObserver: Observer<Void> = Observer {
        (activity as? HomeActivity)?.popFragment()
    }

    private val showCommenterObserver: Observer<String> = Observer { artist ->
        (activity as? HomeActivity)?.homeViewModel?.onArtistScreenRequested(artist)
    }

    private val showAddCommentObserver: Observer<AMResultItem> = Observer { music ->
        val commentAddFragment = AddCommentFragment.newInstance(music, null)
        (activity as? HomeActivity)?.openOptionsFragment(commentAddFragment)
    }

    private val showAddReplyObserver: Observer<Pair<AMResultItem, String>> = Observer { (music, threadId) ->
        val commentAddFragment = AddCommentFragment.newInstance(music, threadId)
        (activity as? HomeActivity)?.openOptionsFragment(commentAddFragment)
    }

    private val showDeleteAlertViewObserver: Observer<AMComment> = Observer { comment ->
        activity?.let {
            AMAlertFragment.show(it,
                SpannableString(getString(R.string.comments_delete_alert_title)),
                null, getString(R.string.comments_delete_alert_confirm),
                getString(R.string.comments_alert_cancel),
                Runnable { viewModel.onCommentDeleteTapped(comment) },
                Runnable {},
                Runnable {}
            )
        }
    }

    private val showReportAlertViewObserver: Observer<AMComment> = Observer { comment ->
        activity?.let {
            AMAlertFragment.show(it,
                SpannableString(getString(R.string.comments_flag_alert_title)),
                getString(R.string.comments_flag_alert_subtitle),
                getString(R.string.comments_flag_alert_confirm),
                getString(R.string.comments_alert_cancel),
                Runnable { viewModel.onCommentReportTapped(comment) },
                Runnable {},
                Runnable {}
            )
        }
    }

    private val avatarObserver: Observer<String?> = Observer {
        if (it.isNullOrBlank()) {
            imageViewUserProfile.setImageResource(R.drawable.profile_placeholder)
            imageViewUserProfileBis.setImageResource(R.drawable.profile_placeholder)
        } else {
            viewModel.imageLoader.load(imageViewUserProfile.context, it, imageViewUserProfile)
            viewModel.imageLoader.load(imageViewUserProfile.context, it, imageViewUserProfileBis)
        }
    }

    private val showSortViewObserver: Observer<CommentSort> = Observer { sort ->
        val actions = listOf(
            Action(getString(R.string.comments_filter_top), sort == CommentSort.Top, R.drawable.menu_top_comments, object : Action.ActionListener {
                override fun onActionExecuted() {
                    try {
                        scrollToTop = true
                        viewModel.onChangedSorting(CommentSort.Top)
                    } catch (e: Exception) {
                        Timber.w(e)
                    }
                }
            }),
            Action(getString(R.string.comments_filter_newest), sort == CommentSort.Newest, R.drawable.menu_newest_first, object : Action.ActionListener {
                override fun onActionExecuted() {
                    try {
                        scrollToTop = true
                        viewModel.onChangedSorting(CommentSort.Newest)
                    } catch (e: Exception) {
                        Timber.w(e)
                    }
                }
            }),
            Action(getString(R.string.comments_filter_oldest), sort == CommentSort.Oldest, R.drawable.menu_oldest_first, object : Action.ActionListener {
                override fun onActionExecuted() {
                    try {
                        scrollToTop = true
                        viewModel.onChangedSorting(CommentSort.Oldest)
                    } catch (e: Exception) {
                        Timber.w(e)
                    }
                }
            })
        )
        (activity as HomeActivity).openOptionsFragment(OptionsMenuFragment.newInstance(actions))
    }

    private val showMoreCommentsObserver: Observer<AMShowMoreComments> = Observer { showMore ->
        showMore.textView.visibility = View.GONE
        val commentsAdapter = ReplyCommentsAdapter(
            showMore.comment,
            showMore.uploaderSlug ?: "",
            showMore.listener
        )
        showMore.recyclerView.visibility = View.VISIBLE
        showMore.recyclerView.setHasFixedSize(true)
        showMore.recyclerView.adapter = commentsAdapter
        commentsAdapter.update(showMore.comment.children.subList(1, showMore.comment.children.size))
    }

    private val showOptionsObserver: Observer<AMComment> = Observer {
        showMoreOptions(it)
    }

    private val showLoginAlertObserver: Observer<Void> = Observer {
        activity?.let {
            AMAlertFragment.show(
                it,
                SpannableString(LoginAlertUseCase().getMessage(it)),
                null,
                getString(R.string.login_needed_yes),
                getString(R.string.login_needed_no),
                Runnable { viewModel.onStartLoginTapped() },
                Runnable { viewModel.onCancelLoginTapped() },
                Runnable { viewModel.onCancelLoginTapped() }
            )
        }
    }

    private val showLoggedInObserver: Observer<LoginSignupSource> = Observer { source ->
        AuthenticationActivity.show(activity, source)
    }

    private val showLoadErrorToastObserver: Observer<Void> = Observer {
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.generic_error_occurred))
            .withSubtitle(getString(R.string.comments_try_load_later))
            .withDrawable(R.drawable.ic_snackbar_error)
            .withSecondary(R.drawable.ic_snackbar_comment_grey)
            .show()
    }

    private val showConnectionErrorToastObserver: Observer<Void> = Observer {
        AMSnackbar.Builder(activity)
            .withTitle(getString(R.string.noconnection_placeholder))
            .withSubtitle(getString(R.string.comments_try_later_connection))
            .withDrawable(R.drawable.ic_snackbar_error)
            .withSecondary(R.drawable.ic_snackbar_comment_grey)
            .show()
    }

    private val stopInfiniteScrollObserver: Observer<Void> = Observer {
        commentsAdapter.hideLoading()
    }

    private val expandCommentObserver: Observer<AMExpandComment> = Observer { expand ->
        expand.comment.expanded = !expand.comment.expanded
        if (expand.comment.expanded) {
            expand.tvMessage.maxLines = Int.MAX_VALUE
            expand.tvExpand.text = expand.tvExpand.context.getString(R.string.comments_minimize)
        } else {
            expand.tvMessage.maxLines = 5
            expand.tvExpand.text = expand.tvExpand.context.getString(R.string.comments_expand)
        }
    }

    private val standaloneHeaderVisibleObserver: Observer<Boolean> = Observer {
        standaloneHeader.visibility = if (it) View.VISIBLE else View.GONE
    }

    private val playerHeaderVisibleObserver: Observer<Boolean> = Observer { visible ->
        playerHeader.visibility = if (visible) View.VISIBLE else View.GONE

        if (mode == Mode.Player) {
            mainContainer.doOnNextLayout {
                val lp = it.layoutParams
                val height = SizesRepository.screenHeight -
                    it.top -
                    (64 * it.resources.displayMetrics.density).toInt() // tabs height
                lp.height = height
                it.layoutParams = lp
            }
        }
    }

    private val noDataPlaceholderVisibleObserver: Observer<Boolean> = Observer {
        placeholderNoComments.visibility = if (it) View.VISIBLE else View.GONE
    }

    private val noConnectionPlaceholderVisibleObserver: Observer<Boolean> = Observer {
        placeholderNoConnection.visibility = if (it) View.VISIBLE else View.GONE
    }

    private val singleCommentModeVisibleObserver: Observer<Boolean> = Observer {
        viewTitle.visibility = if (it) View.VISIBLE else View.GONE
        buttonViewAll.visibility = if (it) View.VISIBLE else View.GONE
        buttonSort.visibility = if (it) View.GONE else View.VISIBLE
        tvCommentCount.visibility = if (it) View.GONE else View.VISIBLE
        tvCommentTitleBis.visibility = if (it) View.GONE else View.VISIBLE
        linWriteComment.visibility = if (it) View.GONE else View.VISIBLE
        viewBorder.visibility = if (it) View.GONE else View.VISIBLE
    }

    private val scrollViewNestedScrollEnabledObserver: Observer<Boolean> = Observer { enabled ->
        recyclerView.isNestedScrollingEnabled = enabled
    }

    fun isDisplayingSameData(newEntity: AMResultItem?, newComments: ArrayList<AMComment>?): Boolean {

        newEntity?.let { notNullNewEntity ->
            entity?.let { notNullEntity ->
                if (notNullEntity.itemId == notNullNewEntity.itemId) {
                    return true
                }
            }
        }

        newComments?.let { notNullNewComments ->
            if (comments == notNullNewComments) {
                return true
            }
        }

        return false
    }

    override fun onResume() {
        super.onResume()
        (activity as? HomeActivity)?.closeTooltipFragment()
    }

    private fun updateCommentList(comments: ArrayList<AMComment>, uploaderSlug: String?) {
        commentsAdapter.uploaderSlug = uploaderSlug ?: ""
        if (comments.isNullOrEmpty()) {
            recyclerView.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            commentsAdapter.update(comments)
        }
    }

    private fun showMoreOptions(commentsItem: AMComment) {
        val action = if (Credentials.itsMe(MainApplication.context, commentsItem.userId.toString())) {
            Action(getString(R.string.comments_delete_comment), false, R.drawable.ic_options_delete_comment, object : Action.ActionListener {
                override fun onActionExecuted() {
                    viewModel.showDeleteAlertView(commentsItem)
                }
            })
        } else {
            Action(getString(R.string.comments_flag_comment), false, R.drawable.ic_options_flag_comment, object : Action.ActionListener {
                override fun onActionExecuted() {
                    viewModel.showReportAlertView(commentsItem)
                }
            })
        }
        val shareAction = Action(getString(R.string.comments_share_comment), false, R.drawable.ic_options_share_comment, object : Action.ActionListener {
            override fun onActionExecuted() {
                viewModel.onShareCommentTapped(activity, commentsItem)
            }
        })
        (activity as? HomeActivity)?.openOptionsFragment(OptionsMenuFragment.newInstance(listOf(action, shareAction)))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(commentAdded: EventCommentAdded) {
        viewModel.updateCommentListWithComment(commentAdded.comment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        mode = arguments?.getSerializable(ARGS_MODE) as? Mode ?: Mode.Standalone
    }

    val mixpanelSource: MixpanelSource
        get() = entity?.mixpanelSource ?: MixpanelSource.empty

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CommentsFragment"
        private const val ARGS_MODE = "mode"
        fun newInstance(mode: Mode, entity: AMResultItem? = null, comment: AMComment? = null, comments: ArrayList<AMComment>? = null) =
            CommentsFragment().apply {
                this.arguments = bundleOf(ARGS_MODE to mode)
                this.comments = comments
                this.entity = entity
                this.comment = comment
            }
    }
}
