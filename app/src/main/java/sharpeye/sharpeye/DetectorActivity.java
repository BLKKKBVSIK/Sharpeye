/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sharpeye.sharpeye;

import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import sharpeye.sharpeye.OverlayView.DrawCallback;
import sharpeye.sharpeye.env.BorderedText;
import sharpeye.sharpeye.env.ImageUtils;
import sharpeye.sharpeye.env.Logger;
import sharpeye.sharpeye.signs.SignList;
import sharpeye.sharpeye.tracking.MultiBoxTracker;
import sharpeye.sharpeye.tracking.Tracker;
import sharpeye.sharpeye.warning.Speech;
import sharpeye.sharpeye.warning.WarningEvent;

import java.io.IOException;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener, GPSCallback {
    private static final Logger LOGGER = new Logger();

    // Configuration values for the prepackaged multibox model.
    private static final int MB_INPUT_SIZE = 224;
    private static final int MB_IMAGE_MEAN = 128;
    private static final float MB_IMAGE_STD = 128;
    private static final String MB_INPUT_NAME = "ResizeBilinear";
    private static final String MB_OUTPUT_LOCATIONS_NAME = "output_locations/Reshape";
    private static final String MB_OUTPUT_SCORES_NAME = "output_scores/Reshape";
    private static final String MB_MODEL_FILE = "file:///android_asset/multibox_model.pb";
    private static final String MB_LOCATION_FILE =
            "file:///android_asset/multibox_location_priors.txt";

    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/trafficSignGeneralMobilenet.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/generalTrafficLabels.txt";

    // Configuration values for tiny-yolo-voc. Note that the graph is not included with TensorFlow and
    // must be manually placed in the assets/ directory by the user.
    // Graphs and models downloaded from http://pjreddie.com/darknet/yolo/ may be converted e.g. via
    // DarkFlow (https://github.com/thtrieu/darkflow). Sample command:
    // ./flow --model cfg/tiny-yolo-voc.cfg --load bin/tiny-yolo-voc.weights --savepb --verbalise
    private static final String YOLO_MODEL_FILE = "file:///android_asset/generalTraffic.pb";
    private static final String YOLO_LABEL_FILE = "file:///android_asset/generalTrafficLabels.txt";
    private static final int YOLO_INPUT_SIZE = 416;
    private static final String YOLO_INPUT_NAME = "input";
    private static final String YOLO_OUTPUT_NAMES = "output";
    private static final int YOLO_BLOCK_SIZE = 32;

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.  Optionally use legacy Multibox (trained using an older version of the API)
    // or YOLO.
    private enum DetectorMode {
        TF_OD_API, MULTIBOX, YOLO;
    }

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;//DetectorMode.TF_OD_API;

    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.2f;
    private static final float MINIMUM_CONFIDENCE_MULTIBOX = 0.1f;
    private static final float MINIMUM_CONFIDENCE_YOLO = 0.5f;

    private static final boolean MAINTAIN_ASPECT = (MODE == DetectorMode.YOLO);

    private static final boolean SAVE_PREVIEW_BITMAP = true;
    private static final float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;

    private Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Bitmap rgbOrientedBitmap = null;


    private long timestamp = 0;

    private Matrix rotationTransform;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker multiBoxTracker;

    private byte[] luminanceCopy;

    private BorderedText borderedText;

    long lastDetection = 0;

    SignClassifier signClassifier;
    private Tracker tracker;

    protected boolean initializedTracking = false;

    protected boolean computingDetection = false;

    private long lastRecognition = 0;

    private WarningEvent warningEvent;

    ///speed update and display
    /*private var gpsManager: GPSManager? = null
    private var locationManager: LocationManager? = null
    var isGPSEnabled: Boolean = false
    var speed = 0.toDouble()
    var currentSpeed: Double = 0.toDouble()
    var kmphSpeed:Double = 0.toDouble()
    var txtview: TextView? = null*/
    float speed;
    double kmphSpeed;
    TextView txtview = null;
    GPSManager gpsManager;
    LocationManager locationManager;
    boolean isGPSEnabled;
    ///-----------------------
    CurrentState currentState;
    SignList signList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("TRACKER")) {
            tracker = savedInstanceState.getParcelable("TRACKER");
            if (tracker == null) {
                tracker = new Tracker();
            }
        } else {
            tracker = new Tracker();
        }
        if (tracker.needInit())
            tracker.init();
            txtview = findViewById(R.id.speed);
            txtview.setVisibility(View.VISIBLE);
            currentState = new CurrentState();
            signList = new SignList(this);
            ///ne pas oublier de set la visibility à true
            try {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            getCurrentSpeed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("TRACKER", tracker);
        super.onSaveInstanceState(outState);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(getApplicationContext(),"signs_on",false))
            warningEvent = new WarningEvent(this);
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(getApplicationContext(),"speed_display",false))
            txtview.setVisibility(View.VISIBLE);
        else
            txtview.setVisibility(View.INVISIBLE);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (warningEvent != null) {
            warningEvent.clean();
            warningEvent = null;
        }
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        tracker.free();
        gpsManager.stopListening();
        gpsManager.setGPSCallback(null);
        gpsManager = null;

    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        multiBoxTracker = new MultiBoxTracker(this);

        try {
            signClassifier = new SignClassifier(getApplicationContext());
        } catch (final IOException e) {
            LOGGER.e("Exception initializing classifier!", e);
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        int cropSize = TF_OD_API_INPUT_SIZE;
        if (MODE == DetectorMode.YOLO) {
            try {
                detector =
                        TensorFlowYoloDetector.create(
                                getAssets(),
                                YOLO_MODEL_FILE,
                                YOLO_LABEL_FILE,
                                YOLO_INPUT_SIZE,
                                YOLO_INPUT_NAME,
                                YOLO_OUTPUT_NAMES,
                                YOLO_BLOCK_SIZE);
                cropSize = YOLO_INPUT_SIZE;
            } catch (final IOException e) {
                LOGGER.e("Exception initializing classifier!", e);
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }
        } else if (MODE == DetectorMode.MULTIBOX) {
            detector =
                    TensorFlowMultiBoxDetector.create(
                            getAssets(),
                            MB_MODEL_FILE,
                            MB_LOCATION_FILE,
                            MB_IMAGE_MEAN,
                            MB_IMAGE_STD,
                            MB_INPUT_NAME,
                            MB_OUTPUT_LOCATIONS_NAME,
                            MB_OUTPUT_SCORES_NAME);
            cropSize = MB_INPUT_SIZE;
        } else {
            try {
                detector = TensorFlowObjectDetectionAPIModel.create(
                        getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
                cropSize = TF_OD_API_INPUT_SIZE;
            } catch (final IOException e) {
                LOGGER.e("Exception initializing classifier!", e);
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        rgbOrientedBitmap = Bitmap.createBitmap(previewHeight, previewWidth, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);


        final boolean maintainAspectRatio = MAINTAIN_ASPECT;
        final boolean maintainAspectRatioForReal = maintainAspectRatio;
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, maintainAspectRatio, maintainAspectRatioForReal);

        rotationTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        previewHeight, previewWidth,
                        sensorOrientation, maintainAspectRatio, maintainAspectRatioForReal);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        multiBoxTracker.draw(canvas);
                        if (isDebug()) {
                            multiBoxTracker.drawDebug(canvas);
                        }
                    }
                });

        addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (!isDebug()) {
                            return;
                        }
                        final Bitmap copy = cropCopyBitmap;
                        if (copy == null) {
                            return;
                        }

                        final int backgroundColor = Color.argb(100, 0, 0, 0);
                        canvas.drawColor(backgroundColor);

                        final Matrix matrix = new Matrix();
                        final float scaleFactor = 2;
                        matrix.postScale(scaleFactor, scaleFactor);
                        matrix.postTranslate(
                                canvas.getWidth() - copy.getWidth() * scaleFactor,
                                canvas.getHeight() - copy.getHeight() * scaleFactor);
                        canvas.drawBitmap(copy, matrix, new Paint());

                        final Vector<String> lines = new Vector<String>();
                        if (detector != null) {
                            final String statString = detector.getStatString();
                            final String[] statLines = statString.split("\n");
                            for (final String line : statLines) {
                                lines.add(line);
                            }
                        }
                        lines.add("");

                        lines.add("Frame: " + previewWidth + "x" + previewHeight);
                        lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
                        lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
                        lines.add("Rotation: " + sensorOrientation);
                        lines.add("Inference time: " + lastProcessingTimeMs + "ms");

                        borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
                    }
                });
    }

    OverlayView trackingOverlay;

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        byte[] originalLuminance = getLuminance();
        multiBoxTracker.onFrame(
                previewWidth,
                previewHeight,
                getLuminanceStride(),
                sensorOrientation,
                originalLuminance,
                timestamp);
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        final Canvas canvas1 = new Canvas(croppedBitmap);
        canvas1.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        final Canvas canvas2 = new Canvas(rgbOrientedBitmap);
        canvas2.drawBitmap(rgbFrameBitmap, rotationTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        LOGGER.i("Running detection on image " + currTimestamp);
        final long startTime = SystemClock.uptimeMillis();

        //final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
        final List<Classifier.Recognition> results;
        if (!initializedTracking || (startTime - lastRecognition) >= 300) {
            results = detector.recognizeImage(croppedBitmap);
            tracker.track(croppedBitmap, results);
            initializedTracking = true;
            lastRecognition = SystemClock.uptimeMillis();
        } else {
            results = tracker.update(croppedBitmap);
        }
        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
        final Canvas canvas = new Canvas(cropCopyBitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2.0f);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        switch (MODE) {
            case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
            case MULTIBOX:
                minimumConfidence = MINIMUM_CONFIDENCE_MULTIBOX;
                break;
            case YOLO:
                minimumConfidence = MINIMUM_CONFIDENCE_YOLO;
                break;
        }

        long timeSpent = System.currentTimeMillis();
        //Classifier
        for (int i = 0; i < results.size(); ++i) {
            if (results.get(i).getConfidence() > minimumConfidence) {
                String result = signClassifier.detectSign(results.get(i), croppedBitmap, rgbOrientedBitmap, 0.6f);
                if (signClassifier.getLastResults().size() >= 1) {
                    Classifier.Recognition elem;
                    elem = new Classifier.Recognition(results.get(i).getId(), result, signClassifier.getLastResults().get(0).getConfidence(), results.get(i).getLocation());
                    elem.setOpencvID(results.get(i).getOpencvID());
                    results.add(i, elem);
                    results.remove(i + 1);
                }
            }
        }
        timeSpent = System.currentTimeMillis() - timeSpent;
        System.out.println("Time: " + timeSpent / 1000.0f);

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
                try {
                    if (warningEvent != null) {
                        warningEvent.triggerWarning(result.getTitle());
                        currentState.addSign(signList.get(result.getTitle()));
                    }
                } catch (NullPointerException ex) {
                    Log.e("Detector", "WarningEvent already cleaned");
                }
            }
        }

        multiBoxTracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
        trackingOverlay.postInvalidate();

        requestRender();
        computingDetection = false;
    }

    ///--------Speed-------------------
    public void getCurrentSpeed(){

        txtview.setText(getString(R.string.speed_counter).toString());
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        gpsManager = new GPSManager(DetectorActivity.this);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(isGPSEnabled) {
            gpsManager.startListening(this);
            gpsManager.setGPSCallback((GPSCallback) this);
        } else {
            gpsManager.showSettingsAlert();
        }
    }

    @Override
    public void onGPSUpdate(Location location) {
        speed = location.getSpeed() * 3.6f;
        currentState.setSpeed(round(speed, 3, BigDecimal.ROUND_HALF_UP));
        kmphSpeed = round((currentState.getSpeed()),3,BigDecimal.ROUND_HALF_UP);//c'est le bordel dans ma tête
        txtview.setText(kmphSpeed+"km/h");
        if (currentState.getSpeed() >= currentState.getSpeedLimit() * 1.05)
        {
            txtview.setTextColor(Color.rgb(255,0,0));
            if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(getApplicationContext(),"speed_control",false)) {
                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
            }
        }
        else if (currentState.getSpeed() > currentState.getSpeedLimit())
        {
            txtview.setTextColor(Color.rgb(255,165,0));
        }
        else
        {
            txtview.setTextColor(Color.rgb(255,255,255));
        }
    }

    public static double round(double unrounded, int precision, int roundingMode) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }
    ///-----------------


    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onSetDebug(final boolean debug) {
        detector.enableStatLogging(debug);
    }
}
