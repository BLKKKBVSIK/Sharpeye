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
import android.media.ImageReader.OnImageAvailableListener;
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
import sharpeye.sharpeye.customview.OverlayView;
import sharpeye.sharpeye.customview.OverlayView.DrawCallback;
import sharpeye.sharpeye.data.SharedPreferencesHelper;
import sharpeye.sharpeye.objects_logic.ObjectsProcessing;
import sharpeye.sharpeye.objects_logic.Speech;
import sharpeye.sharpeye.utils.BorderedText;
import sharpeye.sharpeye.utils.ImageUtils;
import sharpeye.sharpeye.utils.Logger;
import sharpeye.sharpeye.tflite.Classifier;
import sharpeye.sharpeye.tflite.SignClassifier;
import sharpeye.sharpeye.tflite.TFLiteObjectDetectionAPIModel;
import sharpeye.sharpeye.signs.BipGenerator;
import sharpeye.sharpeye.signs.SignList;
import sharpeye.sharpeye.tracking.MultiBoxTracker;
import sharpeye.sharpeye.tracking.Tracker;
import sharpeye.sharpeye.objects_logic.WarningEvent;

import java.io.IOException;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener, GPSCallback {
    private static final Logger LOGGER = new Logger();


    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE = "models/traffic sign general/signDetectSmallData.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/models/traffic sign general/generalTrafficLabels.txt";
    private static final String TF_OD_API_MODEL_FILE_DANGER = "models/car - person/detect_coco.tflite";
    private static final String TF_OD_API_LABELS_FILE_DANGER = "file:///android_asset/models/car - person/labelmap_coco.txt";
    /*private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/trafficSignGeneralMobilenet.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/generalTrafficLabels.txt";*/


    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.  Optionally use legacy Multibox (trained using an older version of the API)
    // or YOLO.
    private enum DetectorMode {
        TF_OD_API
    }

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;

    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

    private static final boolean MAINTAIN_ASPECT = false;

    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;

    private Classifier detector;
    private Classifier dangerDetector;

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

    private SignClassifier signClassifier;

    private Tracker tracker;

    protected boolean initializedTracking = false;

    protected boolean computingDetection = false;

    private long lastRecognition = 0;

    private ObjectsProcessing objectsProcessing;

    ///speed update and display
    /*private var gpsManager: GPSManager? = null
    private var locationManager: LocationManager? = null
    var isGPSEnabled: Boolean = false
    var speed = 0.toDouble()
    var currentSpeed: Double = 0.toDouble()
    var kmphSpeed:Double = 0.toDouble()
    var txtview: TextView? = null*/
    boolean alertCollision = false;
    float speed;
    double kmphSpeed;
    TextView txtview = null;
    GPSManager gpsManager;
    LocationManager locationManager;
    boolean isGPSEnabled;
    BipGenerator bipGenerator;
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
        //txtview.setVisibility(View.VISIBLE);
        currentState = new CurrentState();
        signList = new SignList(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("TRACKER", tracker);
        super.onSaveInstanceState(outState);
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(getApplicationContext(),"vocal_on",false)) {
            if (objectsProcessing == null) {
                objectsProcessing = new ObjectsProcessing();
            }
            objectsProcessing.init(this);
        }
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(getApplicationContext(),"speed_display",false))
            txtview.setVisibility(View.VISIBLE); //séparer afficher la vitesse et rappel de vitesse pour pouvoir mieux désactiver l'un ou l'autre
        else
            txtview.setVisibility(View.INVISIBLE);
        initializeGPS();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (objectsProcessing != null) {
            objectsProcessing.release();
            objectsProcessing = null;
        }
        cleanGPS();
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        tracker.free();
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

	    try {
	      detector =
		  TFLiteObjectDetectionAPIModel.create(
		      getAssets(),
		      TF_OD_API_MODEL_FILE,
		      TF_OD_API_LABELS_FILE,
		      TF_OD_API_INPUT_SIZE,
		      TF_OD_API_IS_QUANTIZED);
	      dangerDetector =
                  TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE_DANGER,
                            TF_OD_API_LABELS_FILE_DANGER,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
	      cropSize = TF_OD_API_INPUT_SIZE;
	    } catch (final IOException e) {
	      e.printStackTrace();
	      LOGGER.e(e, "Exception initializing classifier!");
	      Toast toast =
		  Toast.makeText(
		      getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
	      toast.show();
	      finish();
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
	    multiBoxTracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    OverlayView trackingOverlay;
    private Speech speech;
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

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

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

        final List<Classifier.Recognition> results;
        List<Classifier.Recognition> dangerResults = null;
        final List<Classifier.Recognition> fullResults = new ArrayList<>();
        boolean tracking = false;
        if (!initializedTracking || (startTime - lastRecognition) >=2000) {
            results = new ArrayList<>();
            List<Classifier.Recognition> tmp = detector.recognizeImage(croppedBitmap);
            for (Classifier.Recognition val: tmp) {
                if (val.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API && val.getLocation().right >= 0 &&
                        val.getLocation().left >= 0 && val.getLocation().bottom >= 0 && val.getLocation().top >= 0 &&
                        val.getLocation().right < 500 && val.getLocation().left < 500 && val.getLocation().bottom < 500 &&
                        val.getLocation().top < 500) {
                    results.add(val);
                }
            }
            // TODO uncomment and set tflite model
            long timeSpent = System.currentTimeMillis();
            //Classifier
            for (int i = 0; i < results.size(); ++i) {
                if (results.get(i).getConfidence() > MINIMUM_CONFIDENCE_TF_OD_API) {
                    String result = signClassifier.detectSign(results.get(i), croppedBitmap, rgbOrientedBitmap, 0.6f);
                    if (signClassifier.getLastResults().size() >= 1) {
                        Classifier.Recognition elem;
                        elem = new Classifier.Recognition(results.get(i).getId(), result, signClassifier.getLastResults().get(0).getConfidence(), results.get(i).getLocation());
                        elem.setOpencvID(results.get(i).getOpencvID());
                        Log.d("SIGNCLASSIFIER", "ResultAdd="+elem.getTitle()+"|"+elem.getOpencvID());

                        results.add(i, elem);
                        Log.d("SIGNCLASSIFIER", "ResultRemove="+results.get(i+1).getTitle()+"|"+results.get(i+1).getOpencvID());
                        results.remove(i + 1);
                    }
                }
            }
            timeSpent = System.currentTimeMillis() - timeSpent;
            System.out.println("Time: " + timeSpent / 1000.0f);

            dangerResults = new ArrayList<>();
            tmp = dangerDetector.recognizeImage(croppedBitmap);
            for (Classifier.Recognition val: tmp) {
                if ((val.getTitle().equals("person") || val.getTitle().equals("car")) &&
                        val.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API && val.getLocation().right >= 0 &&
                        val.getLocation().left >= 0 && val.getLocation().bottom >= 0 && val.getLocation().top >= 0 &&
                        val.getLocation().right < 500 && val.getLocation().left < 500 && val.getLocation().bottom < 500 &&
                        val.getLocation().top < 500) {
                    dangerResults.add(val);
                }
            }
            for (Classifier.Recognition recog : dangerResults) {
                Log.d("DETECTORACTIVITY", "OID="+recog.getOpencvID()+ " | ID="+recog.getId()+" | title="+recog.getTitle()+" | bottom="+recog.getLocation().bottom+" | top="+ recog.getLocation().top+" | left="+recog.getLocation().left+" | right="+recog.getLocation().right);
            }
            fullResults.addAll(results);
            fullResults.addAll(dangerResults);
            tracker.track(croppedBitmap, fullResults);
            initializedTracking = true;
            lastRecognition = SystemClock.uptimeMillis();
        } else {
            results = tracker.update(croppedBitmap);
            alertCollision = tracker.isAlertCollision();
            tracking = true;
        }
//        if (currentState.getSpeed() > 10 && alertCollision) {
//            if (speech == null) {
//                speech = new Speech(getApplicationContext());
//            }
//            speech.speak("Alerte collision");
//            Log.e("Collision", "alerte collision");
//        }
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
        }


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
                    if (objectsProcessing != null && !tracking) {
                        objectsProcessing.processDetectedObject(result);
                        currentState.addSign(signList.get(result.getTitle()));
                    }
                } catch (NullPointerException ex) {
                    Log.e("Detector", "WarningEvent already released");
                }
            }
        }
        if (dangerResults != null) {
            for (final Classifier.Recognition result : dangerResults) {
                final RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= minimumConfidence) {
                    canvas.drawRect(location, paint);

                    cropToFrameTransform.mapRect(location);
                    result.setLocation(location);
                    mappedRecognitions.add(result);
                    try {
                        if (objectsProcessing != null) {
                            objectsProcessing.processDetectedObject(result);
                        }
                    } catch (NullPointerException ex) {
                        Log.e("Detector", "WarningEvent already released");
                    }
                }
            }
        }

        multiBoxTracker.trackResults(mappedRecognitions, currTimestamp);
        trackingOverlay.postInvalidate();

        computingDetection = false;
	runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                    showInference(lastProcessingTimeMs + "ms");
                  }
                });
    }

    ///--------Speed-------------------
    public void initializeGPS(){
        Log.d("initializeGPS", "start");
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }//demander dans camera Activity
        } catch (Exception e) {
            e.printStackTrace();
        }
        txtview.setText(getString(R.string.speed_counter));
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        gpsManager = new GPSManager(DetectorActivity.this);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(isGPSEnabled) {
            gpsManager.startListening(this);
            gpsManager.setGPSCallback((GPSCallback) this);
        } else {
            gpsManager.showSettingsAlert();
        }
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(getApplicationContext(),"speed_control",false))
        {
            bipGenerator = new BipGenerator();
        } else {
            bipGenerator = null;
        }
        Log.d("initializeGPS", "end");
    }

    private void cleanGPS() {
        Log.d("cleanGPS", "start");
        if (gpsManager != null) {
            gpsManager.stopListening();
            gpsManager.setGPSCallback(null);
            gpsManager = null;
        }
        if (bipGenerator != null)
        {
            bipGenerator = null;
        }
        Log.d("cleanGPS", "end");
    }

    @Override
    public void onGPSUpdate(Location location) {
        speed = location.getSpeed() * 3.6f;
        currentState.setSpeed(round(speed, 3, BigDecimal.ROUND_HALF_UP));
        kmphSpeed = round((currentState.getSpeed()),3,BigDecimal.ROUND_HALF_UP);
        txtview.setText(kmphSpeed+"km/h");
        if (currentState != null && currentState.getSpeedLimit() != 0) {
            if (currentState.getSpeed() >= currentState.getSpeedLimit() * 1.05) {
                txtview.setTextColor(Color.rgb(255, 0, 0));
                if (bipGenerator != null) {
                    bipGenerator.bip(150, 100);
                }
            } else if (currentState.getSpeed() > currentState.getSpeedLimit()) {
                txtview.setTextColor(Color.rgb(255, 165, 0));
            } else {
                txtview.setTextColor(Color.rgb(255, 255, 255));
            }
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
    @Override
    protected void setUseNNAPI(final boolean isChecked) {
      runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
      runInBackground(() -> detector.setNumThreads(numThreads));
    }
}
