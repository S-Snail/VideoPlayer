package com.example.sunshow_02.videoplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Environment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import static android.content.res.Configuration.*;

public class MainActivity extends Activity{

    private VideoView mVideoView;
    private SeekBar media_seekbar, volume_seekbar;
    private ImageView player_controller,screen_controller,operation_bg,operation_percent, volume_image;
    private TextView current_time,total_time;

    private int screen_width,screen_height;
    private RelativeLayout videoLayout;

    public static final int UPDATE_UI = 1;

    private AudioManager mAudiomanager;
    private boolean isFulssScreen = false;

    private boolean isAdjust = false;
    private float threshold = 54*2;

    private float mBrightness;
    private float lastX,lastY;
    private FrameLayout center_show_progress;

    private VolumnReceiver volumeReceiver;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_UI){
                int currentPosition = mVideoView.getCurrentPosition();
                int totalDuration = mVideoView.getDuration();
                updateTextViewWithTimeFormat(current_time,currentPosition);
                updateTextViewWithTimeFormat(total_time,totalDuration);
                media_seekbar.setMax(totalDuration);
                media_seekbar.setProgress(currentPosition);
                mHandler.sendEmptyMessageDelayed(UPDATE_UI,500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudiomanager = (AudioManager) getSystemService(AUDIO_SERVICE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        InitUI();
        SetPlayerEvent();
        /**
         * 播放本地视频
         */
        String video_path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/video/robot.mp4";
        mVideoView.setVideoPath(video_path);
        mHandler.sendEmptyMessage(UPDATE_UI);
        mVideoView.start();
    }

    private void updateTextViewWithTimeFormat(TextView tv,int millisecond){
        int second = millisecond/1000;
        int hh = second/3600;
        int mm = second%3600/60;
        int ss = second%60;
        String str = null;
        if (hh!=0){
            str = String.format("%02d:%02d:%02d",hh,mm,ss);
        }
        else
        {
            str = String.format("%02d:%02d",mm,ss);
        }
        tv.setText(str);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeMessages(UPDATE_UI);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mVideoView.canPause()){
            player_controller.setImageResource(R.drawable.media_stop_style);
        }

        if (mHandler.hasMessages(UPDATE_UI)){
            mHandler.removeMessages(UPDATE_UI);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (volumeReceiver != null){
//            unregisterReceiver(volumeReceiver);
//        }
        if (mHandler!= null){
            mHandler.removeCallbacksAndMessages(UPDATE_UI);
        }
    }

    private void SetPlayerEvent() {
        /**
         * 控制视频播放和暂停
         */
        player_controller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoView.isPlaying()){
                    player_controller.setImageResource(R.drawable.media_stop_style);
                    //暂停播放
                    mVideoView.pause();
                    mHandler.removeMessages(UPDATE_UI);
                }
                else
                {
                    player_controller.setImageResource(R.drawable.media_play_style);
                    //继续播放
                    mVideoView.start();
                    mHandler.sendEmptyMessage(UPDATE_UI);
                }
            }
        });

        media_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //同步显示播放当前时间的text view
                updateTextViewWithTimeFormat(current_time,progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                /**
                 * 在拖动时应该停止UI刷新
                 */
                mHandler.removeMessages(UPDATE_UI);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();//获取到拖动的进度
                //令视频播放进度遵循seekBar停止拖动的这一刻的进度
                mVideoView.seekTo(progress);
                mHandler.sendEmptyMessage(UPDATE_UI);
            }
        });

        volume_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /**
                 * 设置当前的音量
                 */
                mAudiomanager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        screen_controller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFulssScreen){
                    /**
                     * 如果是半屏显示，切换为全屏显示
                     */
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                else
                {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        });

        /**
         * 控制VideoView的手势事件
         */
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                float x = event.getX();//触摸时当前x轴和y轴的滑动距离
                float y = event.getY();

                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        lastX = x;
                        lastY = y;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float detlaX = x - lastX;
                        float detlaY = y - lastY;
                        float absdetlaX = Math.abs(detlaX);
                        float absdetlaY = Math.abs(detlaY);
                        if (absdetlaX > threshold && absdetlaY > threshold)
                        {
                            if (absdetlaX < absdetlaY){
                                isAdjust = true;
                            }
                            else
                            {
                                isAdjust = false;
                            }
                        }
                        else if (absdetlaX < threshold && absdetlaY > threshold){
                            isAdjust = true;
                        }
                        else if (absdetlaX >threshold && absdetlaY < threshold)
                        {
                            isAdjust = false;
                        }

                        Log.e("Main","手势是否合法" + isAdjust);
                        if (isAdjust){
                            /**
                             * 在判断好当前手势是否合法的前提下，去区分此时手势是应该调节亮度还是调节声音
                             */
                            if ( x < screen_width / 2){
                                /**
                                 * 调节亮度
                                 */
                                if (detlaY > 0){
                                    /**
                                     * 降低亮度
                                     */
                                    Log.e("Main","降低亮度"+detlaY);
                                }
                                else
                                {
                                    /**
                                     * 升高亮度
                                     */
                                    Log.e("Main","升高亮度"+detlaY);
                                }
                                changeBrightness(-detlaY);
                            }
                            else
                            {
                                /**
                                 * 调节声音
                                  */
                                if (detlaY > 0){
                                    /**
                                     * 减小声音
                                     */
                                    Log.e("Mian","减小声音"+detlaY);
                                }
                                else
                                {
                                    /**
                                     * 增大声音
                                     */
                                    Log.e("Main","增大声音"+detlaY);
                                }
                                    changeVolumn(-detlaY);
                            }
                        }
                        lastX = x;
                        lastY = y;
                        break;

                    case MotionEvent.ACTION_UP:
                        lastX = 0;
                        lastY = 0;
                        center_show_progress.setVisibility(View.GONE);
                        break;
                }
                return true;
            }
        });
    }

    /**
     * 改变音量
     */
    private void changeVolumn(float detlaY){
        int max = mAudiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = mAudiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int index = (int) (detlaY/screen_height*max*4);
        int volumn = Math.max(current + index,0);
        if (center_show_progress.getVisibility() == View.GONE){
            center_show_progress.setVisibility(View.VISIBLE);
        }
        operation_bg.setImageResource(R.drawable.video_voice_bg);
        ViewGroup.LayoutParams params = operation_percent.getLayoutParams();
        params.width = (int) (PixelUtil.dp2px(94)*(volumn*1.0f/max));
        operation_percent.setLayoutParams(params);

        mAudiomanager.setStreamVolume(AudioManager.STREAM_MUSIC,volumn,0);
        volume_seekbar.setProgress(volumn);
    }

    private void changeBrightness(float detlaY){
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        mBrightness = attributes.screenBrightness;
        float index = detlaY/screen_height;
        mBrightness+=index;
        if (mBrightness > 1.0f){
            mBrightness = 1.0f;
        }
        if (mBrightness < 0.01f){
            mBrightness = 0.01f;
        }
        attributes.screenBrightness = mBrightness;
        getWindow().setAttributes(attributes);

        if (center_show_progress.getVisibility() == View.GONE){
            center_show_progress.setVisibility(View.VISIBLE);
        }
        operation_bg.setImageResource(R.drawable.video_brightness_bg);
        ViewGroup.LayoutParams params = operation_percent.getLayoutParams();
        params.width = (int) (PixelUtil.dp2px(94)*(mBrightness));
        operation_percent.setLayoutParams(params);
    }

    private void InitUI() {
        volumeReceiver = new VolumnReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver,intentFilter);

        PixelUtil.initContext(this);//初始化，防止找不到上下文
        mVideoView = (VideoView) findViewById(R.id.videoView);
        media_seekbar = (SeekBar) findViewById(R.id.media_seekbar);
        volume_seekbar = (SeekBar) findViewById(R.id.volumn_seekbar);
        player_controller = (ImageView) findViewById(R.id.player_controller);
        screen_controller = (ImageView) findViewById(R.id.screen_controller);
        current_time = (TextView) findViewById(R.id.current_time);
        total_time = (TextView) findViewById(R.id.total_time);
        volume_image = (ImageView) findViewById(R.id.volume_image);

        operation_bg = (ImageView) findViewById(R.id.operation_bg);
        operation_percent = (ImageView) findViewById(R.id.operation_percent);
        center_show_progress = (FrameLayout) findViewById(R.id.center_show_progress);

        screen_width = getResources().getDisplayMetrics().widthPixels;
        screen_height = getResources().getDisplayMetrics().heightPixels;
        videoLayout = (RelativeLayout) findViewById(R.id.videoLayout);

        /**
         * 当前设备的最大音量
         */
        int max_volumn = mAudiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        /**
         * 当前设备的音量
         */
        int current_volumn = mAudiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume_seekbar.setMax(max_volumn);
        volume_seekbar.setProgress(current_volumn);
//        mAudiomanager.setStreamVolume(AudioManager.STREAM_MUSIC,current_volumn,0);
    }

    /**
     * 给VideoView和VideoView最外层的RelativeLayout设置宽高
     * @param width
     * @param height
     */
    private void setVideoViewSize(int width,int height){
        ViewGroup.LayoutParams layoutParams = mVideoView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        mVideoView.setLayoutParams(layoutParams);

        ViewGroup.LayoutParams layoutParams1 = videoLayout.getLayoutParams();
        layoutParams1.width = width;
        layoutParams1.height = height;
        videoLayout.setLayoutParams(layoutParams1);
    }

    /**
     * 监听屏幕的方向时横屏还是竖屏
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /**
         *当屏幕方向为横屏的时候
         * (横屏时将videoView的大小进行拉伸以适应整个屏幕)
         */
        if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE){
            setVideoViewSize(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
            isFulssScreen = true;

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);//移除半屏的状态
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            center_show_progress.setVisibility(View.GONE);
        }
        else
        {
            setVideoViewSize(ViewGroup.LayoutParams.MATCH_PARENT,PixelUtil.dp2px(240));
            isFulssScreen = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            center_show_progress.setVisibility(View.GONE);
        }
    }

    private class VolumnReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")){
                int volume = mAudiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (volume == 0 ){
                    volume_image.setImageResource(R.drawable.mute);
                }else
                {
                    volume_image.setImageResource(R.mipmap.volume);
                }
                volume_seekbar.setProgress(volume);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else {
            super.onBackPressed();
        }
    }
}
