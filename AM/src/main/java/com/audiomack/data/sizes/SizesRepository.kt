package com.audiomack.data.sizes

import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import com.audiomack.utils.Utils
import kotlin.math.roundToInt

interface SizesDataSource {
    val tinyMusic: Int
    val smallMusic: Int
    val largeMusic: Int

    val tinyArtist: Int
    val smallArtist: Int
    val mediumArtist: Int
    val largeArtist: Int

    val screenHeightDp: Int
    val screenHeight: Int

    fun initialize(context: Context)
}

object SizesRepository : SizesDataSource {

    private var density: Float = 0F
    private var screenSize: Point = Point(1080, 1920)
    private var tinyMusicImageSizeInternal: Int = 60
    private var smallMusicImageSizeInternal: Int = 150
    private var fullScreenSizeInternal: Int = 750
    private var tinyArtistImageSizeInternal: Int = 80
    private var smallArtistImageSizeInternal: Int = 150
    private var mediumArtistImageSizeInternal: Int = 200

    override val tinyMusic: Int
        get() = tinyMusicImageSizeInternal

    override val smallMusic: Int
        get() = smallMusicImageSizeInternal

    override val largeMusic: Int
        get() = fullScreenSizeInternal

    override val tinyArtist: Int
        get() = tinyArtistImageSizeInternal

    override val smallArtist: Int
        get() = smallArtistImageSizeInternal

    override val mediumArtist: Int
        get() = mediumArtistImageSizeInternal

    override val largeArtist: Int
        get() = fullScreenSizeInternal

    override val screenHeightDp: Int
        get() = (screenSize.y.toFloat() / density).roundToInt()

    override val screenHeight: Int
        get() = screenSize.y

    override fun initialize(context: Context) {
        density = context.resources.displayMetrics.density

        screenSize = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.let { windowManager ->
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            size
        } ?: Point(0, 0)

        tinyMusicImageSizeInternal = Utils.roundNumber((30.toFloat() * density).roundToInt(), 20)
        smallMusicImageSizeInternal = Utils.roundNumber((80.toFloat() * density).roundToInt(), 50)
        fullScreenSizeInternal = Utils.roundNumber(screenSize.x, 150)
        tinyArtistImageSizeInternal = Utils.roundNumber((40.toFloat() * density).roundToInt(), 20)
        mediumArtistImageSizeInternal = Utils.roundNumber((100.toFloat() * density).roundToInt(), 50)
        smallArtistImageSizeInternal = Utils.roundNumber((80.toFloat() * density).roundToInt(), 50)
    }
}
