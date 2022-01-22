/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.flutter.plugins.videoplayer.ext360;

import static io.flutter.plugins.videoplayer.ext360.Utils.checkGlError;

import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.AnyThread;
import androidx.annotation.BinderThread;
import androidx.annotation.UiThread;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Video360Renderer implements GLSurfaceView.Renderer {
    private final String TAG = "Video360Renderer";

    private Mesh displayMesh;
    private Mesh requestedDisplayMesh;
    private SurfaceTexture displayTexture;
    private int displayTexId;
    private final AtomicBoolean frameAvailable = new AtomicBoolean();

    // Arbitrary vertical field of view. Adjust as desired.
    private static final int FIELD_OF_VIEW_DEGREES = 90;
    private static final float Z_NEAR = .1f;
    private static final float Z_FAR = 100;
    private final float[] projectionMatrix = new float[16];

    // There is no model matrix for this scene so viewProjectionMatrix is used for the mvpMatrix.
    private final float[] viewProjectionMatrix = new float[16];

    // Device orientation is derived from sensor data. This is accessed in the sensor's thread and
    // the GL thread.
    private final float[] deviceOrientationMatrix = new float[16];

    // Optional pitch and yaw rotations are applied to the sensor orientation. These are accessed on
    // the UI, sensor and GL Threads.
    private final float[] touchPitchMatrix = new float[16];
    private final float[] touchYawMatrix = new float[16];
    private float touchPitch;
    private float deviceRoll;

    // viewMatrix = touchPitch * deviceOrientation * touchYaw.
    private final float[] viewMatrix = new float[16];
    private final float[] tempMatrix = new float[16];

    public Video360Renderer(int displayTexId) {
        this.displayTexId = displayTexId;
        this.displayTexture = new SurfaceTexture(displayTexId);
        Matrix.setIdentityM(deviceOrientationMatrix, 0);
        Matrix.setIdentityM(touchPitchMatrix, 0);
        Matrix.setIdentityM(touchYawMatrix, 0);
    }

    @AnyThread
    public synchronized Surface createSurface() {
        return new Surface(displayTexture);
    }

    @AnyThread
    public synchronized void configureSurface(Mesh mesh) {
        if (displayTexture == null) {
            Log.e(TAG, ".createDisplay called before GL Initialization completed.");
        }
        requestedDisplayMesh = mesh;
    }

    /**
     * Configures any late-initialized components.
     *
     * <p>Since the creation of the Mesh can depend on disk access, this configuration needs to run
     * during each drawFrame to determine if the Mesh is ready yet. This also supports replacing an
     * existing mesh while the app is running.
     *
     * @return true if the scene is ready to be drawn
     */
    private synchronized boolean glConfigureScene() {
        if (displayMesh == null && requestedDisplayMesh == null) {
            // The scene isn't ready and we don't have enough information to configure it.
            return false;
        }

        // The scene is ready and we don't need to change it so we can glDraw it.
        if (requestedDisplayMesh == null) {
            return true;
        }

        // Configure or reconfigure the scene.
        if (displayMesh != null) {
            // Reconfiguration.
            displayMesh.glShutdown();
        }

        displayMesh = requestedDisplayMesh;
        requestedDisplayMesh = null;
        displayMesh.glInit(displayTexId);

        return true;
    }


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        checkGlError();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        checkGlError();

        // When the video decodes a new frame, tell the GL thread to update the image.
        displayTexture.setOnFrameAvailableListener(surfaceTexture -> frameAvailable.set(true));
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Matrix.perspectiveM(projectionMatrix, 0, FIELD_OF_VIEW_DEGREES, width/height, Z_NEAR, Z_FAR);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        // Combine touch & sensor data.
        // Orientation = pitch * sensor * yaw since that is closest to what most users expect the
        // behavior to be.
        synchronized (this) {
            Matrix.multiplyMM(tempMatrix, 0, deviceOrientationMatrix, 0, touchYawMatrix, 0);
            Matrix.multiplyMM(viewMatrix, 0, touchPitchMatrix, 0, tempMatrix, 0);
        }

        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        // From SceneRenderer glDrawFrame
        if (!glConfigureScene()) {
            // displayMesh isn't ready.
            return;
        }

        // glClear isn't strictly necessary when rendering fully spherical panoramas, but it can improve
        // performance on tiled renderers by causing the GPU to discard previous data.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        checkGlError();

        // The uiQuad uses alpha.
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

        if (frameAvailable.compareAndSet(true, false)) {
            displayTexture.updateTexImage();
            checkGlError();
        }

        displayMesh.glDraw(viewProjectionMatrix);
    }

    public void glShutdown() {
        if (displayMesh != null) {
            displayMesh.glShutdown();
        }
    }


    /**
     * Adjusts the GL camera's rotation based on device rotation. Runs on the sensor thread.
     */
    @BinderThread
    public synchronized void setDeviceOrientation(float[] matrix, float deviceRoll) {
        System.arraycopy(matrix, 0, deviceOrientationMatrix, 0, deviceOrientationMatrix.length);
        this.deviceRoll = -deviceRoll;
        updatePitchMatrix();
    }

    /**
     * Adjusts the GL camera's rotation based on device rotation. Runs on the sensor thread.
     */
    @BinderThread
    public synchronized void setRollOffset(float deviceRoll) {
        this.deviceRoll = -deviceRoll;
        updatePitchMatrix();
    }

    /**
     * Updates the pitch matrix after a physical rotation or touch input. The pitch matrix rotation
     * is applied on an axis that is dependent on device rotation so this must be called after
     * either touch or sensor update.
     */
    @AnyThread
    private void updatePitchMatrix() {
        // The camera's pitch needs to be rotated along an axis that is parallel to the real world's
        // horizon. This is the <1, 0, 0> axis after compensating for the device's roll.
        Matrix.setRotateM(touchPitchMatrix, 0,
                -touchPitch, (float) Math.cos(deviceRoll), (float) Math.sin(deviceRoll), 0);
    }

    /**
     * Set the pitch offset matrix.
     */
    @UiThread
    public synchronized void setPitchOffset(float pitchDegrees) {
        touchPitch = pitchDegrees;
        updatePitchMatrix();
    }

    /**
     * Set the yaw offset matrix.
     */
    @UiThread
    public synchronized void setYawOffset(float yawDegrees) {
        Matrix.setRotateM(touchYawMatrix, 0, -yawDegrees, 0, 1, 0);
    }

}

