package sharpeye.sharpeye.Detection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sharpeye.sharpeye.data.SharedPreferencesHelper;
import sharpeye.sharpeye.objects_logic.ObjectsProcessing;
import sharpeye.sharpeye.processors.GPSProcessor;
import sharpeye.sharpeye.processors.HeadUpSignProcessor;
import sharpeye.sharpeye.processors.ProcessorsManager;
import sharpeye.sharpeye.signs.Sign;
import sharpeye.sharpeye.signs.SignList;
import sharpeye.sharpeye.tflite.Classifier;
import sharpeye.sharpeye.tflite.FrameBuffer;
import sharpeye.sharpeye.tflite.SignDetector;
import sharpeye.sharpeye.tflite.TFLiteObjectDetectionAPIModel;
import sharpeye.sharpeye.tracking.Tracker;
import sharpeye.sharpeye.utils.CurrentState;
import sharpeye.sharpeye.utils.ImageUtils;
import sharpeye.sharpeye.utils.Logger;

public class Detector {

    private static final Logger LOGGER = new Logger();

    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE_DANGER = "models/car_person/detect_coco.tflite";
    private static final String TF_OD_API_LABELS_FILE_DANGER = "file:///android_asset/models/car_person/labelmap_coco.txt";

    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;

    private static final boolean MAINTAIN_ASPECT = false;

    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private boolean initializedTracking = false;

    private Tracker tracker;
    private ObjectsProcessing objectsProcessing;

    private long lastRecognition = 0;

    private Classifier dangerDetector;
    private SignDetector signClassifier;

    private SignList signList;

    private Matrix rotationTransform;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private Bitmap rgbOrientedBitmap = null;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private CurrentState currentState;

    private ProcessorsManager processorsManager;
    private FrameBuffer frameBuffer;


    private Size previewSize;

    public Detector(Context context, FrameBuffer _frameBuffer) {
        currentState = new CurrentState();
        frameBuffer = _frameBuffer;
        processorsManager = new ProcessorsManager(new Logger(ProcessorsManager.class));
        processorsManager
                .add(new GPSProcessor(context, (Activity)context, new Logger(GPSProcessor.class)))
                .add(new HeadUpSignProcessor(context, (Activity)context, new Logger(HeadUpSignProcessor.class)))
                .create();
    }

    public void save(Bundle state) {
        state.putParcelable("TRACKER", tracker);
    }

    public void restore(Bundle savedInstanceState) {
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
    }

    public void resume(Context context) {
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context,"vocal_on",false)) {
            if (objectsProcessing == null) {
                objectsProcessing = new ObjectsProcessing();
            }
            objectsProcessing.init(context);
        }
        processorsManager.resume(currentState);
    }

    public void pause() {
        if (objectsProcessing != null) {
            objectsProcessing.release();
            objectsProcessing = null;
        }
        processorsManager.pause(currentState);
    }

    public void destroy() {
        if (tracker != null) {
            tracker.free();
        }
        processorsManager.clean();
    }

    public void setNumThread(int numThread) {
        dangerDetector.setNumThreads(numThread);
    }

    public void onPreviewSizeChosen(Context context, Size size, int rotation, int orientation) {
        previewSize = size;
        signList = new SignList(context);
        int cropSize = TF_OD_API_INPUT_SIZE;
        try {
            signClassifier = new SignDetector(context, frameBuffer);
            dangerDetector = TFLiteObjectDetectionAPIModel.create(
                    context.getAssets(),
                    TF_OD_API_MODEL_FILE_DANGER,
                    TF_OD_API_LABELS_FILE_DANGER,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            context, "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            ((Activity)context).finish();
        }
        int sensorOrientation = rotation - orientation;
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", size.getWidth(), size.getHeight());
        rgbFrameBitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Bitmap.Config.ARGB_8888);
        rgbOrientedBitmap = Bitmap.createBitmap(size.getHeight(), size.getWidth(), Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);


        final boolean maintainAspectRatio = MAINTAIN_ASPECT;
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        size.getWidth(), size.getHeight(),
                        cropSize, cropSize,
                        sensorOrientation, maintainAspectRatio);

        rotationTransform =
                ImageUtils.getTransformationMatrix(
                        size.getWidth(), size.getHeight(),
                        size.getHeight(), size.getWidth(),
                        sensorOrientation, maintainAspectRatio);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        signClassifier.setBitmapProcessVariables(rgbFrameBitmap, rgbOrientedBitmap, rotationTransform, size.getWidth(), size.getHeight());
    }

    public void detect(Context context, int[] rgbBytes, DetectorListener detectorListener) {
        //------------------processorsManager------------------
        currentState = processorsManager.process(currentState);
        //-----------------------------------------------

        rgbFrameBitmap.setPixels(rgbBytes, 0, previewSize.getWidth(), 0, 0, previewSize.getWidth(), previewSize.getHeight());

        final Canvas canvas1 = new Canvas(croppedBitmap);
        canvas1.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        final Canvas canvas2 = new Canvas(rgbOrientedBitmap);
        canvas2.drawBitmap(rgbFrameBitmap, rotationTransform, null);
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }
        final long startTime = SystemClock.uptimeMillis();
        final List<Classifier.Recognition> results;
        List<Classifier.Recognition> dangerResults = null;
        final List<Classifier.Recognition> fullResults = new ArrayList<>();
        boolean tracking = false;
        boolean signConfirmation = false;
        results = new ArrayList<>();
        if (signClassifier.isDetectingSign()) {
            List<Classifier.Recognition> tmp = signClassifier.verifySign(rgbOrientedBitmap, MINIMUM_CONFIDENCE_TF_OD_API);
            for (Classifier.Recognition val: tmp) {
                if (val.getLocation().right >= 0 &&
                        val.getLocation().left >= 0 && val.getLocation().bottom >= 0 && val.getLocation().top >= 0 &&
                        val.getLocation().right < 5000 && val.getLocation().left < 5000 && val.getLocation().bottom < 5000 &&
                        val.getLocation().top < 5000) {
                    results.add(val);
                    signConfirmation = true;
                }
            }
        }
        if (!initializedTracking || (startTime - lastRecognition) >= 200) {
            List<Classifier.Recognition> tmp;
            if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context,"signs_on",false)) {
                tmp = signClassifier.detectSign(rgbOrientedBitmap, MINIMUM_CONFIDENCE_TF_OD_API);
                for (Classifier.Recognition val : tmp) {
                    if (val.getLocation().right >= 0 &&
                            val.getLocation().left >= 0 && val.getLocation().bottom >= 0 && val.getLocation().top >= 0 &&
                            val.getLocation().right < 5000 && val.getLocation().left < 5000 && val.getLocation().bottom < 5000 &&
                            val.getLocation().top < 5000) {
                        results.add(val);
                    }
                }
            }
            dangerResults = new ArrayList<>();
            if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context,"danger_on",false)) {
                tmp = dangerDetector.recognizeImage(croppedBitmap);
                for (Classifier.Recognition val : tmp) {
                    if ((val.getTitle().equals("person") || val.getTitle().equals("car")) &&
                            val.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API && val.getLocation().right >= 0 &&
                            val.getLocation().left >= 0 && val.getLocation().bottom >= 0 && val.getLocation().top >= 0 &&
                            val.getLocation().right < 500 && val.getLocation().left < 500 && val.getLocation().bottom < 500 &&
                            val.getLocation().top < 500) {
                        dangerResults.add(val);
                    }
                }
            }

            fullResults.addAll(results);
            fullResults.addAll(dangerResults);
            tracker.track(croppedBitmap, dangerResults);
            initializedTracking = true;
            lastRecognition = SystemClock.uptimeMillis();
        } else {
            double speed = currentState.isSpeed() ? currentState.getSpeed() : 0;
            results.addAll(tracker.update(croppedBitmap, speed));
            if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context,"collision_on",false)) {
                tracker.alertIfDangerous(speed);
            }
            tracking = true;
        }
        final long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

        Bitmap cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
        final Canvas canvas = new Canvas(cropCopyBitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<>();

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
                mappedRecognitions.add(result);
                try {
                    if (objectsProcessing != null && (!tracking || signConfirmation)) {
                        objectsProcessing.processDetectedObject(result);
                        Sign sign = signList.get(result.getTitle());
                        if (sign != null) currentState.addSign(sign);
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
        detectorListener.detectionCallback(lastProcessingTimeMs, cropCopyBitmap, mappedRecognitions);
    }

    public interface DetectorListener {
        void detectionCallback(long processingTime, Bitmap cropBitmap, List<Classifier.Recognition> recognitionList);
    }

}
