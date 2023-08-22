package com.audiomack.model

import android.content.Context
import com.audiomack.R

enum class AMResultItemSort {
    NewestFirst {
        override fun clause(): String = """CASE 
            WHEN download_date IS NULL THEN ID
            ELSE download_date
            END DESC
            """
        override fun index(): Int = 0
        override fun humanValue(context: Context?) = context?.getString(R.string.offline_sort_newest) ?: ""
    },
    OldestFirst {
        override fun clause(): String = """CASE 
            WHEN download_date IS NULL THEN ID
            ELSE download_date
            END ASC
            """
        override fun index(): Int = 1
        override fun humanValue(context: Context?) = context?.getString(R.string.offline_sort_oldest) ?: ""
    },
    AToZ {
        override fun clause(): String = "artist ASC"
        override fun index(): Int = 2
        override fun humanValue(context: Context?) = context?.getString(R.string.offline_sort_alphabetically) ?: ""
    };
    abstract fun clause(): String
    abstract fun index(): Int
    abstract fun humanValue(context: Context?): String

    companion object {
        fun fromIndex(index: Int?): AMResultItemSort {
            return values().firstOrNull { it.index() == index } ?: NewestFirst
        }
    }
}
