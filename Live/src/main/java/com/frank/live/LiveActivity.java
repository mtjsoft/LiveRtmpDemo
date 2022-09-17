package com.frank.live;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.frank.live.camera2.Camera2Helper;
import com.frank.live.listener.LiveStateChangeListener;
import com.frank.live.param.AudioParam;
import com.frank.live.param.VideoParam;

/**
 * Realtime living with rtmp stream
 * Created by frank on 2018/1/28.
 */

public class LiveActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener, LiveStateChangeListener {

    private final static String TAG = LiveActivity.class.getSimpleName();
    private final static String LIVE_URL = "rtmp://live-push.bilivideo.com/live-bvc/??streamname=live_491889741_1951543&key=e4cec58df064330165bde8357db13ed6&schedule=rtmp&pflag=1";
    private final static int MSG_ERROR = 100;
    private SurfaceView textureView;
    private LivePusherNew livePusher;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_ERROR) {
                String errMsg = (String) msg.obj;
                if (!TextUtils.isEmpty(errMsg)) {
                    Toast.makeText(LiveActivity.this, errMsg, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    int getLayoutId() {
        return R.layout.activity_live;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //保持亮屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }
        hideActionBar();
        initView();
        initPusher();
    }

    private void initView() {
        initViewsWithClick(R.id.btn_swap);
        ((ToggleButton) findViewById(R.id.btn_live)).setOnCheckedChangeListener(this);
        ((ToggleButton) findViewById(R.id.btn_mute)).setOnCheckedChangeListener(this);
        textureView = getView(R.id.surface_camera);
    }

    private void initPusher() {
        int width = 640;//resolution
        int height = 480;
        int videoBitRate = 800_000;//kb/s
        int videoFrameRate = 10;//fps
        VideoParam videoParam = new VideoParam(width, height,
                Integer.valueOf(Camera2Helper.CAMERA_ID_BACK), videoBitRate, videoFrameRate);
        int sampleRate = 44100;//sample rate: Hz
        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int numChannels = 2;//channel number
        AudioParam audioParam = new AudioParam(sampleRate, channelConfig, audioFormat, numChannels);
        livePusher = new LivePusherNew(this, videoParam, audioParam);
        livePusher.setPreviewDisplay(textureView.getHolder());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.btn_live) {//start or stop living
            if (isChecked) {
                livePusher.startPush(LIVE_URL, this);
            } else {
                livePusher.stopPush();
            }
        } else if (id == R.id.btn_mute) {//mute or not
            Log.i(TAG, "isChecked=" + isChecked);
            livePusher.setMute(isChecked);
        }
    }

    @Override
    public void onError(String msg) {
        Log.e(TAG, "errMsg=" + msg);
        mHandler.obtainMessage(MSG_ERROR, msg).sendToTarget();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (livePusher != null) {
            livePusher.release();
        }
    }

    @Override
    void onViewClick(View view) {
        if (view.getId() == R.id.btn_swap) {//switch camera
            livePusher.switchCamera();
        }
    }

    @Override
    void onSelectedFile(String filePath) {

    }
}
