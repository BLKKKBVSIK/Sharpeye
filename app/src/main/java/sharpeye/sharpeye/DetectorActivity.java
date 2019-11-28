package sharpeye.sharpeye;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.*;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;

import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Size;
import android.widget.Toast;

import sharpeye.sharpeye.Detection.Detector;
import sharpeye.sharpeye.data.BooleanKeyValueDBHelper;
import sharpeye.sharpeye.customview.OverlayView;
import sharpeye.sharpeye.data.SharedPreferencesHelper;
import sharpeye.sharpeye.popups.BatteryPopupHandler;
import sharpeye.sharpeye.popups.PopupHandler;
import sharpeye.sharpeye.processors.HeadUpSignProcessor;
import sharpeye.sharpeye.utils.Logger;
import sharpeye.sharpeye.tflite.Classifier;
import sharpeye.sharpeye.tracking.MultiBoxTracker;

import java.util.List;


public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private long timestamp = 0;

    private MultiBoxTracker multiBoxTracker;

    protected boolean computingDetection = false;

    private BatteryPopupHandler batteryPopupHandler;


    private Detector detector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);

        batteryPopupHandler = new BatteryPopupHandler(getApplicationContext(), this);
        batteryPopupHandler.Start();
        BooleanKeyValueDBHelper kvDatabase = new BooleanKeyValueDBHelper(this);
        PopupHandler starting = new PopupHandler(this,
                getApplicationContext().getString(R.string.starting_popup), kvDatabase);
        starting.NextPopup(0);
        detector = new Detector(this);
        detector.restore(savedInstanceState);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == HeadUpSignProcessor.CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            if (!Settings.canDrawOverlays(this)) {
                SharedPreferencesHelper.INSTANCE.setSharedPreferencesBoolean(getApplicationContext(),"sign_bubble",false);
                Toast.makeText(this,
                        getString(R.string.sign_bubble_toast),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        detector.save(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        detector.resume(getApplicationContext());
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        detector.pause();
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        batteryPopupHandler.Stop();
        detector.destroy();
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {

        multiBoxTracker = new MultiBoxTracker(this);
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        detector.onPreviewSizeChosen(getApplicationContext(), size, rotation, getScreenOrientation());
        trackingOverlay = findViewById(R.id.tracking_overlay);
        final int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            trackingOverlay.setAspectRatio(previewWidth, previewHeight);
        } else {
            trackingOverlay.setAspectRatio(previewHeight, previewWidth);
        }
        trackingOverlay.addCallback(
                canvas -> {
                    multiBoxTracker.draw(canvas);
                    if (isDebug()) {
                        multiBoxTracker.drawDebug(canvas);
                    }
                });
        int sensorOrientation = rotation - getScreenOrientation();
        multiBoxTracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    OverlayView trackingOverlay;

    @Override
    protected void processImage() {

        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");
        int[] rgbBytes = getRgbBytes();
        readyForNextImage();
        detector.detect(getApplicationContext(), rgbBytes, new Detector.DetectorListener() {
            @Override
            public void detectionCallback(long processingTime, Bitmap cropBitmap, List<Classifier.Recognition> recognitionList) {
                multiBoxTracker.trackResults(recognitionList, currTimestamp);
                runOnUiThread(
                        () -> {
                            showFrameInfo(previewWidth + "x" + previewHeight);
                            showCropInfo(cropBitmap.getWidth() + "x" + cropBitmap.getHeight());
                            showInference(processingTime + "ms");
                        });
            }
        });
        trackingOverlay.postInvalidate();

        computingDetection = false;

    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(new Runnable() {
            @Override
            public void run() {
                if (detector != null) {
                    detector.setNumThread(numThreads);
                }
            }
        });
    }
}