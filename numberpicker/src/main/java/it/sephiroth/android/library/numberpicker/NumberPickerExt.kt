@file:Suppress("unused")
package it.sephiroth.android.library.numberpicker

import kotlinx.coroutines.*

inline fun NumberPicker.doOnProgressChanged(
    crossinline action: (
        numberPicker: NumberPicker,
        progress: Int,
        formUser: Boolean
    ) -> Unit
) =
    addProgressChangedListener(progressChanged = action)

inline fun NumberPicker.doOnStartTrackingTouch(crossinline action: (numberPicker: NumberPicker) -> Unit) =
    addProgressChangedListener(startTrackingTouch = action)

inline fun NumberPicker.doOnStopTrackingTouch(crossinline action: (numberPicker: NumberPicker) -> Unit) =
    addProgressChangedListener(stopTrackingTouch = action)


inline fun NumberPicker.addProgressChangedListener(
    crossinline progressChanged: (
        numberPicker: NumberPicker,
        progress: Int,
        formUser: Boolean
    ) -> Unit = { _, _, _ -> },

    crossinline startTrackingTouch: (numberPicker: NumberPicker) -> Unit = { _ -> },

    crossinline stopTrackingTouch: (numberPicker: NumberPicker) -> Unit = { _ -> }

): NumberPicker.OnNumberPickerChangeListener {
    val listener = object : NumberPicker.OnNumberPickerChangeListener {

        override fun onProgressChanged(
            numberPicker: NumberPicker,
            progress: Int,
            fromUser: Boolean
        ) {
            progressChanged.invoke(numberPicker, progress, fromUser)
        }

        override fun onStartTrackingTouch(numberPicker: NumberPicker) {
            startTrackingTouch.invoke(numberPicker)
        }

        override fun onStopTrackingTouch(numberPicker: NumberPicker) {
            stopTrackingTouch.invoke(numberPicker)
        }

    }
    numberPickerChangeListener = listener
    return listener
}

class OnNumberPickerChangeListener : NumberPicker.OnNumberPickerChangeListener {

    override fun onProgressChanged(numberPicker: NumberPicker, progress: Int, fromUser: Boolean) {
        _onProgressChanged?.invoke(numberPicker, progress, fromUser)
    }

    override fun onStartTrackingTouch(numberPicker: NumberPicker) {
        _onStartTrackingTouch?.invoke(numberPicker)
    }

    override fun onStopTrackingTouch(numberPicker: NumberPicker) {
        _onStopTrackingTouch?.invoke(numberPicker)
    }

    fun onProgressChanged(func: (NumberPicker, Int, Boolean) -> Unit) {
        _onProgressChanged = func
    }

    fun onStartTrackingTouch(func: (NumberPicker) -> Unit) {
        _onStartTrackingTouch = func
    }

    fun onStopTrackingTouch(func: (NumberPicker) -> Unit) {
        _onStopTrackingTouch = func
    }

    private var _onProgressChanged: ((NumberPicker, Int, Boolean) -> Unit)? = null
    private var _onStartTrackingTouch: ((NumberPicker) -> Unit)? = null
    private var _onStopTrackingTouch: ((NumberPicker) -> Unit)? = null
}

inline fun NumberPicker.setListener(func: OnNumberPickerChangeListener.() -> Unit) {
    val listener = OnNumberPickerChangeListener()
    listener.func()
    numberPickerChangeListener = listener
}

fun intervalJob(
    initialDelay: Long = 0L,
    period: Long = 700L,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    callback: (Int) -> Unit
): Job {
    var value = 0
    return scope.launch {
        delay(initialDelay)
        while (isActive) {
            callback(value++)
            delay(period)
        }
    }
}