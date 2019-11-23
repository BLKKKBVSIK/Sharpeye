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

public class SignDetector {
    List<Classifier.Recognition> lastResults;

    private Classifier generalDetector;
    private Classifier signDifferentiator;
    private CropTracker cropTracker = null;
    private boolean signVerification = false;
    private boolean debugMode = false;

    private static final int VERIFICATION_NBR = 1;
    private int currentVerificationNbr = 0;


    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE_GENERAL =
            "models/traffic_sign_general/signDetectSmallData.tflite";
    private static final String TF_OD_API_LABELS_FILE_GENERAL =
            "file:///android_asset/models/traffic_sign_general/generalTrafficLabels.txt";

    private static final String TF_OD_API_MODEL_FILE_DIFFERENTIATOR =
            "models/traffic_sign_classifier/signDetector43.tflite";
    private static final String TF_OD_API_LABELS_FILE_DIFFERENTIATOR =
            "file:///android_asset/models/traffic_sign_classifier/label43.txt";


    private List<Classifier.Recognition> resultsFinal;

    private final boolean quantized = true;
    int i = 0;


    public SignDetector(Context context) throws IOException {
            generalDetector = TFLiteObjectDetectionAPIModel.create(
                    context.getAssets(), TF_OD_API_MODEL_FILE_GENERAL, TF_OD_API_LABELS_FILE_GENERAL, TF_OD_API_INPUT_SIZE, quantized);


            signDifferentiator = TFLiteObjectDetectionAPIModel.create(
                    context.getAssets(), TF_OD_API_MODEL_FILE_DIFFERENTIATOR, TF_OD_API_LABELS_FILE_DIFFERENTIATOR, TF_OD_API_INPUT_SIZE, quantized);

            resultsFinal = new ArrayList<>();
            resultsFinal.clear();
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void saveTempBitmap(Bitmap bitmap) {
        if (isExternalStorageWritable()) {
            saveImage(bitmap);
        }else{
            //prompt the user or do something
        }
    }

    public void SetDebugMode(boolean value) {
        debugMode = value;
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
        /*int width = original.getWidth();
        int height = original.getHeight();
        int cropSize = TF_OD_API_INPUT_SIZE;
        int left;
        int right;
        int top;
        int bottom;

        right = original.getWidth();
        left = right - cropSize;
        top = (int)((height / 2.0f) - (cropSize / 2.0f));
        bottom = top + cropSize;*/

        RectF crop = cropTracker.getCropRect();
        System.out.println("crop debug " + crop);
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
        boolean requeueVerification = false;

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
                            cropTracker.updateTarget(diffResult.getTitle(), tmpRect);
                            diffResult.setLocation(originalRect);
                            signs.add(diffResult);
                        } else {
                            diffResult.setLocation(originalRect);
                            Log.e("SignDetect", "Original: " + cropTracker.getTarget() + " - Verification: " + diffResult.getTitle());
                            if (cropTracker.getTarget() != null && diffResult.getTitle().equals(cropTracker.getTarget()) && !requeueVerification) {
                                if (currentVerificationNbr < VERIFICATION_NBR) {
                                    currentVerificationNbr++;
                                    requeueVerification = true;
                                }
                                else {
                                    Log.e("SignDetect", "Sign confirmation: " + diffResult.getTitle());
                                    signs.add(diffResult);
                                }
                            }
                            signVerification = false;
                        }
                    }
                }
            }
        }

        if (requeueVerification)
            signVerification = true;

        if (debugMode) {
            RectF cropTrackerDebug = cropTracker.getCropRect();

            adaptLocationsToCropSize(300, 300, cropTrackerDebug, original.getHeight(), original.getWidth());
            Classifier.Recognition debug = new Classifier.Recognition("-1", "Debug", 1.0f, cropTrackerDebug);

            signs.add(debug);
        }

        return (signs);
    }

    private boolean signDetected(List<Classifier.Recognition> signs) {
        for (Classifier.Recognition sign : signs) {
            if (!sign.getTitle().equals("Debug")) {
                signVerification = true;
                return (true);
            }
        }
        return (false);
    }

    public boolean isDetectingSign() {
        return (signVerification);
    }

    public List<Classifier.Recognition> verifySign(Bitmap original, float confidence) {
        Log.e("SignDetect", "Sign verification: " + currentVerificationNbr + "/" + VERIFICATION_NBR);
        cropTracker.trackTarget();
        cropTracker.updateTrack();
        signVerification = false;
        return (detectOnCrop(confidence, original, true));
    }

    public List<Classifier.Recognition> detectSign(Bitmap original, float confidence) {
        if (cropTracker == null) {
            cropTracker = new CropTracker(CropTracker.Direction.Vertical, original.getWidth(), original.getHeight(), TF_OD_API_INPUT_SIZE, (int)(TF_OD_API_INPUT_SIZE * 0.8f), true, (int)(TF_OD_API_INPUT_SIZE * 0.8f) + TF_OD_API_INPUT_SIZE + 2);
            cropTracker.setOffPos(original.getWidth() - TF_OD_API_INPUT_SIZE);
        }

        currentVerificationNbr = 1;
        List<Classifier.Recognition> signs = new ArrayList<>();
        cropTracker.cancelTarget();

        while (!signDetected(signs) && cropTracker.hasNextOffset()) {
            cropTracker.updateTrack();
            signs.addAll(detectOnCrop(confidence, original, false));
        }

        if (signVerification)
            Log.e("SignDetect", "Found sign, starting fast verification");

        cropTracker.resetOffset();

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
