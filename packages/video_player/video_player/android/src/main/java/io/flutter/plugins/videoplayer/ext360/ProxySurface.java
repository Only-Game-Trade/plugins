package io.flutter.plugins.videoplayer.ext360;

import android.graphics.SurfaceTexture;
import android.view.Surface;

public class ProxySurface extends OpenGLRenderer {

    private final String TAG = "ProxySurface";
    private Video360Renderer renderer;
    private int displayTexId;
    private int width = 1;
    private int height = 1;
    private int format = 0;
    private boolean isEnable3D= false;

    public ProxySurface(SurfaceTexture texture) {
        super(texture);
        displayTexId = Utils.glCreateExternalTexture();
        renderer = new Video360Renderer(displayTexId);
        Utils.checkGlError();
        setRenderer(renderer);
        surfaceCreated();
        surfaceChanged(width, height);
        setMediaFormat(format);
    }

    public void setResolution(int width, int height) {
        this.width = width;
        this.height = height;
        int surfaceWidth = isEnable3D ? Math.min(width, height) : width;
        int surfaceHeight = isEnable3D ? Math.min(width, height) : height;
        surfaceChanged(surfaceWidth, surfaceHeight);
    }

    public void setMediaFormat(int format) {
        this.format = format;
        isEnable3D = format >> 3 == 1;
        Mesh mesh;
        if (isEnable3D) {
            int sphericalType = (format & 0x2) >> 1;
            int mediaType = (format & 0x1) + ((format & 0x4) >> 2);
            mesh = Sphere.createUvSphere(
                    50,
                    50,
                    50,
                    180,
                    sphericalType == 0 ? 180 : 360,
                    mediaType);

            surfaceChanged(Math.min(width, height), Math.min(width, height));
        } else {
            mesh = CanvasQuad.createCanvasQuad();
            surfaceChanged(width, height);
        }
        renderer.configureSurface(mesh);

    }

    public Surface createSurface() {
        return renderer.createSurface();
    }

    public void setCameraRotation(float roll, float pitch,float yaw) {
        if(isEnable3D) {
            renderer.setRollOffset(roll);
            renderer.setPitchOffset(-pitch);
            renderer.setYawOffset(yaw);
        }
    }

    public void destroy() {
        surfaceDestroyed();
        if (renderer != null)
            renderer.glShutdown();
    }

}

