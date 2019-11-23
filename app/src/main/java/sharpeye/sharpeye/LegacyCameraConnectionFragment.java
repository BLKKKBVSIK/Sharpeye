package sharpeye.sharpeye;

import android.app.Fragment;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.*;
import sharpeye.sharpeye.customview.AutoFitTextureView;
import sharpeye.sharpeye.utils.ImageUtils;
import sharpeye.sharpeye.utils.Logger;

import java.io.IOException;
import java.util.List;

public class LegacyCameraConnectionFragment extends Fragment {
    private Camera camera;
    private static final Logger LOGGER = new Logger();
    private Camera.PreviewCallback imageListener;
    private Size desiredSize;

    private Size desiredPreviewSize;


    /**
     * The layout identifier to inflate for this Fragment.
     */
    private int layout;

    public LegacyCameraConnectionFragment(
            final Camera.PreviewCallback imageListener, final int layout, final Size desiredSize) {
        this.imageListener = imageListener;
        this.layout = layout;
        this.desiredSize = desiredSize;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        int x = size.x;
        int y = size.y;
        Log.e("CameraActivity", "xy="+ x +"x"+ y);
        desiredPreviewSize = new Size(((x > y) ? x : y), ((x > y) ? y : x));
    }

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private void startPreview(final SurfaceTexture texture) {
        int index = getCameraId();
        camera = Camera.open(index);

        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes != null
                    && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
            Size[] sizes = new Size[cameraSizes.size()];
            int i = 0;
            for (Camera.Size size : cameraSizes) {
                sizes[i++] = new Size(size.width, size.height);
            }
            Size previewSize =
                    CameraConnectionFragment.chooseOptimalSize(
                            sizes, desiredSize.getWidth(), desiredSize.getHeight());
            parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
            camera.setDisplayOrientation(90);
            camera.setParameters(parameters);
            camera.setPreviewTexture(texture);
        } catch (IOException exception) {
            camera.release();
        }

        camera.setPreviewCallbackWithBuffer(imageListener);
        Camera.Size s = camera.getParameters().getPreviewSize();
        camera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.height, s.width)]);

        textureView.setAspectRatio(desiredPreviewSize.getWidth(), desiredPreviewSize.getHeight());
        //textureView.setAspectRatio(s.height, s.width);

        camera.startPreview();
    }

    /**
     * {@link android.view.TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {
                    startPreview(texture);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {}
            };

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView textureView;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;

    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        textureView = view.findViewById(R.id.texture);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).

        if (textureView.isAvailable()) {

            startPreview(textureView.getSurfaceTexture());
            //camera.startPreview();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        stopCamera();
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
    }

    protected void stopCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    private int getCameraId() {
        CameraInfo ci = new CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == CameraInfo.CAMERA_FACING_BACK)
                return i;
        }
        return -1; // No camera found
    }
}
