package com.audiomack.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.audiomack.R
import com.audiomack.data.autocompletion.EmailAutocompletionEngine
import com.audiomack.data.autocompletion.EmailAutocompletionInterface
import com.audiomack.utils.TextWatcherAdapter
import com.audiomack.utils.extensions.colorCompat
import com.audiomack.utils.getTypefaceSafely
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit.MILLISECONDS
import timber.log.Timber

class AMEmailAutocompleteEditTextLayout : LinearLayout {

    lateinit var typingEditText: EditText
        private set
    lateinit var autocompleteTextView: TextView
        private set

    private var autoCompletionInterfaceEmail: EmailAutocompletionInterface? = null

    private var typingEditTextHint: String? = null
    private var autocompleteString = ""

    private val autofillManager: AutofillManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(AutofillManager::class.java)
        } else {
            null
        }

    private val disposables = CompositeDisposable()

    private val emailTextObservable = PublishSubject.create<String>()

    private val editTextWatcher = object : TextWatcherAdapter() {
        override fun afterTextChanged(s: Editable?) {
            s?.toString()?.let { emailTextObservable.onNext(it) }
        }
    }

    private val emailTextSubscriber = object : Observer<String> {
        override fun onSubscribe(d: Disposable) {
            disposables.add(d)
        }

        override fun onNext(t: String) {
            adjustHint()
            refreshAutocompleteText()
        }

        override fun onError(e: Throwable) {
            typingEditText.error = resources.getString(R.string.signup_email)
        }

        override fun onComplete() {}
    }

    @SuppressLint("NewApi")
    private val editTextFocusListener = { _: View, hasFocus: Boolean ->
        if (!hasFocus) {
            commitAutocompleteText()
            typingEditText.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            autofillManager?.notifyViewExited(this@AMEmailAutocompleteEditTextLayout)
        } else {
            val rect = Rect()
            getGlobalVisibleRect(rect)
            autofillManager?.notifyViewEntered(this@AMEmailAutocompleteEditTextLayout)
        }
        Unit
    }

    constructor(context: Context) : super(context) {
        setup(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setup(context)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
    }

    private fun setup(context: Context) {
        orientation = HORIZONTAL

        typingEditText = EditText(context)
        typingEditText.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        )
        typingEditText.background = null
        typingEditText.setSingleLine()
        typingEditText.gravity = Gravity.CENTER_VERTICAL
        typingEditText.setPadding(0, 0, 0, 0)
        typingEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typingEditText.setTextColor(context.colorCompat(R.color.autocomplete_text))
        typingEditText.setHintTextColor(context.colorCompat(R.color.autocomplete_hint))
        typingEditText.addTextChangedListener(editTextWatcher)
        typingEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                typingEditText.clearFocus()
            }
            false
        }
        typingEditText.setOnFocusChangeListener(editTextFocusListener)

        try {
            val f = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            f.isAccessible = true
            f.set(typingEditText, R.drawable.login_edittext_cursor)
        } catch (ignored: Exception) {
        }

        addView(typingEditText)

        autocompleteTextView = TextView(context)
        autocompleteTextView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        autocompleteTextView.background = null
        autocompleteTextView.setSingleLine()
        autocompleteTextView.gravity = Gravity.CENTER_VERTICAL
        autocompleteTextView.setPadding(0, 0, 0, 0)
        autocompleteTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        autocompleteTextView.setTextColor(context.colorCompat(R.color.autocomplete_text))
        autocompleteTextView.setHintTextColor(context.colorCompat(R.color.autocomplete_hint))
        autocompleteTextView.setTextIsSelectable(false)
        autocompleteTextView.setOnClickListener {
            try {
                typingEditText.requestFocus()
                (typingEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(typingEditText, InputMethodManager.SHOW_IMPLICIT)
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
        addView(autocompleteTextView)

        typingEditText.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE

        typingEditText.typeface = context.getTypefaceSafely(R.font.opensans_regular)
        autocompleteTextView.typeface = context.getTypefaceSafely(R.font.opensans_regular)

        autoCompletionInterfaceEmail = EmailAutocompletionEngine()
        typingEditTextHint = resources.getString(R.string.signup_email_placeholder)

        typingEditText.letterSpacing = (-0.54F) / (typingEditText.textSize / resources.displayMetrics.density)
        autocompleteTextView.letterSpacing = (-0.54F) / (autocompleteTextView.textSize / resources.displayMetrics.density)

        adjustHint()

        emailTextObservable
            .debounce(200L, MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(emailTextSubscriber)
    }

    private fun adjustHint() {
        typingEditText.let {
            if (it.text.toString().isEmpty()) {
                it.hint = typingEditTextHint
                it.layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.MATCH_PARENT
                )
            } else {
                it.hint = null
                it.layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.MATCH_PARENT
                )
            }
        }
    }

    @SuppressLint("NewApi")
    private fun refreshAutocompleteText() {
        autoCompletionInterfaceEmail?.let {
            val text = typingEditText.text.toString()
            val ignoreCase = true
            autocompleteString = it.getCompletionForPrefix(text, ignoreCase)
            updateAutocompleteLabel()
            autofillManager?.notifyValueChanged(this@AMEmailAutocompleteEditTextLayout)
        }
    }

    private fun commitAutocompleteText() {
        if (autocompleteString.isNotEmpty()) {
            typingEditText.setText(typingEditText.text.toString() + autocompleteString)
            autocompleteString = ""
            updateAutocompleteLabel()
        }
    }

    private fun updateAutocompleteLabel() {
        autocompleteTextView.text = autocompleteString
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun autofill(value: AutofillValue?) {
        if (true == value?.isText) {
            typingEditText.setText(value.textValue, TextView.BufferType.EDITABLE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getAutofillType(): Int = View.AUTOFILL_TYPE_TEXT

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getAutofillValue(): AutofillValue? {
        return AutofillValue.forText(typingEditText.text.toString())
    }
}
