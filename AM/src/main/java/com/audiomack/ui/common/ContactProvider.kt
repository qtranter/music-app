package com.audiomack.ui.common

import android.content.Context
import com.audiomack.R

interface ContactProvider {
    fun getWhatTitleList(): List<String>

    fun getHowTitleList(): List<String>

    fun getWhenTitleList(): List<String>
}

class AMContactProvider(private val context: Context) : ContactProvider {

    override fun getWhatTitleList(): List<String> {
        return listOf(
                context.getString(R.string.contact_selection_what_login),
                context.getString(R.string.contact_selection_what_signup)
        )
    }

    override fun getHowTitleList(): List<String> {
        return listOf(
                context.getString(R.string.contact_selection_how_google),
                context.getString(R.string.contact_selection_how_twitter),
                context.getString(R.string.contact_selection_how_facebook),
                context.getString(R.string.contact_selection_how_email)
        )
    }

    override fun getWhenTitleList(): List<String> {
        return listOf(
                context.getString(R.string.contact_selection_when_today),
                context.getString(R.string.contact_selection_when_last_week),
                context.getString(R.string.contact_selection_when_last_month),
                context.getString(R.string.contact_selection_when_dont_remember)
        )
    }
}
