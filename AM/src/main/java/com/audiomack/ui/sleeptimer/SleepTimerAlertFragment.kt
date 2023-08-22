package com.audiomack.ui.sleeptimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.postDelayed
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.audiomack.R
import com.audiomack.data.tracking.mixpanel.SleepTimerSource
import com.audiomack.model.InAppPurchaseMode
import com.audiomack.ui.premium.InAppPurchaseActivity
import kotlinx.android.synthetic.main.fragment_alert_sleep_timer.buttonClose
import kotlinx.android.synthetic.main.fragment_alert_sleep_timer.buttonPositive
import kotlinx.android.synthetic.main.fragment_alert_sleep_timer.mainContainer
import kotlinx.android.synthetic.main.fragment_alert_sleep_timer.pickerHour
import kotlinx.android.synthetic.main.fragment_alert_sleep_timer.pickerMin
import timber.log.Timber

class SleepTimerAlertFragment : DialogFragment() {

    private val source by lazy { requireArguments().get(ARGS_SOURCE) as SleepTimerSource }

    private val viewModel by viewModels<SleepTimerViewModel>(
        factoryProducer = { SleepTimerViewModelFactory(source) }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_alert_sleep_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObservers()
        initClickListeners()
        configureViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AudiomackDialogFragment)
    }

    private fun initViewModelObservers() {
        viewModel.apply {
            upgradeEvent.observe(viewLifecycleOwner, upgradeObserver)
            closeEvent.observe(viewLifecycleOwner, closeObserver)
        }
    }

    private fun initClickListeners() {
        // TODO: #1330 add clear button action ?
        mainContainer.setOnClickListener { viewModel.onCloseTapped() }
        buttonClose.setOnClickListener { viewModel.onCloseTapped() }
        buttonPositive.setOnClickListener {
            val seconds = (pickerHour.value * 60L * 60L + pickerMin.value * 5L * 60L)
            viewModel.onSetSleepTimerTapped(seconds)
        }
    }

    private val upgradeObserver = Observer<Void> {
        InAppPurchaseActivity.show(activity, InAppPurchaseMode.SleepTimer)
    }

    private val closeObserver = Observer<Void> {
        try {
            dismiss()
        } catch (e: IllegalStateException) {
            Timber.w(e)
        }
    }

    private fun configureViews() {
        val context = context ?: return
        val font = ResourcesCompat.getFont(context, R.font.opensans_semibold)

        pickerHour.apply {
            val hours = arrayListOf<String>()
            for (hour in 0 until 25) {
                val label = if (hour == 1) {
                    getString(R.string.sleep_timer_alert_hour)
                } else {
                    getString(R.string.sleep_timer_alert_hours)
                }
                hours.add("$hour $label")
            }

            displayedValues = hours.toArray(arrayOfNulls(hours.size))
            minValue = 0
            maxValue = hours.size - 1
            setContentTextTypeface(font)
            setOnValueChangedListener { _, _, newVal ->
                if (newVal == 0 && pickerMin.value == 0) pickerMin.value = 1
                else if (newVal == 24 && pickerMin.value != 0) pickerMin.value = 0
            }
        }

        pickerMin.apply {
            val minutes = arrayListOf<String>()
            for (minute in 0 until 12) minutes.add(
                "${minute * PICKER_STEPS} ${getString(R.string.sleep_timer_alert_minutes)}"
            )
            displayedValues = minutes.toArray(arrayOfNulls(minutes.size))
            minValue = 0
            maxValue = minutes.size - 1
            value = 1
            setContentTextTypeface(font)
            setOnValueChangedListener { _, _, newVal ->
                if (newVal == 0 && pickerHour.value == 0) postDelayed(100L) { value = 1 }
                else if (newVal != 0 && pickerHour.value == 24) postDelayed(100L) { value = 0 }
            }
        }
    }

    companion object {
        private const val ARGS_SOURCE = "source"
        private const val PICKER_STEPS = 5

        fun show(activity: FragmentActivity, source: SleepTimerSource) {
            try {
                val fragment = SleepTimerAlertFragment().apply {
                    arguments = bundleOf(ARGS_SOURCE to source)
                }
                fragment.show(activity.supportFragmentManager, fragment.javaClass.simpleName)
            } catch (e: IllegalStateException) {
                Timber.w(e)
            }
        }
    }
}
