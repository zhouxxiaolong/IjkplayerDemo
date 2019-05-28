package com.zxl.ijkplayerdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 只需要导入如下两个依赖包就能完成视频的播放功能
 * implementation 'tv.danmaku.ijk.media:ijkplayer-java:0.8.8'
 * implementation 'tv.danmaku.ijk.media:ijkplayer-armv7a:0.8.8'
 */
public class SimpleExampleActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private IjkMediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_example);
        initSurfaceView();
        initPlayer();
    }

    private void initSurfaceView() {
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);
    }

    private void initPlayer() {
        mPlayer = new IjkMediaPlayer();
        try {
            String path = "http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8";
            mPlayer.setDataSource(path);

        } catch (IOException e) {
            e.printStackTrace();
        }

        mPlayer.prepareAsync();
        mPlayer.start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //将所播放的视频图像输出到指定的SurfaceView组件
        mPlayer.setDisplay(surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
    }
}
