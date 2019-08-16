package it.sephiroth.android.numberpicker.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProviders
import it.sephiroth.android.library.numberpicker.NumberPicker
import it.sephiroth.android.numberpicker.demo.databinding.ActivityMainBinding
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    private lateinit var presenter: Presenter
    private lateinit var model: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProviders.of(this).get(MainViewModel::class.java)
        presenter = Presenter()

        val binding: ActivityMainBinding =
                DataBindingUtil.setContentView<ViewDataBinding>(this, R.layout.activity_main) as ActivityMainBinding
        binding.lifecycleOwner = this
        binding.model = model
        binding.presenter = presenter

        // regular listener
//        numberPicker.setListener {
//            onProgressChanged { numberPicker, progress, fromUser ->
//                Timber.v("doOnProgressChanged($progress, $fromUser)")
//            }
//
//            onStartTrackingTouch {
//                Timber.v("onStartTrackingTouch")
//            }
//
//            onStopTrackingTouch {
//                Timber.v("onStopTrackingTouch")
//            }
//        }
    }

    inner class Presenter {
        fun onProgressChanged(numberPicker: NumberPicker, progress: Int, fromUser: Boolean) {
            Timber.d("onProgressChanged")
            model.globalProgress.value = progress
        }

        fun onStartTracking(numberPicker: NumberPicker) {
            Timber.d("onStartTracking")
        }

        fun onStopTracking(numberPicker: NumberPicker) {
            Timber.d("onStopTracking")
        }
    }
}


