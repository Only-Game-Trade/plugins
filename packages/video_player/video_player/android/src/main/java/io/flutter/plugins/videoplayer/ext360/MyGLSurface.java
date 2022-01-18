package io.flutter.plugins.videoplayer.ext360;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.Surface;

import java.nio.IntBuffer;

public class MyGLSurface extends OpenGLRenderer {

    private final String TAG = "MyGLSurface";
    private Video360Renderer renderer;
    private int displayTexId;


    public MyGLSurface(SurfaceTexture texture) {
        super(texture);
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);

        displayTexId = Utils.glCreateExternalTexture();
        renderer = new Video360Renderer(displayTexId);
        Utils.checkGlError();

        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // initial surface
        surfaceCreated(null);
        surfaceChanged(null, 0, 1, 1);
    }

    public Surface createSphericalSurface(int mediaFormat, int sphericalType) {
        return renderer.createDisplay(Mesh.createUvSphere(
                50,
                50,
                50,
                180,
                sphericalType == 0 ? 180 : 360,
                mediaFormat));
    }

    public void setRollOffset(float delta) {
        renderer.setRollOffset(delta);
    }

    public void setPitchOffset(float delta) {
        renderer.setPitchOffset(delta);
    }

    public void setYawOffset(float delta) {
        renderer.setYawOffset(delta);
    }

    public void destroy() {
        if (renderer != null)
            renderer.glShutdown();
    }

}

