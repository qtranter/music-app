package com.audiomack.ui.search.filters

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.utils.extensions.drawableCompat
import kotlinx.android.synthetic.main.activity_search_filters.*

class SearchFiltersActivity : androidx.fragment.app.FragmentActivity() {

    private val viewModel: SearchFiltersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_filters)

        viewModel.close.observe(this, Observer {
            finish()
        })
        viewModel.resetSortControls.observe(this, Observer {
            listOf(tvMostRelevant, tvMostPopular, tvMostRecent).forEach { it.isSelected = false }
        })
        viewModel.mostRelevant.observe(this, Observer {
            tvMostRelevant.isSelected = true
        })
        viewModel.mostPopular.observe(this, Observer {
            tvMostPopular.isSelected = true
        })
        viewModel.mostRecent.observe(this, Observer {
            tvMostRecent.isSelected = true
        })
        viewModel.updateSortControls.observe(this, Observer {
            listOf(tvMostRelevant, tvMostPopular, tvMostRecent).forEach {
                it.setCompoundDrawablesWithIntrinsicBounds(it.context.drawableCompat(if (it.isSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
                    null,
                    null,
                    null)
            }
        })
        viewModel.updateVerifiedOnly.observe(this, Observer {
            it?.let { checked ->
                switchVerified.isChecked = checked
            }
        })
        viewModel.resetGenreControls.observe(this, Observer {
            listOf(tvAllGenres, tvHipHopRap, tvRnb, tvElectronic, tvReggae, tvRock, tvPop,
                tvAfrobeats, tvPodcast, tvLatin, tvInstrumental).forEach { it.isSelected = false }
        })
        viewModel.allGenres.observe(this, Observer {
            tvAllGenres.isSelected = true
        })
        viewModel.rap.observe(this, Observer {
            tvHipHopRap.isSelected = true
        })
        viewModel.rnb.observe(this, Observer {
            tvRnb.isSelected = true
        })
        viewModel.electronic.observe(this, Observer {
            tvElectronic.isSelected = true
        })
        viewModel.reggae.observe(this, Observer {
            tvReggae.isSelected = true
        })
        viewModel.rock.observe(this, Observer {
            tvRock.isSelected = true
        })
        viewModel.pop.observe(this, Observer {
            tvPop.isSelected = true
        })
        viewModel.afrobeats.observe(this, Observer {
            tvAfrobeats.isSelected = true
        })
        viewModel.podcast.observe(this, Observer {
            tvPodcast.isSelected = true
        })
        viewModel.latin.observe(this, Observer {
            tvLatin.isSelected = true
        })
        viewModel.instrumental.observe(this, Observer {
            tvInstrumental.isSelected = true
        })
        viewModel.updateGenreControls.observe(this, Observer {
            listOf(tvAllGenres, tvHipHopRap, tvRnb, tvElectronic, tvReggae, tvRock, tvPop,
                tvAfrobeats, tvPodcast, tvLatin, tvInstrumental).forEach {
                it.setCompoundDrawablesWithIntrinsicBounds(
                    it.context.drawableCompat(if (it.isSelected) R.drawable.ic_check_on else R.drawable.ic_check_off),
                    null,
                    null,
                    null)
            }
        })

        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
        buttonApply.setOnClickListener { viewModel.onApplyTapped() }
        tvMostRelevant.setOnClickListener { viewModel.onMostRelevantSelected() }
        tvMostPopular.setOnClickListener { viewModel.onMostPopularSelected() }
        tvMostRecent.setOnClickListener { viewModel.onMostRecentSelected() }
        switchVerified.setOnCheckedChangeListener { _, isChecked -> viewModel.onVerifiedSwitchChanged(isChecked) }
        tvAllGenres.setOnClickListener { viewModel.onAllGenresSelected() }
        tvHipHopRap.setOnClickListener { viewModel.onRapSelected() }
        tvRnb.setOnClickListener { viewModel.onRnBSelected() }
        tvElectronic.setOnClickListener { viewModel.onElectronicSelected() }
        tvReggae.setOnClickListener { viewModel.onReggaeSelected() }
        tvRock.setOnClickListener { viewModel.onRockSelected() }
        tvPop.setOnClickListener { viewModel.onPopSelected() }
        tvAfrobeats.setOnClickListener { viewModel.onAfrobeatsSelected() }
        tvPodcast.setOnClickListener { viewModel.onPodcastSelected() }
        tvLatin.setOnClickListener { viewModel.onLatinSelected() }
        tvInstrumental.setOnClickListener { viewModel.onInstrumentalSelected() }

        viewModel.onCreate()
    }

    companion object {
        @JvmStatic
        fun show(activity: Activity?) {
            activity?.let {
                val intent = Intent(it, SearchFiltersActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(intent)
            }
        }
    }
}
