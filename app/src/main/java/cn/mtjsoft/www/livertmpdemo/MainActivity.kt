package cn.mtjsoft.www.livertmpdemo

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import cn.mtjsoft.www.livertmpdemo.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private val threadPoolExecutor = Executors.newSingleThreadExecutor()

    private var isStartLive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //保持亮屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent?.apply {
            val rtmp = getStringExtra("rtmp")
            if (rtmp != null && rtmp.isNotEmpty()) {
                PushLiveUtil.init.rtmp = rtmp
            }
        }
        preview()
    }

    /**
     * 请求 CameraProvider
     * 检查 CameraProvider 可用性
     */
    private fun preview() {
        // 请求 CameraProvider
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // 检查 CameraProvider 可用性
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 选择相机并绑定生命周期和用例
     */
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val size = Size(this.screenWidth, this.screenHeight)
        val preview: Preview = Preview.Builder()
            .setTargetResolution(size)
            .build()
        val cameraSelector: CameraSelector = if (binding.swCamera.isChecked) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(720, 1280))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageRotationEnabled(true)
            .build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
            try {
                if (imageProxy.image != null && isStartLive) {
                    val nv21 = ImageUtil.yuv420ThreePlanesToNV21(
                        imageProxy.image!!.planes,
                        imageProxy.width,
                        imageProxy.height
                    )
                    PushLiveUtil.init.startLiving(this, nv21, imageProxy.width, imageProxy.height)
                }
            } catch (e: Exception) {
                Log.e("cameraX", "error: " + e.message)
            } finally {
                imageProxy.close()
            }
        }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)

        binding.swCompat.setOnCheckedChangeListener { button, b ->
            isStartLive = b
            if (isStartLive.not()) {
                PushLiveUtil.init.stopLive()
            }
        }
        binding.swCamera.setOnCheckedChangeListener { button, b ->
            binding.swCamera.isEnabled = false
            cameraProvider.unbindAll()
            bindPreview(cameraProviderFuture.get())
        }
        binding.swCamera.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        PushLiveUtil.init.stopLive()
        threadPoolExecutor.shutdown()
    }
}