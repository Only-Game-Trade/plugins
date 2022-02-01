package io.flutter.plugins.camera.media;

import android.annotation.SuppressLint;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.AudioRouting;
import android.media.EncoderProfiles;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.MicrophoneInfo;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public class AudioEncoder implements Runnable, IAudioEncoder {
    private final String TAG = "AudioEncoder";
    private final boolean VERBOSE = true;
    private static final long kTimeoutUs = 2500;
    private CountDownLatch mLatch;

    private MediaCodec.BufferInfo mBufferInfo;
    private AudioRecord mAudioRecord;
    private MediaCodec mEncoder;
    private AVMediaMuxer mMuxer;

    private Thread mAudioThread;
    private State mState = State.Unknown;
    private Exception mPreparedException;
    private int mAudioSizeInBytes;
    private int mAudioFrameIndex;
    private int mBitRate;
    private long mMaxFileSizeInBytes = 0;

    private int mSamplingRate = 44100;
    private int mChannelCount = 1;
    private int mAudioSource;

    //#region Unsupported

    @Override
    public void registerAudioRecordingCallback(@NonNull Executor executor, @NonNull AudioManager.AudioRecordingCallback cb) {
        throw new NotImplementedError();
    }

    @Override
    public void unregisterAudioRecordingCallback(@NonNull AudioManager.AudioRecordingCallback cb) {
        throw new NotImplementedError();
    }

    @Override
    public void setPrivacySensitive(boolean privacySensitive) {
        throw new NotImplementedError();
    }

    @Override
    public boolean isPrivacySensitive() {
        throw new NotImplementedError();
    }

    @Override
    public int getMaxAmplitude() throws IllegalStateException {
        throw new NotImplementedError();
    }

    @Override
    public void addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener, Handler handler) {
        throw new NotImplementedError();
    }

    @Override
    public void removeOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener) {
        throw new NotImplementedError();
    }


    //#endregion

    @Override
    public void setAudioSource(int audioSource) throws IllegalStateException {
        mAudioSource = audioSource;
        reconfigureAudioRecord();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void setAudioProfile(@NonNull EncoderProfiles.AudioProfile profile) {
        setAudioEncodingBitRate(profile.getBitrate());
        setAudioChannels(profile.getChannels());
        setAudioSamplingRate(profile.getSampleRate());
        setAudioEncoder(profile.getCodec());
    }

    @Override
    public void setAudioEncoder(int audioEncoder) throws IllegalStateException {
        if (audioEncoder != MediaRecorder.AudioEncoder.AAC) {
            throw new NotImplementedError();
        }
    }

    @Override
    public void setAudioSamplingRate(int samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Audio sampling rate is not positive");
        }
        mSamplingRate = samplingRate;
        reconfigureAudioRecord();
    }

    @Override
    public void setAudioChannels(int numChannels) {
        if (numChannels <= 0) {
            throw new IllegalArgumentException("Number of channels is not positive");
        }
        mChannelCount = numChannels;
        reconfigureAudioRecord();
    }

    @Override
    public void setAudioEncodingBitRate(int bitRate) {
        if (bitRate <= 0) {
            throw new IllegalArgumentException("Audio encoding bit rate is not positive");
        }
        mBitRate = bitRate;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean setPreferredDevice(AudioDeviceInfo deviceInfo) {
        if (mAudioRecord == null) throw new IllegalStateException();
        return mAudioRecord.setPreferredDevice(deviceInfo);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public AudioDeviceInfo getPreferredDevice() {
        if (mAudioRecord == null) throw new IllegalStateException();
        return mAudioRecord.getPreferredDevice();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public AudioDeviceInfo getRoutedDevice() {
        if (mAudioRecord == null) throw new IllegalStateException();
        return mAudioRecord.getRoutedDevice();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public List<MicrophoneInfo> getActiveMicrophones() throws IOException {
        if (mAudioRecord == null) throw new IllegalStateException();
        return mAudioRecord.getActiveMicrophones();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public boolean setPreferredMicrophoneDirection(int direction) {
        if (mAudioRecord == null) throw new IllegalStateException();
        return mAudioRecord.setPreferredMicrophoneDirection(direction);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public boolean setPreferredMicrophoneFieldDimension(float zoom) {
        if (mAudioRecord == null) throw new IllegalStateException();
        if (zoom >= -1 && zoom <= 1) {
            throw new IllegalArgumentException("Argument must fall between -1 & 1 (inclusive)");
        }
        return mAudioRecord.setPreferredMicrophoneFieldDimension(zoom);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @NonNull
    @Override
    public AudioRecordingConfiguration getActiveRecordingConfiguration() {
        if (mAudioRecord == null) throw new IllegalStateException();
        return mAudioRecord.getActiveRecordingConfiguration();
    }

    @Override
    public void setMaxFileSize(long maxFileSizeInBytes) throws IllegalArgumentException {
        mMaxFileSizeInBytes = maxFileSizeInBytes;
    }

    @Override
    public void prepare(AVMediaMuxer muxer) throws IOException, IllegalStateException {
        mMuxer = muxer;

        mLatch = new CountDownLatch(1);

        if (mAudioThread != null && mAudioThread.isAlive()) {
            mAudioThread.interrupt();
        }

        mAudioThread = new Thread(this);
        mAudioThread.setPriority(Thread.MAX_PRIORITY);
        mAudioThread.setName(TAG);
        mAudioThread.start();

        // wait for till finish
        try {
            mLatch.await();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        if (mPreparedException != null) {
            if (mPreparedException instanceof IOException) {
                throw (IOException) mPreparedException;
            } else if (mPreparedException instanceof IllegalStateException) {
                throw (IllegalStateException) mPreparedException;
            } else {
                throw new RuntimeException(mPreparedException);
            }
        }

        mState = State.Ready;
    }

    @Override
    public void start() throws IllegalStateException {
        mAudioFrameIndex = 0;
        mState = State.Record;
    }

    @Override
    public void stop() throws IllegalStateException {
        Log.d(TAG, "Stopping Audio Encoder");
        if (mAudioThread != null && mAudioThread.isAlive()) {
            mLatch = new CountDownLatch(1);
        }
        mState = State.Stop;
    }

    @Override
    public void pause() throws IllegalStateException {
        mState = State.Pause;
    }

    @Override
    public void resume() throws IllegalStateException {
        mState = State.Record;
    }

    @Override
    public void reset() {
        stop();
        release();

        if (mAudioRecord != null) throw new IllegalStateException();
        if (mEncoder != null) throw new IllegalStateException();
        if (mMuxer != null) throw new IllegalStateException();
        if (mAudioThread != null) throw new IllegalStateException();
        if (mPreparedException != null) throw new IllegalStateException();

        // reset
        mState = State.Unknown;
        mAudioSizeInBytes = 0;
        mAudioFrameIndex = 0;
        mBitRate = 0;
        mMaxFileSizeInBytes = 0;
        mSamplingRate = 44100;
        mChannelCount = 1;
        mAudioSource = 0;
    }

    @Override
    public void release() {
        // You must call stop before release
        if (mState != State.Stop) {
            throw new IllegalStateException();
        }

        // wait till fully stopped
        if (mLatch != null) {
            try {
                Log.d(TAG, "Waiting for thread to be fully shutdown!");
                mLatch.await();
                mLatch = null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        synchronized (this) {
            releaseEncoder();
        }
    }

    @SuppressLint("MissingPermission")
    private synchronized void reconfigureAudioRecord() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }

        int channel = AudioFormat.CHANNEL_IN_MONO;
        if (mChannelCount > 1) {
            channel = AudioFormat.CHANNEL_IN_STEREO;
        }
        mAudioSizeInBytes = AudioRecord.getMinBufferSize(mSamplingRate, channel, AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecord = new AudioRecord(mAudioSource, mSamplingRate, channel, AudioFormat.ENCODING_PCM_16BIT, mAudioSizeInBytes);
    }

    private void prepareEncoder() throws IOException {
        if (mAudioRecord == null) throw new IllegalStateException();
        mAudioRecord.startRecording();

        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, mMaxFileSizeInBytes);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSamplingRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        mEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
    }

    void releaseEncoder() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }


    void awaitNewAudio() {
        if (VERBOSE) Log.d(TAG, "awaitNewAudio()");
        ByteBuffer[] encoderInputBuffers = mEncoder.getInputBuffers();
        int encoderStatus = mEncoder.dequeueInputBuffer(kTimeoutUs);
        ByteBuffer buffer = encoderInputBuffers[encoderStatus];
        buffer.clear();
        int size = mAudioRecord.read(buffer, mAudioSizeInBytes);
        long durationTimeUs = (1_000_000L * (size / mChannelCount)) / mSamplingRate / 2;
        long audioTimestamp = (mAudioFrameIndex++ * durationTimeUs);
        mEncoder.queueInputBuffer(encoderStatus, 0, size, audioTimestamp, 0);
        if (VERBOSE) {
            Log.d(TAG, "queued " + size + " bytes of input data with pts " + audioTimestamp / 1000000f);
        }
    }

    void drainEncoder(boolean endOfStream) {
        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            // signal end of stream
            int encoderStatus = mEncoder.dequeueInputBuffer(kTimeoutUs);
            long durationTimeUs = (1_000_000L * (mAudioSizeInBytes / mChannelCount)) / mSamplingRate / 2;
            long audioTimestamp = (mAudioFrameIndex++ * durationTimeUs);
            mEncoder.queueInputBuffer(encoderStatus, 0, 0, audioTimestamp, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, kTimeoutUs);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break; // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxer.isStarted()) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);
                mMuxer.setAudioMediaFormat(newFormat);
                mMuxer.start();
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxer.isStarted()) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxer.writeAudioSampleData(encodedData, mBufferInfo);
                    if (VERBOSE) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }

    @Override
    public void run() {
        // prepare
        mPreparedException = null;
        try {
            prepareEncoder();
        } catch (Exception ex) {
            mPreparedException = ex;
        } finally {
            mLatch.countDown();
        }

        try {
            while (true) {
                boolean requestExit = Thread.interrupted();
                switch (mState) {
                    case Record:
                        drainEncoder(false);
                        awaitNewAudio();
                        break;
                    case Pause:
                        drainEncoder(false);
                        break;
                    case Stop:
                        requestExit = true;
                        break;
                    default:
                        break;
                }
                if (requestExit) {
                    Log.d(TAG, "Exiting thread...");
                    break;
                }
            }
        } finally {
            drainEncoder(true);
            releaseEncoder();
            mLatch.countDown();
            mAudioThread = null;
            Log.d(TAG, "Shutting down...");
        }
    }
}

