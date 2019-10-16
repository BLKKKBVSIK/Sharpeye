package sharpeye.sharpeye.tflite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sharpeye.sharpeye.tflite.Classifier;
import sharpeye.sharpeye.tflite.TFLiteObjectDetectionAPIModel;


public class SignDetector {
    List<Classifier.Recognition> lastResults;

    private Classifier generalDetector;
    private Classifier signDifferentiator;
    private Classifier digitReader;


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
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void saveTempBitmap(Bitmap bitmap) {
        if (isExternalStorageWritable()) {
            saveImage(bitmap);
        }else{
            //prompt the user or do something
        }
    }

    private void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fname = "Shuttab_" + timeStamp + ".jpg";

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
        int width = original.getWidth();
        int height = original.getHeight();
        int left;
        int right;
        int top;
        int bottom;

        if (width < height) {
            left = 0;
            right = width;
            top = (int)((height / 2.0f) - (width / 2.0f));
            bottom = top + width;
            processedImage = cropBitmap(original, left, right, top, bottom);
        } else if (height < width) {
            top = 0;
            bottom = height;
            left = (int)((width / 2.0f) - (height / 2.0f));
            right = left + height;
            processedImage = cropBitmap(original, left, right, top, bottom);
        } else
            processedImage = original;

        return (processedImage);
    }

    private RectF getSignRect(RectF box, Bitmap ref, Bitmap target) {
        RectF newBox = new RectF();

        newBox.left = box.left * target.getWidth() / ref.getWidth();
        newBox.right = box.right * target.getWidth() / ref.getWidth();
        newBox.bottom = box.bottom * target.getHeight() / ref.getHeight();
        newBox.top = box.top * target.getHeight() / ref.getHeight();

        return (newBox);
    }

    public List<Classifier.Recognition> detectSign(Bitmap original, float confidence) {
        Bitmap signProcessedFrame;
        signProcessedFrame = processImage(original);
        Bitmap frameResized = getResizedBitmap(signProcessedFrame.copy(signProcessedFrame.getConfig(), signProcessedFrame.isMutable()), TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);

        List<Classifier.Recognition> results = generalDetector.recognizeImage(frameResized);
        List<Classifier.Recognition> signs = new ArrayList<>();
        List<Classifier.Recognition> differentiator = new ArrayList<>();

        for (Classifier.Recognition result : results) {
            if (result.getConfidence() >= confidence) {
                RectF rect = getSignRect(result.getLocation(), frameResized, signProcessedFrame);
                //saveImage(signProcessedFrame);
                Bitmap cropped = cropBitmap(signProcessedFrame, rect.left, rect.right, rect.top, rect.bottom);
                //saveImage(cropped);
                Bitmap processedCropped = getResizedBitmap(processImage(cropped), TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE);
                saveImage(processedCropped);
                differentiator = signDifferentiator.recognizeImage(processedCropped);
                float offset = (original.getHeight() - signProcessedFrame.getHeight()) / 2.0f;
                RectF originalRect = new RectF(result.getLocation().left, result.getLocation().top, result.getLocation().right, result.getLocation().bottom);
                adaptLocationsToCropSize(signProcessedFrame.getHeight(), signProcessedFrame.getWidth(), originalRect, 300, 300);
                originalRect.top += offset;
                originalRect.bottom += offset;
                adaptLocationsToCropSize(300, 300, originalRect, original.getHeight(), original.getWidth());
                for (Classifier.Recognition diffResult : differentiator) {
                    if (diffResult.getConfidence() > confidence) {
                        diffResult.setLocation(originalRect);
                        signs.add(diffResult);
                    }
                }
            }
        }

        return (signs);
    }

    private void adaptLocationsToCropSize(float cropheight, float cropwidth, RectF location, float height, float width) {
        location.left = (location.left * cropwidth) / width;
        location.right = (location.right * cropwidth) / width;
        location.top = (location.top * cropheight) / height;
        location.bottom = (location.bottom * cropheight) / height;
    }
}
