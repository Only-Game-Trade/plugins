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


import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class OpenGLRenderer implements Runnable {
    private static final String LOG_TAG = "OpenGLRenderer";
    private static final int OPENGL_VERSION = 2;
    private static final int TARGET_FPS = 60;
    protected final SurfaceTexture texture;
    private EGL10 egl;
    private EGLDisplay eglDisplay;
    private EGLContext eglContext;
    private EGLSurface eglSurface;

    private boolean running;

    private boolean signalCreated;
    private boolean signalChanged;
    private boolean readyToDraw;
    private int mWidth;
    private int mHeight;

    private GLSurfaceView.Renderer renderer;

    public OpenGLRenderer(SurfaceTexture texture) {
        this.texture = texture;
        this.running = true;
        this.signalCreated = false;
        this.signalChanged = false;
        this.readyToDraw = false;
        Thread thread = new Thread(this);
        thread.start();
    }

    public void setRenderer(GLSurfaceView.Renderer renderer) {
        this.renderer = renderer;
    }

    public void surfaceCreated() {
        signalCreated =true;
    }

    public void surfaceChanged(int width, int height) {
        signalChanged = true;
        mWidth = width;
        mHeight = height;
    }

    public void surfaceDestroyed() {
        running = false;
    }

    @Override
    public void run() {
        initGL();
        while (running) {
            long loopStart = System.currentTimeMillis();

            if(signalCreated) {
                renderer.onSurfaceCreated(null,null);
                signalCreated = false;
            }

            if(signalChanged) {
                texture.setDefaultBufferSize(mWidth,mHeight);
                renderer.onSurfaceChanged(null, mWidth, mHeight);
                signalChanged = true;
                readyToDraw = true;
            }

            if(readyToDraw && renderer!=null) {
                renderer.onDrawFrame(null);
                if (!egl.eglSwapBuffers(eglDisplay, eglSurface)) {
                    Log.d(LOG_TAG, String.valueOf(egl.eglGetError()));
                }
            }


            long waitDelta = 1000/TARGET_FPS - (System.currentTimeMillis() - loopStart);
            if (waitDelta > 0) {
                try {
                    Thread.sleep(waitDelta);
                } catch (InterruptedException e) {

                }
            }
        }
        deinitGL();
    }

    private void initGL() {
        egl = (EGL10) EGLContext.getEGL();
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        int[] version = new int[2];
        if (!egl.eglInitialize(eglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed");
        }

        EGLConfig eglConfig = chooseEglConfig();
        eglContext = createContext(egl, eglDisplay, eglConfig);

        eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, texture, null);

        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            throw new RuntimeException("GL Error: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        }

        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("GL make current error: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        }
        Log.d(LOG_TAG, "OpenGL init OK.");
    }

    private void deinitGL() {
        egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(eglDisplay, eglSurface);
        egl.eglDestroyContext(eglDisplay, eglContext);
        egl.eglTerminate(eglDisplay);
        Log.d(LOG_TAG, "OpenGL deinit OK.");
    }

    private EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        int[] attribList = {EGL_CONTEXT_CLIENT_VERSION, OPENGL_VERSION, EGL10.EGL_NONE};
        return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attribList);
    }

    private EGLConfig chooseEglConfig() {
        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = getConfig();

        if (!egl.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("Failed to choose config: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        } else if (configsCount[0] > 0) {
            return configs[0];
        }

        return null;
    }

    private int[] getConfig() {
        return new int[]{
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_SAMPLE_BUFFERS, 1,
                EGL10.EGL_SAMPLES, 4,
                EGL10.EGL_NONE
        };
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        running = false;
    }

}