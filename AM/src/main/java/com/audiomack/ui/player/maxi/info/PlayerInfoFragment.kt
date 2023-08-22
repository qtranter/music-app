package com.audiomack.ui.player.maxi.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.data.sizes.SizesRepository
import com.audiomack.ui.home.HomeActivity
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.openUrlInAudiomack
import com.audiomack.utils.spannableString
import kotlinx.android.synthetic.main.fragment_player_info.layoutAddedOn
import kotlinx.android.synthetic.main.fragment_player_info.layoutAlbum
import kotlinx.android.synthetic.main.fragment_player_info.layoutDescription
import kotlinx.android.synthetic.main.fragment_player_info.layoutGenre
import kotlinx.android.synthetic.main.fragment_player_info.layoutProducer
import kotlinx.android.synthetic.main.fragment_player_info.layoutRankings
import kotlinx.android.synthetic.main.fragment_player_info.layoutTotalPlays
import kotlinx.android.synthetic.main.fragment_player_info.tvAddedOn
import kotlinx.android.synthetic.main.fragment_player_info.tvAlbum
import kotlinx.android.synthetic.main.fragment_player_info.tvAllTime
import kotlinx.android.synthetic.main.fragment_player_info.tvDescription
import kotlinx.android.synthetic.main.fragment_player_info.tvDescriptionReadMore
import kotlinx.android.synthetic.main.fragment_player_info.tvGenre
import kotlinx.android.synthetic.main.fragment_player_info.tvMonth
import kotlinx.android.synthetic.main.fragment_player_info.tvPlays
import kotlinx.android.synthetic.main.fragment_player_info.tvProducer
import kotlinx.android.synthetic.main.fragment_player_info.tvToday
import kotlinx.android.synthetic.main.fragment_player_info.tvWeek

class PlayerInfoFragment : Fragment() {

    private lateinit var viewModel: PlayerInfoViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_player_info, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = (requireActivity() as HomeActivity).playerInfoViewModel

        view?.doOnLayout {
            val lp = it.layoutParams
            lp.height = SizesRepository.screenHeight -
                (64 * it.resources.displayMetrics.density).toInt() // tabs height
            it.layoutParams = lp
        }

        initClickListeners()
        initViewModelObservers()
    }

    private fun initClickListeners() {
        tvDescriptionReadMore.setOnClickListener { viewModel.onDescriptionReadMoreTapped() }
        tvToday.setOnClickListener { viewModel.onTodayTapped() }
        tvWeek.setOnClickListener { viewModel.onWeekTapped() }
        tvMonth.setOnClickListener { viewModel.onMonthTapped() }
        tvAllTime.setOnClickListener { viewModel.onAllTimeTapped() }
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            totalPlays.observe(viewLifecycleOwner, totalPlaysObserver)
            album.observe(viewLifecycleOwner, albumObserver)
            producer.observe(viewLifecycleOwner, producerObserver)
            addedOn.observe(viewLifecycleOwner, addedOnObserver)
            genre.observe(viewLifecycleOwner, genreObserver)
            description.observe(viewLifecycleOwner, descriptionObserver)
            descriptionExpanded.observe(viewLifecycleOwner, descriptionExpandedObserver)
            rankVisible.observe(viewLifecycleOwner, rankVisibleObserver)
            rankToday.observe(viewLifecycleOwner, rankTodayObserver)
            rankWeek.observe(viewLifecycleOwner, rankWeekObserver)
            rankMonth.observe(viewLifecycleOwner, rankMonthObserver)
            rankAllTime.observe(viewLifecycleOwner, rankAllTimeObserver)
            openInternalURLEvent.observe(viewLifecycleOwner, openURLObserver)
            closePlayer.observe(viewLifecycleOwner, closePlayerObserver)
        }
    }

    private val totalPlaysObserver = Observer<String> { totalPlays ->
        tvPlays.text = totalPlays
        layoutTotalPlays.visibility = if (totalPlays.isBlank()) View.GONE else View.VISIBLE
    }

    private val albumObserver = Observer<String> { album ->
        tvAlbum.text = album
        layoutAlbum.visibility = if (album.isBlank()) View.GONE else View.VISIBLE
    }

    private val producerObserver = Observer<String> { producer ->
        tvProducer.text = producer
        layoutProducer.visibility = if (producer.isBlank()) View.GONE else View.VISIBLE
    }

    private val addedOnObserver = Observer<String> { addedOn ->
        tvAddedOn.text = addedOn
        layoutAddedOn.visibility = if (addedOn.isBlank()) View.GONE else View.VISIBLE
    }

    private val genreObserver = Observer<String> { genre ->
        tvGenre.text = genre
        layoutGenre.visibility = if (genre.isBlank()) View.GONE else View.VISIBLE
    }

    private val descriptionObserver = Observer<String> { description ->
        tvDescription.text = description
        layoutDescription.visibility = if (description.isBlank()) View.GONE else View.VISIBLE
    }

    private val descriptionExpandedObserver = Observer<Boolean> { descriptionExpanded ->
        tvDescriptionReadMore.visibility = if (descriptionExpanded) View.GONE else View.VISIBLE
        tvDescription.maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2
    }

    private val rankVisibleObserver = Observer<Boolean> { visible ->
        layoutRankings.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private val rankTodayObserver = Observer<String> { rankToday ->
        tvToday.text = formatStatNumber(rankToday)
    }

    private val rankWeekObserver = Observer<String> { rankWeek ->
        tvWeek.text = formatStatNumber(rankWeek)
    }

    private val rankMonthObserver = Observer<String> { rankYear ->
        tvMonth.text = formatStatNumber(rankYear)
    }

    private val rankAllTimeObserver = Observer<String> { rankAllTime ->
        tvAllTime.text = formatStatNumber(rankAllTime)
    }

    private val openURLObserver = Observer<String> { url ->
        context?.openUrlInAudiomack(url)
    }

    private val closePlayerObserver = Observer<Void> {
        (activity as? HomeActivity)?.playerViewModel?.onMinimizeClick()
    }

    private fun formatStatNumber(value: String): CharSequence {
        val context = context ?: return ""
        return if (value.isBlank()) {
            "-"
        } else {
            context.spannableString(
                fullString = value,
                highlightedStrings = listOf("#"),
                highlightedColor = context.colorCompat(R.color.orange),
                highlightedFont = R.font.opensans_semibold
            )
        }
    }

    companion object {
        fun newInstance() = PlayerInfoFragment()
    }
}
