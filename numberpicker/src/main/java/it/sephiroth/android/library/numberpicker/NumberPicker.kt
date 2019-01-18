package it.sephiroth.android.library.numberpicker

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.PopupWindow
import android.widget.TextView
import it.sephiroth.android.library.uigestures.UIGestureRecognizer
import it.sephiroth.android.library.uigestures.UIGestureRecognizerDelegate
import it.sephiroth.android.library.uigestures.UILongPressGestureRecognizer
import it.sephiroth.android.library.uigestures.setGestureDelegate
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class NumberPicker @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    private var mPopupWindow: PopupWindow? = null
    private var mContentView: TextView? = null

    private val mDelegate = UIGestureRecognizerDelegate()

    private val mMin = 0
    private val mMax = 100

    private var mCurrent = 10
        set(value) {
            field = min(mMax, max(mMin, value))
            text = field.toString()
//            mContentView?.text = text


            tooltip?.update(text)
        }

    private var mDownPosition = 0
    private var mLastLocation: Float = 0f

    init {
        setBackgroundColor(Color.CYAN)
        text = mCurrent.toString()

        val gesture = UILongPressGestureRecognizer(context)
        gesture.longPressTimeout = 200
        gesture.actionListener = { it: UIGestureRecognizer ->
            when (it.state) {
                UIGestureRecognizer.State.Began -> {
                    mDownPosition = it.downLocationY.toInt()
                    mLastLocation = it.currentLocationY
                    startInteraction()
                }

                UIGestureRecognizer.State.Ended -> {
                    endInteraction()
                }

                UIGestureRecognizer.State.Changed -> {
                    val distance = (it.currentLocationY - mLastLocation) / 2
                    mCurrent -= distance.toInt()

                    tooltip?.offsetBy(0f, it.currentLocationY - mLastLocation)

                    mLastLocation = it.currentLocationY
                }
                else -> {
                }
            }
        }

        mDelegate.addGestureRecognizer(gesture)
        setGestureDelegate(mDelegate)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }


    private var tooltip: Tooltip? = null
    val mInitLocation = intArrayOf(0, 0)

    fun startInteraction() {
        Timber.i("startInteraction")

        getLocationOnScreen(mInitLocation)
        mInitLocation[1] += height / 2
        mInitLocation[0] -= width / 2

        tooltip = Tooltip.Builder(context)
            .anchor(this, -20, 0, false)
            .arrow(true)
            .closePolicy(ClosePolicy.TOUCH_NONE)
            .overlay(false)
            .showDuration(0)
            .fadeDuration(200)
            .text(text.toString())
            .create()

        tooltip?.show(this, Tooltip.Gravity.LEFT, false)
//
//        val popup = PopupWindow(context)
//
//        val content = TextView(context)
//        content.layoutParams =
//                ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        content.text = text
//        content.gravity = Gravity.RIGHT
//        content.setPadding(20)
//
//        popup.setBackgroundDrawable(ColorDrawable(Color.RED))
//        popup.animationStyle = R.style.Animation
//        popup.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
//        popup.contentView = content
//        popup.isFocusable = false
//        popup.isTouchable = false
//
//        val location = IntArray(2)
//        this.getLocationOnScreen(location)
//
//        popup.showAtLocation(this, Gravity.NO_GRAVITY, location[0] - width * 2, location[1] - 20)
//
//        mPopupWindow = popup
//        mContentView = content
    }

    fun endInteraction() {
        Timber.i("endIteraction")

        tooltip?.dismiss()

        mPopupWindow?.dismiss()
        mPopupWindow = null
        mContentView = null
    }

    companion object {
        init {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        }
    }
}