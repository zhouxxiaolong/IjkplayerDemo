# IjkplayerDemo
视频直播，基于Ijkplayer

## 引言
因工作中遇到需要播放视频的需求，研究了[bilibili/ijkplayer](https://github.com/bilibili/ijkplayer)。
ijkplayer是B站基于 ffmpeg 开发并开源的轻量级视频播放器，支持播放本地网络视频，也支持流媒体播放。
支持 Android 和 iOS 操作系统。

## 使用方式
集成有两种方式，两者取其一即可。方式一较为方便，方式二就需要使用一步步执行命令，但是优势也很明显，可以定制默认情况下不支持的流媒体协议（比如RTSP）。
### 方式一

```
dependencies {
    # required, enough for most devices.
    compile 'tv.danmaku.ijk.media:ijkplayer-java:0.8.8'
    compile 'tv.danmaku.ijk.media:ijkplayer-armv7a:0.8.8'

    # Other ABIs: optional
    compile 'tv.danmaku.ijk.media:ijkplayer-armv5:0.8.8'
    compile 'tv.danmaku.ijk.media:ijkplayer-arm64:0.8.8'
    compile 'tv.danmaku.ijk.media:ijkplayer-x86:0.8.8'
    compile 'tv.danmaku.ijk.media:ijkplayer-x86_64:0.8.8'

    # ExoPlayer as IMediaPlayer: optional, experimental
    compile 'tv.danmaku.ijk.media:ijkplayer-exo:0.8.8'
}
```

### 方式二

```
git clone https://github.com/Bilibili/ijkplayer.git ijkplayer-android
cd ijkplayer-android
git checkout -B latest k0.8.8

./init-android.sh

cd android/contrib
./compile-ffmpeg.sh clean
./compile-ffmpeg.sh all

cd ..
./compile-ijk.sh all

```

## 拓展
### 支持RTSP
1、修改 ijkplayer-android/config/module-lite.sh

```
# 支持rtsp流
export COMMON_FF_CFG_FLAGS="$COMMON_FF_CFG_FLAGS --enable-protocol=rtp"
export COMMON_FF_CFG_FLAGS="$COMMON_FF_CFG_FLAGS --enable-protocol=tcp"
export COMMON_FF_CFG_FLAGS="$COMMON_FF_CFG_FLAGS --enable-demuxer=rtsp"
export COMMON_FF_CFG_FLAGS="$COMMON_FF_CFG_FLAGS --enable-demuxer=sdp"
export COMMON_FF_CFG_FLAGS="$COMMON_FF_CFG_FLAGS --enable-demuxer=rtp"
# 支持mpeg4解码
export COMMON_FF_CFG_FLAGS="$COMMON_FF_CFG_FLAGS --enable-decoder=mpeg4"
export COMMON_FF_CFG_FLAGS="$COMMON_FF_CFG_FLAGS --enable-demuxer=mpeg4"
```
2、创建ndk编译软链接

```
rm module.sh
ln -s module-lite.sh module.sh
```
3、继续

```
cd android/contrib
./compile-ffmpeg.sh clean


//后面操作跟之前一样
./init-android.sh

cd android/contrib
./compile-ffmpeg.sh clean
./compile-ffmpeg.sh all

cd ..
./compile-ijk.sh all
```
4、修改 ijkplayer-android/ijkmedia/ijkplayer/ff_ffplay.c的该方法

```
 static int packet_queue_get_or_buffering(FFPlayer *ffp, PacketQueue *q, AVPacket *pkt, int *serial, int finished)

    {

    while (1) {

    int new_packet = packet_queue_get(q, pkt, 1, serial);

    if (new_packet < 0)

    {

    new_packet = packet_queue_get(q, pkt, 0, serial);

    if(new_packet < 0)

    return -1;

    }

    else if (new_packet == 0) {

    if (!finished)

    ffp_toggle_buffering(ffp, 1);

    new_packet = packet_queue_get(q, pkt, 1, serial);

    if (new_packet < 0)

    return -1;

    }

    if (finished == *serial) {

    av_free_packet(pkt);

    continue;

    }

    else

    break;

    }

return 1;

}

```

5、重新引用生成的so包，但是会发现打开会有10s多的延迟，继续优化。

