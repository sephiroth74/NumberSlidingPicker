package it.sephiroth.android.library.numberpicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import it.sephiroth.android.library.uigestures.UIGestureRecognizer
import it.sephiroth.android.library.uigestures.UIGestureRecognizerDelegate
import it.sephiroth.android.library.uigestures.UILongPressGestureRecognizer
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

    private lateinit var textView: TextView
    private lateinit var upButton: AppCompatImageButton
    private lateinit var downButton: AppCompatImageButton

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

    private var mLastLocationX: Float = 0f
    private var mLastLocationY: Float = 0f

    private val longGestureListener = { it: UIGestureRecognizer ->
        when {
            it.state == UIGestureRecognizer.State.Began -> {
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

                if (orientation == VERTICAL) {
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
            textView.text = data.value.toString()
//            tooltip?.update(text)
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
            textView.text = data.value.toString()

//            upButton.setOnClickListener {
//                this.value += stepSize
//            }

            downButton.setOnClickListener {
                this.value -= stepSize
            }

        } finally {
            array.recycle()
        }


        longGesture = UILongPressGestureRecognizer(context)
        longGesture.longPressTimeout = 200
        longGesture.actionListener = longGestureListener

        delegate.addGestureRecognizer(longGesture)

        delegate.isEnabled = isEnabled

        initializeButtonActions()

//        setGestureDelegate(delegate)
    }

    private fun inflateChildren() {
        upButton = AppCompatImageButton(context)
        upButton.setImageResource(R.drawable.arrow_up_selector)
        upButton.setBackgroundResource(R.drawable.arrow_up_background)

        var params = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.weight = 0f

        addView(upButton, params)


        textView =
            AppCompatTextView(ContextThemeWrapper(context, android.R.style.Widget_Material_TextView), null, 0)
        textView.gravity = Gravity.CENTER
        textView.minEms = max(abs(maxValue).toString().length, abs(minValue).toString().length)

        params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.weight = 1f

        addView(textView, params)

        downButton = AppCompatImageButton(context)
        downButton.setImageResource(R.drawable.arrow_up_selector)
        downButton.setBackgroundResource(R.drawable.arrow_up_background)
        downButton.rotation = 180f

        params = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.weight = 0f

        addView(downButton, params)


    }

    var buttonTimberInterval: Disposable? = null

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
                        upButton.isPressed = true
                        buttonTimberInterval?.dispose()

                        buttonTimberInterval = Observable.interval(
                            ARROW_BUTTON_INITIAL_DELAY,
                            ARROW_BUTTON_FRAME_DELAY,
                            TimeUnit.MILLISECONDS,
                            Schedulers.io()
                        )
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
                        downButton.isPressed = true
                        buttonTimberInterval?.dispose()

                        buttonTimberInterval = Observable.interval(
                            ARROW_BUTTON_INITIAL_DELAY,
                            ARROW_BUTTON_FRAME_DELAY,
                            TimeUnit.MILLISECONDS,
                            Schedulers.io()
                        )
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
            .anchor(this, 0, 0, false)
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
//                textView.text = text.toString()
//                tooltip.offsetBy(-contentView.measuredWidth.toFloat(), 0f)
            }
        }

        tooltip?.doOnShown {
            //            it.update(text.toString())
        }

        tooltip?.show(this, if (orientation == VERTICAL) Tooltip.Gravity.LEFT else Tooltip.Gravity.TOP, false)


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