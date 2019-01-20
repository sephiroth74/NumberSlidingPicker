package it.sephiroth.android.library.numberpicker

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.widget.TextView
import it.sephiroth.android.library.uigestures.UIGestureRecognizer
import it.sephiroth.android.library.uigestures.UIGestureRecognizerDelegate
import it.sephiroth.android.library.uigestures.UILongPressGestureRecognizer
import it.sephiroth.android.library.uigestures.setGestureDelegate
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class NumberPicker @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
                                            ) : TextView(context, attrs, defStyleAttr) {

    private val delegate = UIGestureRecognizerDelegate()
    private val longGesture: UILongPressGestureRecognizer
    private val initLocation = intArrayOf(0, 0)

    private var tooltip: Tooltip? = null
    private lateinit var tracker: Tracker

    private var maxDistance: Int

    internal val data: Data

    private var callback = { newValue: Int ->
        value = newValue
    }

    private var mLastLocationY: Float = 0f

    private val longGestureListener = { it: UIGestureRecognizer ->
        when {
            it.state == UIGestureRecognizer.State.Began -> {
                tracker.begin(it.downLocationX, it.downLocationY)

                mLastLocationY = it.downLocationY
                startInteraction()


            }
            it.state == UIGestureRecognizer.State.Ended -> {
                tracker.end()

                endInteraction()
            }
            it.state == UIGestureRecognizer.State.Changed -> {
                tracker.addMovement(it.currentLocationX, it.currentLocationY)

                val diff = it.currentLocationY - mLastLocationY

                tooltip?.offsetBy(0F, diff)
                mLastLocationY = it.currentLocationY
            }
        }
    }

    var value: Int
        get() = data.value
        set(value) {
            data.value = value
            text = data.value.toString()
            tooltip?.update(text)
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

    init {
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberPicker, 0, R.style.NumberPickerStyle)

        try {
            val maxValue = array.getInteger(R.styleable.NumberPicker_picker_max, 100)
            val minValue = array.getInteger(R.styleable.NumberPicker_picker_min, 0)
            val stepSize = array.getInteger(R.styleable.NumberPicker_picker_stepSize, 1)
            val orientation = array.getInteger(R.styleable.NumberPicker_picker_orientation, VERTICAL)
            val value = array.getInteger(R.styleable.NumberPicker_picker_value, 0)

            maxDistance = context.resources.getDimensionPixelSize(R.dimen.picker_distance_max)

            data = Data(value, minValue, maxValue, stepSize, orientation)
            text = data.value.toString()

            val tracker_type = array.getInteger(R.styleable.NumberPicker_picker_tracker, TRACKER_LINEAR)
            tracker = when (tracker_type) {
                TRACKER_LINEAR -> LinearTracker(this, maxDistance, orientation, callback)
                else -> {
                    LinearTracker(this, maxDistance, orientation, callback)
                }
            }


        } finally {
            array.recycle()
        }


        longGesture = UILongPressGestureRecognizer(context)
        longGesture.longPressTimeout = 200
        longGesture.actionListener = longGestureListener

        delegate.addGestureRecognizer(longGesture)

        delegate.isEnabled = isEnabled

        setGestureDelegate(delegate)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        delegate.isEnabled = enabled
    }


    private fun startInteraction() {
        Timber.i("startInteraction")
        animate().alpha(0.5f).start()

        tooltip = Tooltip.Builder(context)
            .anchor(this, 0, 0, false)
            .styleId(R.style.ToolTipStyle)
            .arrow(false)
            .closePolicy(ClosePolicy.TOUCH_NONE)
            .overlay(false)
            .showDuration(0)
            .text(text.toString())
            .create()

        tooltip?.show(this, Tooltip.Gravity.LEFT, false)
    }

    private fun endInteraction() {
        Timber.i("endInteraction")

        animate().alpha(1.0f).start()

        tooltip?.dismiss()
        tooltip = null
    }

    companion object {
        const val HORIZONTAL = 0
        const val VERTICAL = 1

        const val TRACKER_LINEAR = 0

        init {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        }
    }
}


class Data(value: Int, minValue: Int, maxValue: Int, var stepSize: Int, orientation: Int) {
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


class LinearTracker(val numberPicker: NumberPicker,
                    val maxDistance: Int,
                    val orientation: Int,
                    val callback: (Int) -> Unit) : Tracker {

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

        minDistance = if (orientation == NumberPicker.VERTICAL) {
            min(maxDistance, min(loc[1], metrics.heightPixels - loc[1])).toFloat()
        } else {
            min(maxDistance, min(loc[0], metrics.widthPixels - loc[0])).toFloat()
        }
    }


    override fun begin(x: Float, y: Float) {
        Timber.i("begin($x, $y)")
        calcDistance()

        downPosition = if (orientation == NumberPicker.VERTICAL) y else x
        minPoint.set((-minDistance), (-minDistance))
        initialValue = numberPicker.value

    }

    override fun addMovement(x: Float, y: Float) {
        Timber.i("addMovement($x, $y)")

        val currentPosition = if (orientation == NumberPicker.VERTICAL) y else x

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