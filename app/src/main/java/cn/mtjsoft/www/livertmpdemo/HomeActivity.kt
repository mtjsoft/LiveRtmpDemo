package cn.mtjsoft.www.livertmpdemo

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cn.mtjsoft.www.livertmpdemo.databinding.ActivityHomeBinding
import cn.mtjsoft.www.livertmpdemo.databinding.ActivityMainBinding
import com.frank.live.LiveActivity
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ),
            100
        )


        binding.apply {
            btnCamera2.setOnClickListener {
                startActivity(Intent(this@HomeActivity, LiveActivity::class.java).apply {
                    putExtra("rtmp", getRTMP())
                })
            }
            btnCameraX.setOnClickListener {
                startActivity(Intent(this@HomeActivity, MainActivity::class.java).apply {
                    putExtra("rtmp", getRTMP())
                })
            }
        }
    }

    private fun getRTMP():String {
        return binding.etRtmp.text.toString().trim()
    }
}