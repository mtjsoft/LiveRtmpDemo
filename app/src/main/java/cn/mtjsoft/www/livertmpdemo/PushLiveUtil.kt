package cn.mtjsoft.www.livertmpdemo

import android.app.Activity
import android.media.AudioFormat
import android.util.Log
import com.frank.live.LivePusherNew
import com.frank.live.param.AudioParam
import com.frank.live.param.VideoParam

class PushLiveUtil {
    companion object {
        val init: PushLiveUtil by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { PushLiveUtil() }
    }


    // 推流rtmp地址
//    private var rtmp = "rtmp://192.168.31.165:8035/live/home"
    private var rtmp =
        "rtmp://live-push.bilivideo.com/live-bvc/??streamname=live_491889741_1951543&key=e4cec58df064330165bde8357db13ed6&schedule=rtmp&pflag=1"

    // 视频
    private var videoParam: VideoParam? = null
    private val videoBitRate = 800000 //kb/s
    private val videoFrameRate = 20 //fps

    // 音频
    private val sampleRate = 44100 //sample rate: Hz
    private val channelConfig = AudioFormat.CHANNEL_IN_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val numChannels = 2 //channel number
    private val audioParam = AudioParam(sampleRate, channelConfig, audioFormat, numChannels)

    //
    private var livePusher: LivePusherNew? = null

    /**
     * 开始推流
     */
    fun startLiving(activity: Activity, nv21: ByteArray, w: Int, h: Int) {
        try {
            if (videoParam == null) {
                videoParam = VideoParam(
                    w, h,
                    1,
                    videoBitRate,
                    videoFrameRate
                )
            }
            if (livePusher == null) {
                livePusher = LivePusherNew(activity, rtmp, videoParam, audioParam) {
                    Log.e("PushLiveUtil", "直播推流状态： $it")
                }
            }
            livePusher?.startLive(nv21)
        } catch (e: Exception) {
            Log.e("PushLiveUtil", "直播推流失败${e.message}")
        }
    }

    /**
     * 停止推流
     */
    @Synchronized
    fun stopLive() {
        try {
            livePusher?.stopLive()
        } catch (e: Exception) {
            Log.e("mtj", "直播停止失败${e.toString()}")
        }
    }
}