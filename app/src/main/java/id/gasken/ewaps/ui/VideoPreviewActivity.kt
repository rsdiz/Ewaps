package id.gasken.ewaps.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import id.gasken.ewaps.R
import id.gasken.ewaps.databinding.ActivityVideoPreviewBinding
import id.gasken.ewaps.tool.Const
import id.gasken.ewaps.tool.viewBinding

class VideoPreviewActivity : AppCompatActivity() {

    private val TAG = "VideoPreviewActivity"

    private val binding: ActivityVideoPreviewBinding by viewBinding()

    val storage = FirebaseStorage.getInstance()

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_preview)

        supportActionBar?.title = "Ewaps Video Player"

        val isOnlineVideo = intent.extras?.get("online") as Boolean

        val mediaController = MediaController(this)
        binding.videopreview.setMediaController(mediaController)
        mediaController.setAnchorView(binding.videopreview)

        if (isOnlineVideo) {
            val storageRef = storage.reference
            val videoPath = intent.extras?.get(Const.VIDEOPATH) as String
            val videoResource = storageRef.child(videoPath).child("0")
            videoResource.downloadUrl.addOnSuccessListener {
                Log.d(TAG, "===============downloadUrl: $it")
//                val uri = Uri.parse()
                setVideoVisible(true)
                binding.videopreview.setVideoURI(it)
                binding.videopreview.keepScreenOn = true
                binding.videopreview.start()
//                Toast.makeText(this, "video loaded", Toast.LENGTH_SHORT)
            }
        } else {
            val videoUri = intent.extras?.get("videoUri") as Uri

            binding.videopreview.setVideoURI(videoUri)
            binding.videopreview.start()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        binding.videopreview.stopPlayback()
    }

    private fun setVideoVisible(state: Boolean) {
        if (state) {
            binding.lottieLoading.visibility = View.GONE
            binding.videopreview.visibility = View.VISIBLE
        } else {
            binding.videopreview.visibility = View.GONE
            binding.videopreview.visibility = View.VISIBLE
        }
    }
}
