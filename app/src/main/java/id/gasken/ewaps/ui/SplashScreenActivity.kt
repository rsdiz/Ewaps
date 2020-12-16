package id.gasken.ewaps.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import id.gasken.ewaps.R
import id.gasken.ewaps.databinding.ActivitySplashScreenBinding
import id.gasken.ewaps.tool.hideSystemUI
import id.gasken.ewaps.tool.viewBinding

class SplashScreenActivity : AppCompatActivity(R.layout.activity_splash_screen) {

    private val binding: ActivitySplashScreenBinding by viewBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed(
            {
                startActivity(Intent(this, ViewMapsActivity::class.java))
                finish()
            },
            1900
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}
