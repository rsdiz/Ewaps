package id.gasken.ewaps.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import id.gasken.ewaps.databinding.ActivityUserInputBinding

class UserInputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}