package it.sephiroth.android.numberpicker.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.sephiroth.android.library.numberpicker.doOnProgressChanged
import it.sephiroth.android.library.numberpicker.doOnStartTrackingTouch
import it.sephiroth.android.library.numberpicker.doOnStopTrackingTouch
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        numberPicker.doOnProgressChanged { numberPicker, progress, formUser ->
            // progress changed
        }

        numberPicker.doOnStartTrackingTouch { numberPicker ->
            // tracking started
        }

        numberPicker.doOnStopTrackingTouch { numberPicker ->
            // tracking ended
        }
    }
}
