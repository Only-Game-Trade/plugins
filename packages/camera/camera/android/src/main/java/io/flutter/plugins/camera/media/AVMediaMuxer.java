package io.flutter.plugins.camera.media;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AVMediaMuxer {
    private final String TAG = "AVMediaMuxer";
    private final boolean ENABLE_SHADOW_FILE = true;
    private MediaMuxer mMediaMuxer;
    private MediaMuxer mMediaMuxerAudio;
    private MediaMuxer mMediaMuxerVideo;
    private int mVideoTrackIndex;
    private int mAudioTrackIndex;
    private int mTotalTracks;
    private int mStarted;
    private int mStopped;
    private int mReleased;
    private int mDegrees;

    public AVMediaMuxer(int totalTracks, int outputFormat, String output) throws IOException {
        mTotalTracks = totalTracks;
        mVideoTrackIndex = -1;
        mAudioTrackIndex = -1;
        mMediaMuxer = new MediaMuxer(output, outputFormat);
        if (ENABLE_SHADOW_FILE) {
            File fp = new File(output);
            String name = fp.getName();
            String nameOnly = name.indexOf(".") > 0 ? name.substring(0, name.lastIndexOf(".")) : name;
            String videoOut = fp.getCanonicalPath().replace(name, nameOnly + "_video.m4v");
            String audioOut = fp.getCanonicalPath().replace(name, nameOnly + "_audio.m4a");
            mMediaMuxerVideo = new MediaMuxer(videoOut, outputFormat);
            mMediaMuxerAudio = new MediaMuxer(audioOut, outputFormat);
        }
        mStarted = 0;
        mStopped = 0;
        mReleased = 0;
    }

    public synchronized boolean isStarted() {
        return mStarted == mTotalTracks;
    }

    public synchronized void setOrientationHint(int degrees) {
        if (degrees != 0 &&
                degrees != 90 &&
                degrees != 180 &&
                degrees != 270) {
            throw new IllegalArgumentException("Unsupported angle: " + degrees);
        }
        mMediaMuxer.setOrientationHint(degrees);
        if (ENABLE_SHADOW_FILE) {
            mMediaMuxerVideo.setOrientationHint(degrees);
        }
        mDegrees = degrees;
    }

    public synchronized int getOrientationHint() {
        return mDegrees;
    }

    public synchronized void setLocation(float latitude, float longitude) {
        int latitudex10000 = (int) (latitude * 10000 + 0.5);
        int longitudex10000 = (int) (longitude * 10000 + 0.5);

        if (latitudex10000 > 900000 || latitudex10000 < -900000) {
            String msg = "Latitude: " + latitude + " out of range.";
            throw new IllegalArgumentException(msg);
        }
        if (longitudex10000 > 1800000 || longitudex10000 < -1800000) {
            String msg = "Longitude: " + longitude + " out of range";
            throw new IllegalArgumentException(msg);
        }
        mMediaMuxer.setLocation(latitude, longitude);
        if (ENABLE_SHADOW_FILE) {
            mMediaMuxerVideo.setLocation(latitude, longitude);
            mMediaMuxerAudio.setLocation(latitude, longitude);
        }
    }

    public synchronized void writeVideoSampleData(@NonNull ByteBuffer byteBuf, @NonNull MediaCodec.BufferInfo bufferInfo) {
        mMediaMuxer.writeSampleData(mVideoTrackIndex, byteBuf, bufferInfo);
        if (ENABLE_SHADOW_FILE) {
            mMediaMuxerVideo.writeSampleData(0, byteBuf, bufferInfo);
        }
    }

    public synchronized void writeAudioSampleData(@NonNull ByteBuffer byteBuf, @NonNull MediaCodec.BufferInfo bufferInfo) {
        mMediaMuxer.writeSampleData(mAudioTrackIndex, byteBuf, bufferInfo);
        if (ENABLE_SHADOW_FILE) {
            mMediaMuxerAudio.writeSampleData(0, byteBuf, bufferInfo);
        }
    }

    public synchronized int setAudioMediaFormat(MediaFormat newFormat) {
        if (ENABLE_SHADOW_FILE) {
            mMediaMuxerAudio.addTrack(newFormat);
        }
        mAudioTrackIndex = mMediaMuxer.addTrack(newFormat);
        return mAudioTrackIndex;
    }

    public synchronized int setVideoMediaFormat(MediaFormat newFormat) {
        if (ENABLE_SHADOW_FILE) {
            mMediaMuxerVideo.addTrack(newFormat);
        }
        mVideoTrackIndex = mMediaMuxer.addTrack(newFormat);
        return mVideoTrackIndex;
    }

    public void stop() {
        // try stop muxer
        synchronized (this) {
            mStopped++;
            if (mStopped == mTotalTracks) {
                mMediaMuxer.stop();
                if (ENABLE_SHADOW_FILE) {
                    mMediaMuxerVideo.stop();
                    mMediaMuxerAudio.stop();
                }
                Log.d(TAG, "Stopped");
            }
        }

        while (mStopped != mTotalTracks) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void release() {
        // try release muxer
        synchronized (this) {
            mReleased++;
            if (mReleased == mTotalTracks) {
                mMediaMuxer.release();
                mMediaMuxer = null;
                if (ENABLE_SHADOW_FILE) {
                    mMediaMuxerVideo.release();
                    mMediaMuxerAudio.release();
                    mMediaMuxerVideo = null;
                    mMediaMuxerAudio = null;
                }
                Log.d(TAG, "Released");
            }
        }

        while (mReleased != mTotalTracks) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void start() {
        // try start muxer
        synchronized (this) {
            mStarted++;
            if (mStarted == mTotalTracks) {
                mMediaMuxer.start();
                if (ENABLE_SHADOW_FILE) {
                    mMediaMuxerVideo.start();
                    mMediaMuxerAudio.start();
                }
                Log.d(TAG, "Started");
            }
        }

        while (mStarted != mTotalTracks) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
