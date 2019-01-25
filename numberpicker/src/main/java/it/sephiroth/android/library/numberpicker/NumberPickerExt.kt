package it.sephiroth.android.library.numberpicker


inline fun NumberPicker.doOnProgressChanged(
        crossinline action: (
                numberPicker: NumberPicker,
                progress: Int,
                formUser: Boolean) -> Unit) =
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

        override fun onProgressChanged(numberPicker: NumberPicker, progress: Int, fromUser: Boolean) {
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