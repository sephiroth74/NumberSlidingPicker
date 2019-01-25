package it.sephiroth.android.library.numberpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageButton
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

class NumberPicker @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var editText: TextView
    private lateinit var upButton: AppCompatImageButton
    private lateinit var downButton: AppCompatImageButton
    private var boxBackground: GradientDrawable? = null

    private val delegate = UIGestureRecognizerDelegate()
    private val longGesture: UILongPressGestureRecognizer
    private val initLocation = intArrayOf(0, 0)

    private var tooltip: Tooltip? = null
    private lateinit var tracker: Tracker

    private var maxDistance: Int

    internal lateinit var data: Data

    private var callback = { newValue: Int ->
        value = newValue
    }

    private var mLastLocationX: Float = 0f
    private var mLastLocationY: Float = 0f

    private var buttonTimberInterval: Disposable? = null

    private val longGestureListener = { it: UIGestureRecognizer ->
        when {
            it.state == UIGestureRecognizer.State.Began -> {
                editText.clearFocus()
                tracker.begin(it.downLocationX, it.downLocationY)
                mLastLocationY = it.downLocationY
                mLastLocationX = it.downLocationX
                startInteraction()


            }
            it.state == UIGestureRecognizer.State.Ended -> {
                tracker.end()
                endInteraction()
            }
            it.state == UIGestureRecognizer.State.Changed -> {

                if (data.orientation == VERTICAL) {
                    val diff = it.currentLocationY - mLastLocationY
                    tooltip?.offsetBy(0F, diff)
                } else {
                    val diff = it.currentLocationX - mLastLocationX
                    tooltip?.offsetBy(diff, 0F)
                }

                tracker.addMovement(it.currentLocationX, it.currentLocationY)
                mLastLocationX = it.currentLocationX
                mLastLocationY = it.currentLocationY
            }
        }
    }

    var value: Int
        get() = data.value
        set(value) {
            data.value = value
            editText.text = data.value.toString()
            tooltip?.update(data.value.toString())
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

    init {
        Timber.i("init")

        setWillNotDraw(false)
        gravity = Gravity.CENTER

        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberPicker, 0, R.style.NumberPickerStyle)
        try {
            val maxValue = array.getInteger(R.styleable.NumberPicker_picker_max, 100)
            val minValue = array.getInteger(R.styleable.NumberPicker_picker_min, 0)
            val stepSize = array.getInteger(R.styleable.NumberPicker_picker_stepSize, 1)
            val orientation = array.getInteger(R.styleable.NumberPicker_picker_orientation, LinearLayout.VERTICAL)
            val value = array.getInteger(R.styleable.NumberPicker_picker_value, 0)
            arrowStyle = array.getResourceId(R.styleable.NumberPicker_picker_arrowStyle, 0)

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

            editText.text = data.value.toString()


        } finally {
            array.recycle()
        }


        longGesture = UILongPressGestureRecognizer(context)
        longGesture.longPressTimeout = 200
        longGesture.actionListener = longGestureListener

        val tap = UITapGestureRecognizer(context)


        delegate.addGestureRecognizer(tap)
        delegate.addGestureRecognizer(longGesture)

        tap.actionListener = { it: UIGestureRecognizer ->
            editText.requestFocus()
        }

        delegate.isEnabled = isEnabled

        initializeButtonActions()

        editText.setGestureDelegate(delegate)
    }


    private fun inflateChildren() {
        upButton = AppCompatImageButton(context)
        upButton.setImageResource(R.drawable.arrow_up_selector)
        upButton.setBackgroundResource(R.drawable.arrow_up_background)

        if (data.orientation == HORIZONTAL) {
            upButton.rotation = 90f
        }

        editText =
                EditText(ContextThemeWrapper(context, android.R.style.Widget_Material_EditText), null, 0)
        editText.gravity = Gravity.CENTER
        editText.inputType = InputType.TYPE_CLASS_NUMBER

        editText.setLines(1)
        editText.setEms(max(abs(maxValue).toString().length, abs(minValue).toString().length))

        downButton = AppCompatImageButton(context)
        downButton.setImageResource(R.drawable.arrow_up_selector)
        downButton.setBackgroundResource(R.drawable.arrow_up_background)
        downButton.rotation = if (data.orientation == VERTICAL) 180f else -90f

        val params1 = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params1.weight = 0f

        val params2 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
        params2.weight = 1f

        val params3 = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params3.weight = 0f

        if (data.orientation == VERTICAL) {
            addView(upButton, params1)
            addView(editText, params2)
            addView(downButton, params3)
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
    }

    private fun startInteraction() {
        Timber.i("startInteraction")
        animate().alpha(0.5f).start()

        tooltip = Tooltip.Builder(context)
                .anchor(editText, 0, 0, false)
                .styleId(R.style.ToolTipStyle)
                .arrow(true)
                .closePolicy(ClosePolicy.TOUCH_NONE)
                .overlay(false)
                .showDuration(0)
                .text(minValue.toString())
                .animationStyle(if (orientation == VERTICAL) R.style.NumberPickerStyle_AnimationHorizontal else R.style.NumberPickerStyle_AnimationVertical)
                .create()

        tooltip?.doOnPrepare { tooltip ->
            tooltip.contentView?.let { contentView ->
                val textView = contentView.findViewById<TextView>(android.R.id.text1)
                textView.measure(0, 0)
                textView.minWidth = textView.measuredWidth
//                editText.text = text.toString()
//                tooltip.offsetBy(-contentView.measuredWidth.toFloat(), 0f)
            }
        }

        tooltip?.doOnShown {
            //            it.update(text.toString())
        }

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
}


class LinearTracker(
        val numberPicker: NumberPicker,
        val maxDistance: Int,
        val orientation: Int,
        val callback: (Int) -> Unit
) : Tracker {

    private var initialValue: Int = 0

    var minDistance: Float = 0f
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

        val currentPosition = if (orientation == LinearLayout.VERTICAL) y else x

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