package it.sephiroth.android.library.numberpicker

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingMethod
import androidx.databinding.InverseBindingMethods

@Suppress("unused")
@InverseBindingMethods(
    InverseBindingMethod(
        type = NumberPicker::class,
        attribute = "android:progress"
    )
)
class NumberPickerBindingAdapter {
    companion object {
        @BindingAdapter("progress")
        fun bindProgress(view: NumberPicker, value: Int) {
            view.progress = value
        }

        @BindingAdapter(value = ["picker_min", "picker_max"], requireAll = false)
        @JvmStatic
        fun bindMinAndMax(view: NumberPicker, minValue: Int?, maxValue: Int?) {
            if (null == minValue && null == maxValue) {
                throw java.lang.IllegalArgumentException("At least one value must be passed to picker_min or picker_max")
            }

            if (minValue != null && maxValue != null) {
                if (minValue >= maxValue) throw java.lang.IllegalArgumentException("picker_min cannot be bigger than picker_max")
            }

            minValue?.let { view.minValue = it }
            maxValue?.let { view.maxValue = it }

        }


        @BindingAdapter(
            value = ["android:onStartTrackingTouch", "android:onStopTrackingTouch", "android:onProgressChanged", "android:progressAttrChanged"],
            requireAll = false
        )
        @JvmStatic
        fun bindListener(
            view: NumberPicker,
            startListener: OnStartTrackingTouch?,
            stopListener: OnStopTrackingTouch?,
            progressListener: OnProgressChanged?,
            attrChanged: Boolean?
        ) {
            if (startListener == null && stopListener == null && progressListener == null) {
                view.numberPickerChangeListener = null
            } else {
                view.numberPickerChangeListener =
                    object : NumberPicker.OnNumberPickerChangeListener {
                        override fun onProgressChanged(
                            numberPicker: NumberPicker,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            progressListener?.onProgressChanged(numberPicker, progress, fromUser)
                        }

                        override fun onStartTrackingTouch(numberPicker: NumberPicker) {
                            startListener?.onStartTrackingTouch(numberPicker)
                        }

                        override fun onStopTrackingTouch(numberPicker: NumberPicker) {
                            stopListener?.onStopTrackingTouch(numberPicker)
                        }

                    }
            }
        }
    }

    interface OnStartTrackingTouch {
        fun onStartTrackingTouch(seekBar: NumberPicker)
    }

    interface OnStopTrackingTouch {
        fun onStopTrackingTouch(seekBar: NumberPicker)
    }

    interface OnProgressChanged {
        fun onProgressChanged(seekBar: NumberPicker, progress: Int, fromUser: Boolean)

    }
}