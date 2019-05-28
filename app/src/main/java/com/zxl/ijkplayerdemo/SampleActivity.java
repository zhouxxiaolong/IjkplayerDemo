package com.zxl.ijkplayerdemo;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TableLayout;

import com.zxl.ijkplayerdemo.widget.media.AndroidMediaController;
import com.zxl.ijkplayerdemo.widget.media.IjkVideoView;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created
 * 创 建 人: @author zhouxiaolong
 * 创建日期: 2019-05-27
 * 邮   箱: 1016579848@qq.com
 * 参   考: @link
 * 描   述:
 */
public class SampleActivity extends AppCompatActivity {

    private IjkVideoView ijkVideoView;
    private TableLayout mHudView;

    private AndroidMediaController mMediaController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        ijkVideoView = findViewById(R.id.video_view);
        mHudView = (TableLayout) findViewById(R.id.hud_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        mMediaController = new AndroidMediaController(this, false);
        mMediaController.setSupportActionBar(actionBar);
        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        ijkVideoView.setVideoURI(Uri.parse("rtsp://10.19.141.4:554/openUrl/5E01Kta"));
        ijkVideoView.setMediaController(mMediaController);
        ijkVideoView.setHudView(mHudView);
        ijkVideoView.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ijkVideoView.stopPlayback();
        ijkVideoView.release(true);
        ijkVideoView.stopBackgroundPlay();
        IjkMediaPlayer.native_profileEnd();
    }

}
