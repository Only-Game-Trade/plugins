/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.flutter.plugins.camera.media;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.CamcorderProfile;
import android.media.EncoderProfiles;
import android.media.MediaRecorder;
import android.media.MicrophoneInfo;
import android.media.metrics.LogSessionId;
import android.os.Build;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

public class JMediaRecoder extends MediaRecorder {
    private final static String TAG = "MyMediaRecorder";
    private static final boolean VERBOSE = false;

    private io.flutter.plugins.camera.media.VideoEncoder mVideoEncoder;
    private io.flutter.plugins.camera.media.AudioEncoder mAudioEncoder;
    private AVMediaMuxer mMuxer;

    private boolean mEnableAudio;
    private boolean mEnableVideo;

    // Muxer properties
    private String mOutputPath;
    private int mOutputFormat;
    private float mLatitude;
    private float mLongitude;
    private int mDegrees;

    public JMediaRecoder() {
        mVideoEncoder = new io.flutter.plugins.camera.media.VideoEncoder();
        mAudioEncoder = new io.flutter.plugins.camera.media.AudioEncoder();
    }

    //#region Video Interface

    @Override
    public synchronized void setCamera(Camera c) {
        mVideoEncoder.setCamera(c);
    }

    @Override
    public synchronized Surface getSurface() {
        return mVideoEncoder.getSurface();
    }

    @Override
    public synchronized void setInputSurface(@NonNull Surface surface) {
        mVideoEncoder.setInputSurface(surface);
    }

    @Override
    public synchronized void setPreviewDisplay(Surface sv) {
        mVideoEncoder.setPreviewDisplay(sv);
    }

    @Override
    public synchronized void setVideoSource(int video_source) throws IllegalStateException {
        mVideoEncoder.setVideoSource(video_source);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public synchronized void setVideoProfile(@NonNull EncoderProfiles.VideoProfile profile) {
        mVideoEncoder.setVideoProfile(profile);
    }

    @Override
    public synchronized void setVideoSize(int width, int height) throws IllegalStateException {
        mVideoEncoder.setVideoSize(width, height);
    }

    @Override
    public synchronized void setVideoFrameRate(int rate) throws IllegalStateException {
        mVideoEncoder.setVideoFrameRate(rate);
    }

    @Override
    public synchronized void setVideoEncoder(int video_encoder) throws IllegalStateException {
        mVideoEncoder.setVideoEncoder(video_encoder);
    }

    @Override
    public synchronized void setVideoEncodingBitRate(int bitRate) {
        mVideoEncoder.setVideoEncodingBitRate(bitRate);
    }

    @Override
    public synchronized void setVideoEncodingProfileLevel(int profile, int level) {
        mVideoEncoder.setVideoEncodingProfileLevel(profile, level);
    }

    @Override
    public synchronized void setOrientationHint(int degrees) {
        mDegrees = degrees;
    }

    @Override
    public synchronized void setCaptureRate(double fps) {
        mVideoEncoder.setCaptureRate(fps);
    }

    //#endregion

    //#region Audio Interface

    @Override
    public synchronized void setAudioSource(int audioSource) throws IllegalStateException {
        mAudioEncoder.setAudioSource(audioSource);
    }

    @Override
    public synchronized void setPrivacySensitive(boolean privacySensitive) {
        mAudioEncoder.setPrivacySensitive(privacySensitive);
    }

    @Override
    public synchronized boolean isPrivacySensitive() {
        return mAudioEncoder.isPrivacySensitive();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public synchronized void setAudioProfile(@NonNull EncoderProfiles.AudioProfile profile) {
        mAudioEncoder.setAudioProfile(profile);
    }

    @Override
    public synchronized void setAudioEncoder(int audio_encoder) throws IllegalStateException {
        mAudioEncoder.setAudioEncoder(audio_encoder);
    }

    @Override
    public synchronized void setAudioSamplingRate(int samplingRate) {
        mAudioEncoder.setAudioSamplingRate(samplingRate);
    }

    @Override
    public synchronized void setAudioChannels(int numChannels) {
        mAudioEncoder.setAudioChannels(numChannels);
    }

    @Override
    public synchronized void setAudioEncodingBitRate(int bitRate) {
        mAudioEncoder.setAudioEncodingBitRate(bitRate);
    }

    @Override
    public synchronized int getMaxAmplitude() throws IllegalStateException {
        return mAudioEncoder.getMaxAmplitude();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public synchronized boolean setPreferredDevice(AudioDeviceInfo deviceInfo) {
        return mAudioEncoder.setPreferredDevice(deviceInfo);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public synchronized AudioDeviceInfo getPreferredDevice() {
        return mAudioEncoder.getPreferredDevice();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public synchronized AudioDeviceInfo getRoutedDevice() {
        return mAudioEncoder.getRoutedDevice();
    }

    @Override
    public synchronized void addOnRoutingChangedListener(OnRoutingChangedListener listener, Handler handler) {
        mAudioEncoder.addOnRoutingChangedListener(listener, handler);
    }

    @Override
    public synchronized void removeOnRoutingChangedListener(OnRoutingChangedListener listener) {
        mAudioEncoder.removeOnRoutingChangedListener(listener);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public synchronized List<MicrophoneInfo> getActiveMicrophones() throws IOException {
        return mAudioEncoder.getActiveMicrophones();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public synchronized boolean setPreferredMicrophoneDirection(int direction) {
        return mAudioEncoder.setPreferredMicrophoneDirection(direction);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public synchronized boolean setPreferredMicrophoneFieldDimension(float zoom) {
        return mAudioEncoder.setPreferredMicrophoneFieldDimension(zoom);
    }

    @Override
    public synchronized void registerAudioRecordingCallback(@NonNull Executor executor, @NonNull AudioManager.AudioRecordingCallback cb) {
        mAudioEncoder.registerAudioRecordingCallback(executor, cb);
    }

    @Override
    public synchronized void unregisterAudioRecordingCallback(@NonNull AudioManager.AudioRecordingCallback cb) {
        mAudioEncoder.unregisterAudioRecordingCallback(cb);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Nullable
    @Override
    public synchronized AudioRecordingConfiguration getActiveRecordingConfiguration() {
        return mAudioEncoder.getActiveRecordingConfiguration();
    }

    //#endregion

    @Override
    public synchronized void setProfile(CamcorderProfile profile) {
        setOutputFormat(profile.fileFormat);
        setVideoFrameRate(profile.videoFrameRate);
        setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        setVideoEncodingBitRate(profile.videoBitRate);
        setVideoEncoder(profile.videoCodec);
        if (profile.quality >= CamcorderProfile.QUALITY_TIME_LAPSE_LOW &&
                profile.quality <= CamcorderProfile.QUALITY_TIME_LAPSE_QVGA) {
            // Nothing needs to be done. Call to setCaptureRate() enables
            // time lapse video recording.
        } else {
            setAudioEncodingBitRate(profile.audioBitRate);
            setAudioChannels(profile.audioChannels);
            setAudioSamplingRate(profile.audioSampleRate);
            setAudioEncoder(profile.audioCodec);
        }
    }


    @Override
    public synchronized void setLocation(float latitude, float longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    @Override
    public synchronized void setOutputFormat(int outputFormat) throws IllegalStateException {
        mOutputFormat = outputFormat;
    }

    @Override
    public synchronized void setOutputFile(String path) throws IllegalStateException {
        mOutputPath = path;
    }

    @Override
    public synchronized void setNextOutputFile(File file) throws IOException {
        throw new NotImplementedError();
    }

    @SuppressLint("MissingPermission")
    @Override
    public synchronized void prepare() throws IOException, IllegalStateException {
        mMuxer = new AVMediaMuxer(2, mOutputFormat, mOutputPath);
        mMuxer.setLocation(mLatitude, mLongitude);
        mMuxer.setOrientationHint(mDegrees);
        mVideoEncoder.prepare(mMuxer);
        mAudioEncoder.prepare(mMuxer);
    }

    @Override
    public synchronized void start() throws IllegalStateException {
        mVideoEncoder.start();
        mAudioEncoder.start();
    }

    @Override
    public synchronized void stop() throws IllegalStateException {
        mVideoEncoder.stop();
        mAudioEncoder.stop();
    }

    @Override
    public synchronized void pause() throws IllegalStateException {
        mVideoEncoder.pause();
        mAudioEncoder.pause();
    }

    @Override
    public synchronized void resume() throws IllegalStateException {
        mVideoEncoder.resume();
        mAudioEncoder.resume();
    }

    @Override
    public synchronized void reset() {
        mVideoEncoder.reset();
        mAudioEncoder.reset();
    }

    @Override
    public synchronized void release() {
        mVideoEncoder.release();
        mAudioEncoder.release();
    }

    //#region Not Implemented Features

    @RequiresApi(api = Build.VERSION_CODES.S)
    public JMediaRecoder(@NonNull Context context) {
        throw new NotImplementedError();
    }

    @Override
    public synchronized void setLogSessionId(@NonNull LogSessionId id) {
        throw new NotImplementedError();
    }

    @NonNull
    @Override
    public synchronized LogSessionId getLogSessionId() {
        throw new NotImplementedError();
    }

    @Override
    public synchronized void setMaxDuration(int maxDurationMs) throws IllegalArgumentException {
        throw new NotImplementedError();
    }

    @Override
    public synchronized void setMaxFileSize(long maxFileSizeBytes) throws IllegalArgumentException {
        throw new NotImplementedError();
    }

    @Override
    public synchronized void setOutputFile(FileDescriptor fd) throws IllegalStateException {
        throw new NotImplementedError();
    }

    @Override
    public synchronized void setOutputFile(File file) {
        throw new NotImplementedError();
    }

    @Override
    public synchronized void setNextOutputFile(FileDescriptor fd) throws IOException {
        throw new NotImplementedError();
    }

    @Override
    public synchronized void setOnErrorListener(OnErrorListener l) {
        throw new NotImplementedError();
    }

    @Override
    public synchronized void setOnInfoListener(OnInfoListener listener) {
        throw new NotImplementedError();
    }

    @Override
    public synchronized PersistableBundle getMetrics() {
        throw new NotImplementedError();
    }

    //#endregion

}

class NotImplementedError extends RuntimeException {
    public NotImplementedError() {
        super();
    }
}

enum State {
    Unknown,
    Ready,
    Record,
    Pause,
    Stop,
}
