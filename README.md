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
		if (new_packet < 0){
			new_packet = packet_queue_get(q, pkt, 0, serial);
			if(new_packet < 0)
			return -1;
		} else if (new_packet == 0) {
			if (!finished)
				ffp_toggle_buffering(ffp, 1);
			new_packet = packet_queue_get(q, pkt, 1, serial);
			if (new_packet < 0)
				return -1;
		}
		
		if (finished == *serial) {
			av_free_packet(pkt);
			continue;
		} else {
			break;
		}
			
    }
    return 1;
}

```

5、重新引用生成的so包，但是会发现打开会有10s多的延迟，继续优化。
继续修改vp_duration方法 
```
static double vp_duration(VideoState *is, Frame *vp, Frame *nextvp) {
   /* if (vp->serial == nextvp->serial) {
        double duration = nextvp->pts - vp->pts;
        if (isnan(duration) || duration <= 0 || duration > is->max_frame_duration)
            return vp->duration;
        else
            return duration;
    } else {
        return 0.0;
    }*/
	return vp->duration;
}
```

修改ffplay_video_thread方法

```
static int ffplay_video_thread(void *arg)
{
    FFPlayer *ffp = arg;
    VideoState *is = ffp->is;
    AVFrame *frame = av_frame_alloc();
    double pts;
    double duration;
    int ret;
    AVRational tb = is->video_st->time_base;
	
    //AVRational frame_rate = av_guess_frame_rate(is->ic, is->video_st, NULL);
    int64_t dst_pts = -1;
    int64_t last_dst_pts = -1;
    int retry_convert_image = 0;
    int convert_frame_count = 0;

#if CONFIG_AVFILTER
    AVFilterGraph *graph = avfilter_graph_alloc();
    AVFilterContext *filt_out = NULL, *filt_in = NULL;
    int last_w = 0;
    int last_h = 0;
    enum AVPixelFormat last_format = -2;
    int last_serial = -1;
    int last_vfilter_idx = 0;
    if (!graph) {
        av_frame_free(&frame);
        return AVERROR(ENOMEM);
    }

#else
    ffp_notify_msg2(ffp, FFP_MSG_VIDEO_ROTATION_CHANGED, ffp_get_video_rotate_degrees(ffp));
#endif

    if (!frame) {
#if CONFIG_AVFILTER
        avfilter_graph_free(&graph);
#endif
        return AVERROR(ENOMEM);
    }

    for (;;) {
        ret = get_video_frame(ffp, frame);
        if (ret < 0)
            goto the_end;
        if (!ret)
            continue;

        if (ffp->get_frame_mode) {
            if (!ffp->get_img_info || ffp->get_img_info->count <= 0) {
                av_frame_unref(frame);
                continue;
            }

            last_dst_pts = dst_pts;

            if (dst_pts < 0) {
                dst_pts = ffp->get_img_info->start_time;
            } else {
                dst_pts += (ffp->get_img_info->end_time - ffp->get_img_info->start_time) / (ffp->get_img_info->num - 1);
            }

            pts = (frame->pts == AV_NOPTS_VALUE) ? NAN : frame->pts * av_q2d(tb);
            pts = pts * 1000;
            if (pts >= dst_pts) {
                while (retry_convert_image <= MAX_RETRY_CONVERT_IMAGE) {
                    ret = convert_image(ffp, frame, (int64_t)pts, frame->width, frame->height);
                    if (!ret) {
                        convert_frame_count++;
                        break;
                    }
                    retry_convert_image++;
                    av_log(NULL, AV_LOG_ERROR, "convert image error retry_convert_image = %d\n", retry_convert_image);
                }

                retry_convert_image = 0;
                if (ret || ffp->get_img_info->count <= 0) {
                    if (ret) {
                        av_log(NULL, AV_LOG_ERROR, "convert image abort ret = %d\n", ret);
                        ffp_notify_msg3(ffp, FFP_MSG_GET_IMG_STATE, 0, ret);
                    } else {
                        av_log(NULL, AV_LOG_INFO, "convert image complete convert_frame_count = %d\n", convert_frame_count);
                    }
                    goto the_end;
                }
            } else {
                dst_pts = last_dst_pts;
            }
            av_frame_unref(frame);
            continue;
        }

#if CONFIG_AVFILTER
        if (   last_w != frame->width
            || last_h != frame->height
            || last_format != frame->format
            || last_serial != is->viddec.pkt_serial
            || ffp->vf_changed
            || last_vfilter_idx != is->vfilter_idx) {
            SDL_LockMutex(ffp->vf_mutex);
            ffp->vf_changed = 0;
            av_log(NULL, AV_LOG_DEBUG,
                   "Video frame changed from size:%dx%d format:%s serial:%d to size:%dx%d format:%s serial:%d\n",
                   last_w, last_h,
                   (const char *)av_x_if_null(av_get_pix_fmt_name(last_format), "none"), last_serial,
                   frame->width, frame->height,
                   (const char *)av_x_if_null(av_get_pix_fmt_name(frame->format), "none"), is->viddec.pkt_serial);
            avfilter_graph_free(&graph);
            graph = avfilter_graph_alloc();
            if ((ret = configure_video_filters(ffp, graph, is, ffp->vfilters_list ? ffp->vfilters_list[is->vfilter_idx] : NULL, frame)) < 0) {
                // FIXME: post error
                SDL_UnlockMutex(ffp->vf_mutex);
                goto the_end;
            }
            filt_in  = is->in_video_filter;
            filt_out = is->out_video_filter;
            last_w = frame->width;
            last_h = frame->height;
            last_format = frame->format;
            last_serial = is->viddec.pkt_serial;
            last_vfilter_idx = is->vfilter_idx;
            //frame_rate = av_buffersink_get_frame_rate(filt_out);
            SDL_UnlockMutex(ffp->vf_mutex);
        }

        ret = av_buffersrc_add_frame(filt_in, frame);
        if (ret < 0)
            goto the_end;

        while (ret >= 0) {
            is->frame_last_returned_time = av_gettime_relative() / 1000000.0;

            ret = av_buffersink_get_frame_flags(filt_out, frame, 0);
            if (ret < 0) {
                if (ret == AVERROR_EOF)
                    is->viddec.finished = is->viddec.pkt_serial;
                ret = 0;
                break;
            }

            is->frame_last_filter_delay = av_gettime_relative() / 1000000.0 - is->frame_last_returned_time;
            if (fabs(is->frame_last_filter_delay) > AV_NOSYNC_THRESHOLD / 10.0)
                is->frame_last_filter_delay = 0;
            tb = av_buffersink_get_time_base(filt_out);
#endif
            //duration = (frame_rate.num && frame_rate.den ? av_q2d((AVRational){frame_rate.den, frame_rate.num}) : 0);
			duration = 0.01;
            pts = (frame->pts == AV_NOPTS_VALUE) ? NAN : frame->pts * av_q2d(tb);
            ret = queue_picture(ffp, frame, pts, duration, frame->pkt_pos, is->viddec.pkt_serial);
            av_frame_unref(frame);
#if CONFIG_AVFILTER
        }
#endif

        if (ret < 0)
            goto the_end;
    }
 the_end:
#if CONFIG_AVFILTER
    avfilter_graph_free(&graph);
#endif
    av_log(NULL, AV_LOG_INFO, "convert image convert_frame_count = %d\n", convert_frame_count);
    av_frame_free(&frame);
	
    return 0;
}
```

