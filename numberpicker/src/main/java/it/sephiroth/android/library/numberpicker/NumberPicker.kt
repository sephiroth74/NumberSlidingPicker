package it.sephiroth.android.library.numberpicker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageButton
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import it.sephiroth.android.library.uigestures.UIGestureRecognizer
import it.sephiroth.android.library.uigestures.UIGestureRecognizerDelegate
import it.sephiroth.android.library.uigestures.UILongPressGestureRecognizer
import it.sephiroth.android.library.uigestures.UITapGestureRecognizer
import it.sephiroth.android.library.uigestures.setGestureDelegate
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin


class NumberPicker @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.pickerStyle,
        defStyleRes: Int = R.style.NumberPicker_Filled) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    interface OnNumberPickerChangeListener {
        fun onProgressChanged(numberPicker: NumberPicker, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(numberPicker: NumberPicker)
        fun onStopTrackingTouch(numberPicker: NumberPicker)
    }

    var numberPickerChangeListener: OnNumberPickerChangeListener? = null

    private lateinit var editText: EditText
    private lateinit var upButton: AppCompatImageButton
    private lateinit var downButton: AppCompatImageButton
    private lateinit var tracker: Tracker

    private var arrowStyle: Int
    private var editTextStyleId: Int
    private var tooltipStyleId: Int

    private val delegate = UIGestureRecognizerDelegate()
    private lateinit var longGesture: UILongPressGestureRecognizer
    private lateinit var tapGesture: UITapGestureRecognizer
    private var disableGestures: Boolean = false

    private var tooltip: Tooltip? = null
    private var maxDistance: Int

    private lateinit var data: Data

    private var callback = { newValue: Int ->
        setProgress(newValue)
    }

    private var buttonInterval: Disposable? = null

    private val longGestureListener = { it: UIGestureRecognizer ->
        Timber.i("longGestureListener = ${it.state}")
        when {
            it.state == UIGestureRecognizer.State.Began -> {
                requestFocus()
                editText.isSelected = false
                editText.clearFocus()

                tracker.begin(it.downLocationX, it.downLocationY)
                startInteraction()
            }

            it.state == UIGestureRecognizer.State.Ended -> {
                tracker.end()
                endInteraction()
            }

            it.state == UIGestureRecognizer.State.Changed -> {
                var diff =
                        if (data.orientation == VERTICAL) it.currentLocationY - it.downLocationY else it.currentLocationX - it.downLocationX
                if (diff > tracker.minDistance) {
                    diff = tracker.minDistance
                } else if (diff < -tracker.minDistance) {
                    diff = -tracker.minDistance
                }
                val final2 = sin((diff / tracker.minDistance) * Math.PI / 2).toFloat()

                tooltip?.let { tooltip ->
                    when (data.orientation) {
                        VERTICAL -> tooltip.offsetTo(tooltip.offsetX, final2 / 2 * tracker.minDistance)
                        HORIZONTAL -> tooltip.offsetTo(final2 / 2 * tracker.minDistance, tooltip.offsetY)
                    }
                }

                tracker.addMovement(it.currentLocationX, it.currentLocationY)
            }
        }
    }

    private val tapGestureListener = { _: UIGestureRecognizer ->
        requestFocus()
        if (!editText.isFocused)
            editText.requestFocus()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setProgress(value: Int, fromUser: Boolean = true) {
        Timber.i("setProgress($value, $fromUser)")
        if (value != data.value) {
            data.value = value
            tooltip?.update(data.value.toString())

            if (editText.text.toString() != data.value.toString())
                editText.setText(data.value.toString())

            numberPickerChangeListener?.onProgressChanged(this, progress, fromUser)
        }
    }

    var progress: Int
        get() = data.value
        set(value) = setProgress(value, false)

    var minValue: Int
        get() = data.minValue
        set(value) {
            data.minValue = value
        }

    var maxValue: Int
        get() = data.maxValue
        set(value) {
            data.maxValue = value
        }

    var stepSize: Int
        get() = data.stepSize
        set(value) {
            data.stepSize = value
        }

    private var initialized = false

    init {
        setWillNotDraw(false)
        isFocusable = true
        isFocusableInTouchMode = true

        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberPicker, defStyleAttr, defStyleRes)
        try {
            val maxValue = array.getInteger(R.styleable.NumberPicker_picker_max, 100)
            val minValue = array.getInteger(R.styleable.NumberPicker_picker_min, 0)
            val stepSize = array.getInteger(R.styleable.NumberPicker_picker_stepSize, 1)
            val orientation = array.getInteger(R.styleable.NumberPicker_picker_orientation, LinearLayout.VERTICAL)
            val value = array.getInteger(R.styleable.NumberPicker_android_progress, 0)
            arrowStyle = array.getResourceId(R.styleable.NumberPicker_picker_arrowStyle, 0)
            background = array.getDrawable(R.styleable.NumberPicker_android_background)
            editTextStyleId = array.getResourceId(R.styleable.NumberPicker_picker_editTextStyle, R.style.NumberPicker_EditTextStyle)
            tooltipStyleId = array.getResourceId(R.styleable.NumberPicker_picker_tooltipStyle, R.style.NumberPicker_ToolTipStyle)
            disableGestures = array.getBoolean(R.styleable.NumberPicker_picker_disableGestures, false)
            maxDistance = context.resources.getDimensionPixelSize(R.dimen.picker_distance_max)

            data = Data(value, minValue, maxValue, stepSize, orientation)

            val tracker_type = array.getInteger(R.styleable.NumberPicker_picker_tracker, TRACKER_LINEAR)
            tracker = when (tracker_type) {
                TRACKER_LINEAR -> LinearTracker(this, maxDistance, orientation, callback)
                TRACKER_EXPONENTIAL -> ExponentialTracker(this, maxDistance, orientation, callback)
                else -> {
                    LinearTracker(this, maxDistance, orientation, callback)
                }
            }
            inflateChildren()

            editText.setText(data.value.toString())


        } finally {
            array.recycle()
        }

        initializeButtonActions()
        if(!disableGestures) {
            initializeGestures()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        delegate.isEnabled = enabled
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun inflateChildren() {
        upButton = AppCompatImageButton(context)
        upButton.setImageResource(R.drawable.arrow_up_selector_24)
        upButton.setBackgroundResource(R.drawable.arrow_up_background)

        if (data.orientation == HORIZONTAL) {
            upButton.rotation = 90f
        }

        editText = EditText(ContextThemeWrapper(context, editTextStyleId), null, 0)
        editText.setLines(1)
        editText.setEms(max(abs(maxValue).toString().length, abs(minValue).toString().length))
        editText.isFocusableInTouchMode = true
        editText.isFocusable = true
        editText.isClickable = true
        editText.isLongClickable = false


        downButton = AppCompatImageButton(context)
        downButton.setImageResource(R.drawable.arrow_up_selector_24)
        downButton.setBackgroundResource(R.drawable.arrow_up_background)
        downButton.rotation = if (data.orientation == VERTICAL) 180f else -90f

        val params1 = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params1.weight = 0f

        val params2 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
        params2.weight = 1f

        val params3 = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params3.weight = 0f

        addView(downButton, params3)
        addView(editText, params2)
        addView(upButton, params1)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeButtonActions() {
        upButton.setOnTouchListener { _, event ->
            if (!isEnabled) {
                false
            } else {
                val action = event.actionMasked
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        requestFocus()
                        setProgress(progress + stepSize)
                        editText.clearFocus()
                        hideKeyboard()

                        upButton.requestFocus()
                        upButton.isPressed = true
                        buttonInterval?.dispose()

                        buttonInterval = Observable.interval(
                                ARROW_BUTTON_INITIAL_DELAY,
                                ARROW_BUTTON_FRAME_DELAY,
                                TimeUnit.MILLISECONDS,
                                Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                setProgress(progress + stepSize)
                            }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        upButton.isPressed = false
                        buttonInterval?.dispose()
                        buttonInterval = null
                    }
                }

                true
            }
        }

        downButton.setOnTouchListener { _, event ->
            if (!isEnabled) {
                false
            } else {
                val action = event.actionMasked
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        requestFocus()
                        setProgress(progress - stepSize)
                        editText.clearFocus()
                        hideKeyboard()

                        downButton.requestFocus()
                        downButton.isPressed = true
                        buttonInterval?.dispose()

                        buttonInterval = Observable.interval(
                                ARROW_BUTTON_INITIAL_DELAY,
                                ARROW_BUTTON_FRAME_DELAY,
                                TimeUnit.MILLISECONDS,
                                Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                setProgress(progress - stepSize)
                            }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        downButton.isPressed = false

                        buttonInterval?.dispose()
                        buttonInterval = null


                    }
                }

                true
            }
        }

//        editText.doOnTextChanged { text, _, _, _ ->
//            if (!text.isNullOrEmpty()) {
//                try {
//                    this.setProgress(Integer.valueOf(text.toString()))
//                } catch (e: NumberFormatException) {
//                    Timber.e(e)
//                }
//            }
//        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            setBackgroundFocused(hasFocus)

            if (!hasFocus) {
                if (!editText.text.isNullOrEmpty()) {
                    setProgress(Integer.valueOf(editText.text.toString()), true)
                } else {
                    editText.setText(data.value.toString())
                }
            }
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    editText.clearFocus()
                    true
                }
                else -> false
            }
        }
    }

    private fun setBackgroundFocused(hasFocus: Boolean) {
        if (hasFocus) {
            background?.state = FOCUSED_STATE_ARRAY
        } else {
            background?.state = UNFOCUSED_STATE_ARRAY
        }
    }

    private fun initializeGestures() {
        longGesture = UILongPressGestureRecognizer(context)
        longGesture.longPressTimeout = LONG_TAP_TIMEOUT
        longGesture.actionListener = longGestureListener
        longGesture.cancelsTouchesInView = false

        tapGesture = UITapGestureRecognizer(context)
        tapGesture.cancelsTouchesInView = false

        delegate.addGestureRecognizer(longGesture)
        delegate.addGestureRecognizer(tapGesture)

        tapGesture.actionListener = tapGestureListener

        delegate.isEnabled = isEnabled

        editText.setGestureDelegate(delegate)
    }

    private fun startInteraction() {
        animate().alpha(0.5f).start()

        tooltip = Tooltip.Builder(context)
            .anchor(editText, 0, 0, false)
            .styleId(tooltipStyleId)
            .arrow(true)
            .closePolicy(ClosePolicy.TOUCH_NONE)
            .overlay(false)
            .showDuration(0)
            .text(if (minValue.toString().length > maxValue.toString().length) minValue.toString() else maxValue.toString())
            .animationStyle(if (orientation == VERTICAL) R.style.NumberPicker_AnimationVertical else R.style.NumberPicker_AnimationHorizontal)
            .create()

        tooltip?.doOnPrepare { tooltip ->
            tooltip.contentView?.let { contentView ->
                val textView = contentView.findViewById<TextView>(android.R.id.text1)
                textView.measure(0, 0)
                textView.minWidth = textView.measuredWidth
            }
        }

        tooltip?.doOnShown { it.update(data.value.toString()) }
        tooltip?.show(this, if (data.orientation == VERTICAL) Tooltip.Gravity.LEFT else Tooltip.Gravity.TOP, false)

        numberPickerChangeListener?.onStartTrackingTouch(this)

    }

    private fun endInteraction() {
        Timber.i("endInteraction")

        animate().alpha(1.0f).start()

        tooltip?.dismiss()
        tooltip = null

        numberPickerChangeListener?.onStopTrackingTouch(this)
    }

    companion object {

        const val TRACKER_LINEAR = 0
        const val TRACKER_EXPONENTIAL = 1

        const val ARROW_BUTTON_INITIAL_DELAY = 800L
        const val ARROW_BUTTON_FRAME_DELAY = 16L
        const val LONG_TAP_TIMEOUT = 300L

        val FOCUSED_STATE_ARRAY = intArrayOf(android.R.attr.state_focused)
        val UNFOCUSED_STATE_ARRAY = intArrayOf(0, -android.R.attr.state_focused)
    }
}


class Data(value: Int, minValue: Int, maxValue: Int, var stepSize: Int, val orientation: Int) {
    var value: Int = value
        set(value) {
            field = max(minValue, min(maxValue, value))
        }

    var maxValue: Int = maxValue
        set(newValue) {
            if (newValue < minValue) throw IllegalArgumentException("maxValue cannot be less than minValue")
            field = newValue
            if (value > newValue) value = newValue
        }

    var minValue: Int = minValue
        set(newValue) {
            if (newValue > maxValue) throw IllegalArgumentException("minValue cannot be great than maxValue")
            field = newValue
            if (newValue > value) value = newValue
        }

}

internal abstract class Tracker(
        val numberPicker: NumberPicker,
        private val maxDistance: Int,
        val orientation: Int,
        val callback: (Int) -> Unit) {

    internal var started: Boolean = false
    internal var initialValue: Int = 0
    internal var downPosition: Float = 0f

    @Suppress("MemberVisibilityCanBePrivate")
    internal var minPoint = PointF(0f, 0f)

    open fun begin(x: Float, y: Float) {
        Timber.i("begin($x, $y)")
        calcDistance()

        downPosition = if (orientation == LinearLayout.VERTICAL) -y else x
        minPoint.set((-minDistance), (-minDistance))
        initialValue = numberPicker.progress
        started = true
    }

    abstract fun addMovement(x: Float, y: Float)

    open fun end() {
        Timber.i("end()")
        started = false
    }

    var minDistance: Float = 0f

    private fun calcDistance() {
        Timber.i("maxDistance: $maxDistance")

        val loc = intArrayOf(0, 0)
        val metrics = numberPicker.resources.displayMetrics
        numberPicker.getLocationOnScreen(loc)
        loc[0] += numberPicker.width / 2
        loc[1] += numberPicker.height / 2

        minDistance = if (orientation == LinearLayout.VERTICAL) {
            min(maxDistance, min(loc[1], metrics.heightPixels - loc[1])).toFloat()
        } else {
            min(maxDistance, min(loc[0], metrics.widthPixels - loc[0])).toFloat()
        }
    }
}

internal class ExponentialTracker(
        numberPicker: NumberPicker,
        maxDistance: Int,
        orientation: Int,
        callback: (Int) -> Unit) : Tracker(numberPicker, maxDistance, orientation, callback) {

    private var time: Long = 1000L
    private var direction: Int = 0

    private val handler = Handler()

    private var runnable: Runnable = object : Runnable {
        override fun run() {
            if (!started) return

            if (direction > 0)
                callback.invoke(numberPicker.progress + numberPicker.stepSize)
            else if (direction < 0)
                callback.invoke(numberPicker.progress - numberPicker.stepSize)

            if (started)
                handler.postDelayed(this, time)
        }
    }

    override fun begin(x: Float, y: Float) {
        super.begin(x, y)
        direction = 0
        time = MAX_TIME_DELAY
        handler.post(runnable)
    }

    override fun addMovement(x: Float, y: Float) {
        Timber.i("addMovement($x, $y)")

        val currentPosition = if (orientation == LinearLayout.VERTICAL) -y else x
        val diff: Float
        val perc: Float

        diff = max(-minDistance, min(currentPosition - downPosition, minDistance))
        perc = (diff / minDistance)

        direction = when {
            perc > 0 -> 1
            perc < 0 -> -1
            else -> 0
        }

        time = (MAX_TIME_DELAY - ((MAX_TIME_DELAY - MIN_TIME_DELAY).toFloat() * abs(perc))).toLong()
    }

    override fun end() {
        super.end()
        handler.removeCallbacks(runnable)
    }

    companion object {
        const val MAX_TIME_DELAY = 200L
        const val MIN_TIME_DELAY = 16L
    }
}

internal class LinearTracker(
        numberPicker: NumberPicker,
        maxDistance: Int,
        orientation: Int,
        callback: (Int) -> Unit) : Tracker(numberPicker, maxDistance, orientation, callback) {


    override fun addMovement(x: Float, y: Float) {
        Timber.i("addMovement($x, $y)")
        val currentPosition = if (orientation == LinearLayout.VERTICAL) -y else x

        val diff: Float
        val perc: Float
        var finalValue: Int

        diff = max(-minDistance, min(currentPosition - downPosition, minDistance))
        perc = (diff / minDistance)
        finalValue = initialValue + (abs(numberPicker.maxValue - numberPicker.minValue) * perc).toInt()

        var diffValue = finalValue - numberPicker.progress

        if (numberPicker.stepSize > 1) {
            if (diffValue % numberPicker.stepSize != 0) {
                diffValue -= (diffValue % numberPicker.stepSize)
            }
        }

        finalValue = numberPicker.progress + diffValue

        callback.invoke(finalValue)
    }
}
