package com.audiomack

import android.app.Application
import com.activeandroid.ActiveAndroid
import com.activeandroid.Configuration
import com.audiomack.model.AMArtist
import com.audiomack.model.AMBookmarkItem
import com.audiomack.model.AMBookmarkStatus
import com.audiomack.model.AMPlaylistTracks
import com.audiomack.model.AMResultItem

class TestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val configuration = Configuration.Builder(this).setDatabaseName("test")
        configuration.addModelClasses(
            AMResultItem::class.java,
            AMArtist::class.java,
            AMBookmarkItem::class.java,
            AMBookmarkStatus::class.java,
            AMPlaylistTracks::class.java
        )
        ActiveAndroid.initialize(configuration.create())
    }

    override fun onTerminate() {
        ActiveAndroid.dispose()
        super.onTerminate()
    }
}
