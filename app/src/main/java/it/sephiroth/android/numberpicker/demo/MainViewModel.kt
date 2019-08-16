package it.sephiroth.android.numberpicker.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val globalProgress: MutableLiveData<Int> = MutableLiveData<Int>().apply { value = 0 }

    val minValue: Int = -200
    val maxValue: Int = 200
    val defaultValue: Int = 0
}