package it.sephiroth.android.library.numberpicker

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import it.sephiroth.android.library.uigestures.UIGestureRecognizer
import it.sephiroth.android.library.uigestures.UIGestureRecognizerDelegate
import it.sephiroth.android.library.uigestures.UILongPressGestureRecognizer
import it.sephiroth.android.library.uigestures.setGestureDelegate
import timber.log.Timber

class NumberPicker @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    private var mPopupWindow: PopupWindow? = null
    private val mDelegate = UIGestureRecognizerDelegate()

    init {
        setBackgroundColor(Color.CYAN)

        val gesture = UILongPressGestureRecognizer(context)
        gesture.longPressTimeout = 200
        gesture.actionListener = { it: UIGestureRecognizer ->
            when (it.state) {
                UIGestureRecognizer.State.Began -> {
                    startInteraction()
                }

                UIGestureRecognizer.State.Ended -> {
                    endInteraction()
                }

                UIGestureRecognizer.State.Changed -> {
                    Timber.v("number changing... ${it.currentLocationY} - ${it.downLocationY}")
                }
                else -> {
                }
            }
        }

        mDelegate.addGestureRecognizer(gesture)
        setGestureDelegate(mDelegate)
    }

    fun startInteraction() {
        Timber.i("startInteraction")
        val popup = PopupWindow(context)

        val content = TextView(context)
        content.layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        content.text = text

        popup.setBackgroundDrawable(ColorDrawable(Color.RED))
        popup.animationStyle = R.style.Animation
        popup.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        popup.contentView = content
        popup.isFocusable = false
        popup.isTouchable = false

        val location = IntArray(2)
        this.getLocationOnScreen(location)

        popup.showAtLocation(this, Gravity.NO_GRAVITY, location[0] - width - 20, location[1])

        mPopupWindow = popup
    }

    fun endInteraction() {
        Timber.i("endIteraction")
        mPopupWindow?.dismiss()
        mPopupWindow = null
    }

    companion object {
        init {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        }
    }
}