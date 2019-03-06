package it.sephiroth.android.numberpicker.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.sephiroth.android.library.numberpicker.setListener
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        numberPicker.setListener {
            onProgressChanged { numberPicker, progress, fromUser ->
                Timber.v("doOnProgressChanged($progress, $fromUser)")
            }

            onStartTrackingTouch {
                Timber.v("onStartTrackingTouch")
            }

            onStopTrackingTouch {
                Timber.v("onStopTrackingTouch")
            }
        }
    }
}
