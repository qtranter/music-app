package com.audiomack.ui.common

import android.content.Context
import com.audiomack.model.AMGenre

interface GenreProvider {
    fun getHumanValue(apiValue: String?): String

    fun getApiValue(humanValue: String?): String

    fun getHumanValueList(): List<String>
}

class AMGenreProvider(private val context: Context) : GenreProvider {

    override fun getHumanValue(apiValue: String?) =
        AMGenre.fromApiValue(apiValue).humanValue(context)

    override fun getApiValue(humanValue: String?) =
        AMGenre.fromHumanValue(humanValue, context).apiValue()

    override fun getHumanValueList(): List<String> {
        return listOf(
            AMGenre.Rap.humanValue(context),
            AMGenre.Rnb.humanValue(context),
            AMGenre.Electronic.humanValue(context),
            AMGenre.Dancehall.humanValue(context),
            AMGenre.Latin.humanValue(context),
            AMGenre.Afrobeats.humanValue(context),
            AMGenre.Djmix.humanValue(context),
            AMGenre.Pop.humanValue(context),
            AMGenre.Instrumental.humanValue(context),
            AMGenre.Podcast.humanValue(context),
            AMGenre.Rock.humanValue(context),
            AMGenre.Jazz.humanValue(context),
            AMGenre.Country.humanValue(context),
            AMGenre.World.humanValue(context),
            AMGenre.Classical.humanValue(context),
            AMGenre.Gospel.humanValue(context),
            AMGenre.Acapella.humanValue(context),
            AMGenre.Folk.humanValue(context),
            AMGenre.Other.humanValue(context)
        )
    }
}
