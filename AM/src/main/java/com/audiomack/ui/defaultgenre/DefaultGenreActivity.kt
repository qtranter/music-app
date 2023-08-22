package com.audiomack.ui.defaultgenre

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.data.preferences.DefaultGenre
import com.audiomack.model.GenreModel
import com.audiomack.utils.extensions.drawableCompat
import kotlinx.android.synthetic.main.activity_default_genre.*

class DefaultGenreActivity : androidx.fragment.app.FragmentActivity() {

    private val viewModel: DefaultGenreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_genre)

        viewModel.back.observe(this, Observer {
            finish()
        })

        buttonBack.setOnClickListener { viewModel.onBackTapped() }

        val defaultGenre = viewModel.defaultGenre

        val genreModels = listOf(
            GenreModel(tvAll, DefaultGenre.ALL, "all", "All", defaultGenre == DefaultGenre.ALL),
            GenreModel(tvRnb, DefaultGenre.RNB, "rnb", "R&B", defaultGenre == DefaultGenre.RNB),
            GenreModel(tvReggae, DefaultGenre.REGGAE, "dancehall", "Reggae", defaultGenre == DefaultGenre.REGGAE),
            GenreModel(tvAfrobeats, DefaultGenre.AFROBEATS, "afropop", "Afropop", defaultGenre == DefaultGenre.AFROBEATS),
            GenreModel(tvInstrumentals, DefaultGenre.INSTRUMENTALS, "instrumental", "Instrumental", defaultGenre == DefaultGenre.INSTRUMENTALS),
            GenreModel(tvHipHopRap, DefaultGenre.HIPHOP, "rap", "Hip-Hop/R&B", defaultGenre == DefaultGenre.HIPHOP),
            GenreModel(tvElectronic, DefaultGenre.ELECTRONIC, "electronic", "Electronic", defaultGenre == DefaultGenre.ELECTRONIC),
            GenreModel(tvLatin, DefaultGenre.LATIN, "latin", "Latin", defaultGenre == DefaultGenre.LATIN),
            GenreModel(tvPop, DefaultGenre.POP, "pop", "Pop", defaultGenre == DefaultGenre.POP),
            GenreModel(tvPodcast, DefaultGenre.PODCAST, "podcast", "Podcast", defaultGenre == DefaultGenre.PODCAST)
        )

        genreModels.forEach { genreModel ->
            genreModel.button.setCompoundDrawablesWithIntrinsicBounds(genreModel.button.context.drawableCompat(if (genreModel.selected) R.drawable.ic_check_on else R.drawable.ic_check_off), null, null, null)
            genreModel.button.setOnClickListener {
                viewModel.onGenreSelected(genreModel)
            }
        }
    }

    companion object {
        fun show(activity: Activity?) {
            activity?.let {
                val intent = Intent(it, DefaultGenreActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(intent)
            }
        }
    }
}
