package io.flutter.plugins.camera.media;

import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.media.AudioRouting;
import android.media.EncoderProfiles;
import android.media.MicrophoneInfo;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

public interface IAudioEncoder {

    void setAudioSource(int audioSource) throws IllegalStateException;

    void setPrivacySensitive(boolean privacySensitive);

    boolean isPrivacySensitive();

    void setAudioProfile(@NonNull EncoderProfiles.AudioProfile profile);

    void setAudioEncoder(int audio_encoder) throws IllegalStateException;

    void setAudioSamplingRate(int samplingRate);

    void setAudioChannels(int numChannels);

    void setAudioEncodingBitRate(int bitRate);

    int getMaxAmplitude() throws IllegalStateException;

    boolean setPreferredDevice(AudioDeviceInfo deviceInfo);

    AudioDeviceInfo getPreferredDevice();

    AudioDeviceInfo getRoutedDevice();

    void addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener, Handler handler);

    void removeOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener);

    List<MicrophoneInfo> getActiveMicrophones() throws IOException;

    boolean setPreferredMicrophoneDirection(int direction);

    boolean setPreferredMicrophoneFieldDimension(float zoom);

    void registerAudioRecordingCallback(@NonNull Executor executor, @NonNull AudioManager.AudioRecordingCallback cb);

    void unregisterAudioRecordingCallback(@NonNull AudioManager.AudioRecordingCallback cb);

    @NonNull
    AudioRecordingConfiguration getActiveRecordingConfiguration();

    void setMaxFileSize(long maxFileSizeInBytes) throws IllegalArgumentException;

    void prepare(AVMediaMuxer muxer) throws IOException, IllegalStateException;

    void start() throws IllegalStateException;

    void stop() throws IllegalStateException;

    void pause() throws IllegalStateException;

    void resume() throws IllegalStateException;

    void reset();

    void release();
}
