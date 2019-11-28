package sharpeye.sharpeye.tflite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sharpeye.sharpeye.BuildConfig;
import sharpeye.sharpeye.objects_logic.Speech;

public class SignDetector {

    public class Detection {
        String title;
        float confidence;
        RectF pos;
        long timestamp;
        int confirmations = 0;
        int current_step = 0;
        long previousTimestamp;
        String id;

        public Detection(String _title, RectF _pos, long _timestamp, float _confidence, String _id) {
            title = _title;
            pos = _pos;
            timestamp = _timestamp;
            confidence = _confidence;
            id = _id;
        }
    }

    private Bitmap rgbOrientedBitmap;
    private Bitmap rgbFrameBitmap;
    private Matrix rotationTransform;
    private int previewWidth;
    private int previewHeight;
    Speech speech;


    private Classifier generalDetector;
    private Classifier signDifferentiator;
    private CropTracker cropTracker = null;
    private boolean debugMode = false;
    private boolean voiceDebug = false;

    private static final int CONFIRMATION_NBR = 2;
    private static final int MAXIMUM_VERIFICATION_QUEUE = 4;

    private List<Detection> detections;

    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE_GENERAL =
            "models/traffic_sign_general/signDetectSmallData.tflite";
    private static final String TF_OD_API_LABELS_FILE_GENERAL =
            "file:///android_asset/models/traffic_sign_general/generalTrafficLabels.txt";

    private static final String TF_OD_API_MODEL_FILE_DIFFERENTIATOR =
            "models/traffic_sign_classifier/signDetector43.tflite";
    private static final String TF_OD_API_LABELS_FILE_DIFFERENTIATOR =
            "file:///android_asset/models/traffic_sign_classifier/label43.txt";


    private FrameBuffer frameBuffer;

    private final boolean quantized = true;
    int i = 0;


    public SignDetector(Context context, FrameBuffer _frameBuffer) throws IOException {
            generalDetector = TFLiteObjectDetectionAPIModel.create(
                    context.getAssets(), TF_OD_API_MODEL_FILE_GENERAL, TF_OD_API_LABELS_FILE_GENERAL, TF_OD_API_INPUT_SIZE, quantized);


            signDifferentiator = TFLiteObjectDetectionAPIModel.create(
                    context.getAssets(), TF_OD_API_MODEL_FILE_DIFFERENTIATOR, TF_OD_API_LABELS_FILE_DIFFERENTIATOR, TF_OD_API_INPUT_SIZE, quantized);
            detections = new ArrayList<>();
            frameBuffer = _frameBuffer;
            speech = new Speech(context);
            if (BuildConfig.DEBUG) {
                setDebugMode(true, true);
            }
    }


    public void setBitmapProcessVariables(Bitmap _rgbFrameBitmap, Bitmap _rgbOrientedBitmap, Matrix _rotationTransform, int _previewWidth, int _previewHeight) {
        rgbOrientedBitmap = _rgbOrientedBitmap;
        rgbFrameBitmap = _rgbFrameBitmap;
        rotationTransform = _rotationTransform;
        previewWidth = _previewWidth;
        previewHeight = _previewHeight;
    }

    public void setDebugMode(boolean value, boolean voiceValue) {
        debugMode = value;
        voiceDebug = voiceValue;
    }

    private void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fname = "Shuttab_" + timeStamp + "_" + i + ".jpg";
        ++i;

        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private Bitmap cropBitmap(Bitmap original, float left, float right, float top, float bottom) {
        Bitmap croppedBitmap = Bitmap.createBitmap((int)right - (int)left, (int)bottom - (int)top, Bitmap.Config.ARGB_8888);
        new Canvas(croppedBitmap).drawBitmap(original, (int)-left, (int)-top, null);

        return (croppedBitmap);
    }

    private Bitmap processImage(Bitmap original) {
        Bitmap processedImage;

        RectF crop = cropTracker.getCropRect();
        processedImage = cropBitmap(original, crop.left, crop.right, crop.top, crop.bottom);

        return (processedImage);
    }

    private RectF getSignRect(RectF box) {
        RectF newBox = new RectF();

        newBox.left = box.left;
        newBox.right = box.right;
        newBox.bottom = box.bottom;
        newBox.top = box.top;

        if (newBox.right - newBox.left > newBox.bottom - newBox.top) {
            float diff = (newBox.right - newBox.left) - (newBox.bottom - newBox.top);
            newBox.bottom += diff * 0.5;
            newBox.top -= diff * 0.5;
        } else if (newBox.right - newBox.left < newBox.bottom - newBox.top) {
            float diff = (newBox.bottom - newBox.top) - (newBox.right - newBox.left);
            newBox.right += diff * 0.5;
            newBox.left -= diff * 0.5;
        }

        return (newBox);
    }

    private List<Classifier.Recognition> detectOnCrop(float confidence, Bitmap original, boolean verification) {
        Bitmap signProcessedFrame;
        signProcessedFrame = processImage(original);

        if (verification && debugMode)
            saveImage(signProcessedFrame);
        List<Classifier.Recognition> results = generalDetector.recognizeImage(signProcessedFrame);
        List<Classifier.Recognition> signs = new ArrayList<>();
        List<Classifier.Recognition> differentiator;

        float yoffset = cropTracker.getOffsetSaved();
        float xoffset = cropTracker.getCropRect().left;

        for (Classifier.Recognition result : results) {
            if (result.getConfidence() >= confidence) {
                RectF rect = getSignRect(result.getLocation());
                Bitmap cropped = cropBitmap(signProcessedFrame, rect.left, rect.right, rect.top, rect.bottom);
                Bitmap processedCropped = getResizedBitmap(cropped, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);
                differentiator = signDifferentiator.recognizeImage(processedCropped);
                RectF originalRect = new RectF(result.getLocation().left, result.getLocation().top, result.getLocation().right, result.getLocation().bottom);
                originalRect.top += yoffset;
                originalRect.bottom += yoffset;
                originalRect.left += xoffset;
                originalRect.right += xoffset;
                RectF tmpRect = new RectF(originalRect);
                adaptLocationsToCropSize(300, 300, originalRect, original.getHeight(), original.getWidth());
                for (Classifier.Recognition diffResult : differentiator) {
                    if (diffResult.getConfidence() > confidence) {
                        if (!verification) {
                            diffResult.setLocation(tmpRect);
                            signs.add(diffResult);
                            if (debugMode) {
                                Classifier.Recognition debug = new Classifier.Recognition("-1", "Debug - " + diffResult.getTitle(), diffResult.getConfidence(), originalRect);
                                signs.add(debug);
                            }
                        } else {
                            diffResult.setLocation(originalRect);
                            Log.d("SignDetect", "Original: " + cropTracker.getTarget() + " - Verification: " + diffResult.getTitle());
                            if (cropTracker.getTarget() != null && diffResult.getTitle().equals(cropTracker.getTarget())) {
                                detections.get(0).confirmations++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (debugMode) {
            RectF cropTrackerDebug = cropTracker.getCropRect();

            adaptLocationsToCropSize(300, 300, cropTrackerDebug, original.getHeight(), original.getWidth());
            Classifier.Recognition debug = new Classifier.Recognition("-1", "Debug", 1.0f, cropTrackerDebug);

            signs.add(debug);
        }

        return (signs);
    }

    public boolean isDetectingSign() {
        return (detections.size() >= 1);
    }

    private void processFrameBytes(int[] bytes) {
        rgbFrameBitmap.setPixels(bytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
        final Canvas canvas2 = new Canvas(rgbOrientedBitmap);
        canvas2.drawBitmap(rgbFrameBitmap, rotationTransform, null);
    }

    public List<Classifier.Recognition> verifySign(Bitmap original, float confidence) {
        Log.d("SignDetect", "Sign verification: " + detections.get(0).current_step + "/" + 2);
        FrameBuffer.Frame bufferedFrame;
        List<Classifier.Recognition> signs = new ArrayList<>();


        if (detections.get(0).current_step == 0) {
            bufferedFrame = frameBuffer.getPreviousBufferedFrame(detections.get(0).timestamp);
            if (bufferedFrame == null) {
                detections.remove(0);
                return (signs);
            }
            detections.get(0).previousTimestamp = bufferedFrame.timestamp;
        } else if (detections.get(0).current_step == 1) {
            bufferedFrame = frameBuffer.getPreviousBufferedFrame(detections.get(0).previousTimestamp);
            if (bufferedFrame == null) {
                detections.remove(0);
                return (signs);
            }
        } else {
            bufferedFrame = frameBuffer.getNextBufferedFrame(detections.get(0).timestamp);
            if (bufferedFrame == null) {
                return (signs);
            }
        }

        processFrameBytes(bufferedFrame.bytes);
        if (debugMode)
            saveImage(rgbOrientedBitmap);
        cropTracker.updateTarget(detections.get(0).title, detections.get(0).pos);
        cropTracker.trackTarget();
        cropTracker.updateTrack();
        detectOnCrop(confidence, rgbOrientedBitmap, true);
        if (++detections.get(0).current_step >= 3) {
            if (detections.get(0).confirmations >= CONFIRMATION_NBR) {
                Log.d("SignDetect", "Sign confirmation: " + detections.get(0).title);
                adaptLocationsToCropSize(300, 300, detections.get(0).pos, original.getHeight(), original.getWidth());
                signs.add(new Classifier.Recognition(detections.get(0).id, detections.get(0).title, detections.get(0).confidence, detections.get(0).pos));
            } else {
                Log.d("SignDetect", "False positive: Dismissing");
                if (voiceDebug) {
                    speech.speak("Faux positif.");
                }
            }
            frameBuffer.deleteUntil(detections.get(0).timestamp);
            detections.remove(0);
        }

        return (signs);
    }

    private boolean inVerification(String signName) {
        for (Detection detection : detections) {
            if (detection.title.equals(signName))
                return (true);
        }
        return (false);
    }

    public List<Classifier.Recognition> detectSign(Bitmap original, float confidence) {
        if (cropTracker == null) {
            cropTracker = new CropTracker(CropTracker.Direction.Vertical, original.getWidth(), original.getHeight(), TF_OD_API_INPUT_SIZE, (int)(TF_OD_API_INPUT_SIZE * 0.8f), true, (int)(TF_OD_API_INPUT_SIZE * 0.8f) + TF_OD_API_INPUT_SIZE + 2, 170);
            cropTracker.setOffPos(original.getWidth() - TF_OD_API_INPUT_SIZE);
        }


        List<Classifier.Recognition> signs = new ArrayList<>();
        cropTracker.cancelTarget();
        while (cropTracker.hasNextOffset()) {
            cropTracker.updateTrack();
            signs.addAll(detectOnCrop(confidence, original, false));
        }

        for (int i = 0; i < signs.size(); ++i) {
            if (!signs.get(i).getTitle().startsWith("Debug") && detections.size() < MAXIMUM_VERIFICATION_QUEUE && !inVerification(signs.get(i).getTitle())) {
                Log.d("SignDetect", "Potential sign detected");
                detections.add(new Detection(signs.get(i).getTitle(), new RectF(signs.get(i).getLocation()), frameBuffer.getDetectionFrame().timestamp, signs.get(i).getConfidence(), signs.get(i).getId()));
                if (voiceDebug) {
                    speech.speak("Panneau potentiel détecté " + signs.get(i).getTitle());
                }
                signs.remove(i);
                --i;
                if (debugMode)
                   saveImage(original);
            }
        }

       cropTracker.resetOffset();
       frameBuffer.saveTimeStamp();
       if (isDetectingSign())
           frameBuffer.setDeleteLocked(true);
       else
           frameBuffer.setDeleteLocked(false);

        if (!debugMode)
            signs.clear();
        return (signs);


    }

    private void adaptLocationsToCropSize(float cropheight, float cropwidth, RectF location, float height, float width) {
        location.left = (location.left * cropwidth) / width;
        location.right = (location.right * cropwidth) / width;
        location.top = (location.top * cropheight) / height;
        location.bottom = (location.bottom * cropheight) / height;
    }
}
