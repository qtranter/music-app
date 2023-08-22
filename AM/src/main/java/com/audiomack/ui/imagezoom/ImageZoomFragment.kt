package com.audiomack.ui.imagezoom

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.audiomack.R
import com.audiomack.fragments.TrackedFragment
import kotlinx.android.synthetic.main.fragment_imagezoom.*

class ImageZoomFragment : TrackedFragment(R.layout.fragment_imagezoom, TAG) {

    private val viewModel: ImageZoomViewModel by viewModels()
    private var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = arguments?.getString(ARG_URL, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.toggleProgressBar.observe(viewLifecycleOwner) { progressBar.isVisible = it }
        viewModel.toggleImageView.observe(viewLifecycleOwner) { imageView.isVisible = it }

        buttonClose.setOnClickListener { viewModel.onCloseTapped() }

        imageView.setOnSingleFlingListener { _, _, velocityX, velocityY ->
            viewModel.onImageViewFling(velocityX, velocityY)
            true
        }

        viewModel.loadImage(imageView.context, url, imageView)
    }

    companion object {
        private const val TAG = "ImageZoomFragment"
        private const val ARG_URL = "arg_url"

        @JvmStatic
        fun newInstance(url: String?): ImageZoomFragment =
            ImageZoomFragment().apply {
                arguments = url?.let { bundleOf(ARG_URL to it) }
            }
    }
}
