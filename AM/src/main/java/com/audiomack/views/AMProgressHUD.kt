package com.audiomack.views

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.audiomack.R
import com.audiomack.model.ProgressHUDMode
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class AMProgressHUD {

    enum class Style {
        SUCCESS, ERROR, PROGRESS, NONE
    }

    companion object {
        private var staticView: View? = null
        private var staticHandler: Handler? = null
        private const val delayMs: Long = 2000

        @JvmStatic fun show(activity: Activity?, mode: ProgressHUDMode) {
            when (mode) {
                ProgressHUDMode.Loading -> showWithStatus(activity)
                ProgressHUDMode.Dismiss -> dismiss()
                is ProgressHUDMode.Failure -> showWithError(activity,
                    if (mode.stringResId != null) activity?.getString(mode.stringResId) else mode.message
                )
            }
        }

        @JvmStatic fun showWithSuccess(activity: Activity?, text: String?) {
            showWithAutoDismiss(activity, Style.SUCCESS, text)
        }

        @JvmStatic fun showWithError(activity: Activity?, text: String?) {
            showWithAutoDismiss(activity, Style.ERROR, text)
        }

        @JvmStatic fun showWithStatus(activity: Activity?) {
            showInView(activity, Style.PROGRESS, null)
        }

        @JvmStatic fun showWithStatus(activity: Activity?, text: String?) {
            showInView(activity, Style.PROGRESS, text)
        }

        @JvmStatic private fun showWithAutoDismiss(activity: Activity?, style: Style, text: String?) {
            if (staticHandler != null) {
                dismiss()
            }
            staticHandler = Handler()
            staticHandler?.postDelayed({ dismiss() }, delayMs)

            showInView(activity, style, text)
        }

        @JvmStatic fun showInView(activity: Activity?, style: Style, text: String?) {
            if (staticView == null && activity != null) {
                (activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater)?.let {
                    staticView = it.inflate(R.layout.view_progresshud, null)
                }
            }

            staticView?.let {
                val animationView = it.findViewById<ProgressLogoView>(R.id.animationView)
                if (style == Style.PROGRESS) {
                    animationView.show()
                } else {
                    animationView.hide()
                }

                val imageView = it.findViewById<ImageView>(R.id.imageView)
                if (style != Style.NONE && style != Style.PROGRESS) {
                    imageView.visibility = View.VISIBLE
                    imageView.setImageResource(if (style == Style.SUCCESS) R.drawable.hud_success else R.drawable.hud_failure)
                } else {
                    imageView.visibility = View.GONE
                }

                try {
                    show(activity, it)
                } catch (e: Exception) {
                    Timber.w(e)
                }

                if ((style == Style.SUCCESS || style == Style.ERROR) && !TextUtils.isEmpty(text)) {
                    try {
                        AMSnackbar.Builder(activity!!)
                            .withDrawable(if (style == Style.SUCCESS) R.drawable.ic_snackbar_success else R.drawable.ic_snackbar_error)
                            .withTitle(text!!)
                            .withDuration(Snackbar.LENGTH_SHORT)
                            .show()
                    } catch (e: Exception) {
                        Timber.w(e)
                    }
                }
            }
        }

        @JvmStatic fun dismiss() {
            try {
                hide(staticView)
            } catch (e: Exception) {
                Timber.w(e)
            }

            staticView = null
            staticHandler?.removeCallbacksAndMessages(null)
            staticHandler = null
        }

        @JvmStatic private fun show(context: Context?, v: View) {
            val params: WindowManager.LayoutParams = WindowManager.LayoutParams()
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            params.format = PixelFormat.TRANSLUCENT
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
            params.title = AMProgressHUD::class.java.simpleName
            (context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.let {
                if (v.parent != null) {
                    it.updateViewLayout(v, params)
                } else {
                    it.addView(v, params)
                }
            }
        }

        private fun hide(v: View?) {
            val windowManager = (v?.context?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager) ?: return
            v.parent?.let {
                windowManager.removeView(v)
            }
        }
    }
}
