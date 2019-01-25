package it.sephiroth.android.library.numberpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.widget.doOnTextChanged
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import it.sephiroth.android.library.uigestures.*
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

    private lateinit var editText: EditText
    private lateinit var upButton: AppCompatImageButton
    private lateinit var downButton: AppCompatImageButton

    private val delegate = UIGestureRecognizerDelegate()
    private lateinit var longGesture: UILongPressGestureRecognizer
    private lateinit var tapGesture: UITapGestureRecognizer

    private var tooltip: Tooltip? = null
    private lateinit var tracker: Tracker

    private var maxDistance: Int

    internal lateinit var data: Data

    private var callback = { newValue: Int ->
        value = newValue
    }

    private var buttonTimberInterval: Disposable? = null

    private val longGestureListener = { it: UIGestureRecognizer ->
        when {
            it.state == UIGestureRecognizer.State.Began -> {
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

                when (data.orientation) {
                    VERTICAL -> {
                        var diff = it.currentLocationY - it.downLocationY
                        if (diff > tracker.minDistance) {
                            diff = tracker.minDistance
                        } else if (diff < -tracker.minDistance) {
                            diff = -tracker.minDistance
                        }

                        val final = diff / tracker.minDistance
                        val final2 = sin(final * Math.PI / 2).toFloat()
                        tooltip?.offsetTo(tooltip!!.offsetX, final2 / 2 * tracker.minDistance)
                    }

                    HORIZONTAL -> {
                        val diff = it.currentLocationX - it.downLocationX
                        tooltip?.offsetTo(diff, tooltip!!.offsetY)
                    }
                }

                tracker.addMovement(it.currentLocationX, it.currentLocationY)
            }
        }
    }

    private val tapGestureListener = { it: UIGestureRecognizer ->
        if (!editText.isFocused)
            editText.requestFocus()
    }

    var value: Int
        get() = data.value
        set(value) {
            if (value != data.value) {
                data.value = value
                tooltip?.update(data.value.toString())

                if (editText.text.toString() != data.value.toString())
                    editText.setText(data.value.toString())
            }
        }

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

    private var arrowStyle: Int
    private var editTextStyleId: Int
    private var tooltipStyleId: Int

    init {
        Timber.i("init")

        setWillNotDraw(false)
        orientation = HORIZONTAL

        gravity = Gravity.CENTER

        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberPicker, defStyleAttr, defStyleRes)
        try {
            val maxValue = array.getInteger(R.styleable.NumberPicker_picker_max, 100)
            val minValue = array.getInteger(R.styleable.NumberPicker_picker_min, 0)
            val stepSize = array.getInteger(R.styleable.NumberPicker_picker_stepSize, 1)
            val orientation = array.getInteger(R.styleable.NumberPicker_picker_orientation, LinearLayout.VERTICAL)
            val value = array.getInteger(R.styleable.NumberPicker_picker_value, 0)
            arrowStyle = array.getResourceId(R.styleable.NumberPicker_picker_arrowStyle, 0)
            background = array.getDrawable(R.styleable.NumberPicker_android_background)
            editTextStyleId = array.getResourceId(R.styleable.NumberPicker_picker_editTextStyle, R.style.NumberPicker_EditTextStyle)
            tooltipStyleId = array.getResourceId(R.styleable.NumberPicker_picker_tooltipStyle, R.style.NumberPicker_ToolTipStyle)
            maxDistance = context.resources.getDimensionPixelSize(R.dimen.picker_distance_max)

            data = Data(value, minValue, maxValue, stepSize, orientation)

            val tracker_type = array.getInteger(R.styleable.NumberPicker_picker_tracker, TRACKER_LINEAR)
            tracker = when (tracker_type) {
                TRACKER_LINEAR -> LinearTracker(this, maxDistance, orientation, callback)
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
        initializeGestures()
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

        if (data.orientation == VERTICAL) {
            addView(downButton, params3)
            addView(editText, params2)
            addView(upButton, params1)
        } else {
            addView(downButton, params3)
            addView(editText, params2)
            addView(upButton, params1)
        }
    }


    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        delegate.isEnabled = enabled
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeButtonActions() {

        upButton.setOnTouchListener { v, event ->
            if (!isEnabled) {
                false
            } else {
                val action = event.actionMasked
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        value += stepSize
                        editText.clearFocus()
                        upButton.requestFocus()
                        upButton.isPressed = true
                        buttonTimberInterval?.dispose()

                        buttonTimberInterval = Observable.interval(
                                ARROW_BUTTON_INITIAL_DELAY,
                                ARROW_BUTTON_FRAME_DELAY,
                                TimeUnit.MILLISECONDS,
                                Schedulers.io())
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    value += stepSize
                                }
                    }

                    MotionEvent.ACTION_UP -> {
                        upButton.isPressed = false
                        buttonTimberInterval?.dispose()
                        buttonTimberInterval = null
                    }
                }

                true
            }
        }

        downButton.setOnTouchListener { v, event ->
            if (!isEnabled) {
                false
            } else {
                val action = event.actionMasked
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        value -= stepSize
                        editText.clearFocus()
                        downButton.requestFocus()
                        downButton.isPressed = true
                        buttonTimberInterval?.dispose()

                        buttonTimberInterval = Observable.interval(
                                ARROW_BUTTON_INITIAL_DELAY,
                                ARROW_BUTTON_FRAME_DELAY,
                                TimeUnit.MILLISECONDS,
                                Schedulers.io())
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    value -= stepSize
                                }
                    }

                    MotionEvent.ACTION_UP -> {
                        downButton.isPressed = false

                        buttonTimberInterval?.dispose()
                        buttonTimberInterval = null


                    }
                }

                true
            }
        }

        editText.doOnTextChanged { text, start, count, after ->
            if (!text.isNullOrEmpty()) {
                try {
                    this.value = Integer.valueOf(text.toString())
                } catch (e: NumberFormatException) {
                    Timber.e(e)
                }
            }
        }

        editText.setOnFocusChangeListener { v, hasFocus ->
            setBackgroundFocused(hasFocus)

            if (!hasFocus) {
                if (editText.text.isNullOrEmpty()) {
                    editText.setText(data.value.toString())
                }
            }
        }
    }

    private val focusedStateArray = intArrayOf(android.R.attr.state_focused)
    private val unfocusedStateArray = intArrayOf(-android.R.attr.state_focused)

    private fun setBackgroundFocused(hasFocus: Boolean) {
        if (hasFocus) {
            background?.state = focusedStateArray
        } else {
            background?.state = unfocusedStateArray
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
        Timber.i("startInteraction")
        animate().alpha(0.5f).start()

        tooltip = Tooltip.Builder(context)
                .anchor(editText, 0, 0, false)
                .styleId(tooltipStyleId)
                .arrow(true)
                .closePolicy(ClosePolicy.TOUCH_NONE)
                .overlay(false)
                .showDuration(0)
                .text(minValue.toString())
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

    }

    private fun endInteraction() {
        Timber.i("endInteraction")

        animate().alpha(1.0f).start()

        tooltip?.dismiss()
        tooltip = null
    }

    companion object {

        const val TRACKER_LINEAR = 0
        const val ARROW_BUTTON_INITIAL_DELAY = 800L
        const val ARROW_BUTTON_FRAME_DELAY = 16L
        const val LONG_TAP_TIMEOUT = 300L

        init {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        }
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

interface Tracker {
    fun begin(x: Float, y: Float)
    fun addMovement(x: Float, y: Float)
    fun end()
    var minDistance: Float
}


class LinearTracker(
        val numberPicker: NumberPicker,
        val maxDistance: Int,
        val orientation: Int,
        val callback: (Int) -> Unit
) : Tracker {

    private var initialValue: Int = 0

    override var minDistance: Float = 0f
    var downPosition: Float = 0f

    var minPoint = PointF(0f, 0f)

    fun calcDistance() {
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


    override fun begin(x: Float, y: Float) {
        Timber.i("begin($x, $y)")
        calcDistance()

        downPosition = if (orientation == LinearLayout.VERTICAL) y else x
        minPoint.set((-minDistance), (-minDistance))
        initialValue = numberPicker.value

    }

    override fun addMovement(x: Float, y: Float) {
        Timber.i("addMovement($x, $y)")

        val currentPosition = if (orientation == LinearLayout.VERTICAL) -y else x

        val diff: Float
        val perc: Float
        var finalValue: Int

        diff = max(-minDistance, min(currentPosition - downPosition, minDistance))
        perc = (diff / minDistance)
        finalValue = initialValue + (abs(numberPicker.maxValue - numberPicker.minValue) * perc).toInt()

        Timber.v("perc: $perc")
        Timber.v("initialValue: $initialValue")

        var diffValue = finalValue - numberPicker.value

        Timber.v("finalValue: $finalValue, diffValue: $diffValue, ${diffValue % numberPicker.stepSize}")

        if (numberPicker.stepSize > 1) {
            if (diffValue % numberPicker.stepSize != 0) {
                diffValue -= (diffValue % numberPicker.stepSize)
            }
        }

        finalValue = numberPicker.value + diffValue

        callback.invoke(finalValue)
    }

    override fun end() {
        Timber.i("end")
    }


}