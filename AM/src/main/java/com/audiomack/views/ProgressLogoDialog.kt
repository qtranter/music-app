package com.audiomack.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import com.audiomack.R
import kotlinx.android.synthetic.main.dialog_progress_logo.animationView

class ProgressLogoDialog @JvmOverloads constructor(
    context: Context,
    themeResId: Int = R.style.FullScreenDialogOverlay
) : Dialog(context, themeResId) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        setContentView(R.layout.dialog_progress_logo)

        setCancelable(false)

        animationView.show()
    }
}
