package io.flutter.plugins.camera.media;

import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.media.EncoderProfiles;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.IOException;

public interface IVideoEncoder {

    void setCamera(Camera c);

    Surface getSurface();

    void setInputSurface(@NonNull Surface surface);

    void setPreviewDisplay(Surface sv);

    void setVideoSource(int video_source) throws IllegalStateException;

    void setVideoProfile(@NonNull EncoderProfiles.VideoProfile profile);

    void setVideoSize(int width, int height) throws IllegalStateException;

    void setVideoFrameRate(int rate) throws IllegalStateException;

    void setVideoEncoder(int video_encoder) throws IllegalStateException;

    void setVideoEncodingBitRate(int bitRate);

    void setVideoEncodingProfileLevel(int profile, int level);

    void setMaxFileSize(long maxFileSizeInBytes) throws IllegalArgumentException;

    void prepare(AVMediaMuxer muxer) throws IOException, IllegalStateException;

    void setCaptureRate(double fps);

    void start() throws IllegalStateException;

    void stop() throws IllegalStateException;

    void pause() throws IllegalStateException;

    void resume() throws IllegalStateException;

    void reset();

    void release();
}
