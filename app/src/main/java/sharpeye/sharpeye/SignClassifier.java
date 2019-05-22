package sharpeye.sharpeye;

import android.content.Context;
import android.graphics.Bitmap;
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

public class SignClassifier {

    //Format d'image attendu par le classifier
    /*private static final int INPUT_SIZE = 32;
    private static final int IMAGE_MEAN = 0;
    private static final int IMAGE_STD = 118;

    //Nom des nodes d'input et d'output du classifier (à vérifier avec un parser de modèle)
    private static final String INPUT_NAME = "1";
    private static final String OUTPUT_NAME = "predictions";

    //Infos sur le modèle
    private static final String MODEL_FILE =
            "file:///android_asset/model_hacked.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/model_labels.txt";*/

    /*private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 0;
    private static final int IMAGE_STD = 255;

    //Nom des nodes d'input et d'output du classifier (à vérifier avec un parser de modèle)
    private static final String INPUT_NAME = "Placeholder";
    private static final String OUTPUT_NAME = "final_result";

    //Infos sur le modèle
    private static final String MODEL_FILE =
            "file:///android_asset/inception.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/inception.txt";

    private static final float MINIMUM_CONFIDENCE_DIFFERENCE = 0.75f;*/

    Bitmap savedBitmap;

    List<Classifier.Recognition> lastResults;

    Classifier classifier;
    private Classifier detector;


    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/trafficSignReader.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/trafficSignsLabels.txt";

    private List<Classifier.Recognition> resultsFinal;


    public SignClassifier(Context context) throws IOException {
        /*classifier =
                TensorFlowImageClassifier.create(
                        context.getAssets(),
                        MODEL_FILE,
                        LABEL_FILE,
                        INPUT_SIZE,
                        IMAGE_MEAN,
                        IMAGE_STD,
                        INPUT_NAME,
                        OUTPUT_NAME);*/

            detector = TensorFlowObjectDetectionAPIModel.create(
                    context.getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);

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
        String fname = "Shutta_"+ timeStamp +".jpg";

        File file = new File(myDir, fname);
        if (file.exists()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String detectSign(Classifier.Recognition recognition, Bitmap cropped, Bitmap ref, float confidence) {
        RectF rect = recognition.getLocation();

        int startX = (int)(rect.left * ref.getWidth() / cropped.getWidth());
        int startY = (int)(rect.top * ref.getHeight() / cropped.getHeight());
        int width = (int)((rect.right * ref.getWidth() / cropped.getWidth()) - startX);
        int height = (int)((rect.bottom * ref.getHeight() / cropped.getHeight()) - startY);
        List<Classifier.Recognition> results;

        Bitmap croppedBmp;
        Bitmap finalBmp;

        startX = startX < 0 ? 0 : startX;
        startY = startY < 0 ? 0 : startY;
        width = (width + startX) > ref.getWidth() ? (ref.getWidth() - 1) - startX : width;
        height = (height + startY) > ref.getHeight() ? (ref.getHeight() - 1) - startY : height;

        if (width > height)
            height = width;
        else
            width = height;

        width = (width + startX) > ref.getWidth() ? (ref.getWidth() - 1) - startX : width;
        height = (height + startY) > ref.getHeight() ? (ref.getHeight() - 1) - startY : height;

        croppedBmp = Bitmap.createBitmap(ref, startX, startY, width, height);
        finalBmp = Bitmap.createScaledBitmap(croppedBmp, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, true);
        results = detector.recognizeImage(finalBmp);
        resultsFinal.clear();
        for (int i = 0; i < results.size(); ++i) {
            if (results.get(i).getConfidence() > confidence)
                resultsFinal.add(results.get(i));
        }
        System.out.println("Detection: " + resultsFinal.size());
        lastResults = resultsFinal;
        //saveTempBitmap(finalBmp);

        return (interpretResultsDetector(results));
    }

    /*public String checkForTrafficSign(Classifier.Recognition recognition, Bitmap cropped, Bitmap ref) {
        RectF rect = recognition.getLocation();

        int startX = (int)(rect.left * ref.getWidth() / cropped.getWidth());
        int startY = (int)(rect.top * ref.getHeight() / cropped.getHeight());
        int width = (int)((rect.right * ref.getWidth() / cropped.getWidth()) - startX);
        int height = (int)((rect.bottom * ref.getHeight() / cropped.getHeight()) - startY);
        List<Classifier.Recognition> results;

        Bitmap croppedBmp;
        Bitmap finalBmp;

        startX = startX < 0 ? 0 : startX;
        startY = startY < 0 ? 0 : startY;
        width = (width + startX) > ref.getWidth() ? (ref.getWidth() - 1) - startX : width;
        height = (height + startY) > ref.getHeight() ? (ref.getHeight() - 1) - startY : height;

        if (width > height)
            height = width;
        else
            width = height;

        width = (width + startX) > ref.getWidth() ? (ref.getWidth() - 1) - startX : width;
        height = (height + startY) > ref.getHeight() ? (ref.getHeight() - 1) - startY : height;

        croppedBmp = Bitmap.createBitmap(ref, startX, startY, width, height);
        finalBmp = Bitmap.createScaledBitmap(croppedBmp, INPUT_SIZE, INPUT_SIZE, true);
        results = classifier.recognizeImage(finalBmp);
        lastResults = results;

        savedBitmap = finalBmp;
        saveTempBitmap(finalBmp);

        return (interpretResults(results));
    }

    public Bitmap getBitmap() {
        return (savedBitmap);
    }

    private String interpretResults(List<Classifier.Recognition> results) {
        if (results.size() == 0 || (results.size() > 1 && results.get(0).getConfidence() - results.get(1).getConfidence() < MINIMUM_CONFIDENCE_DIFFERENCE)) {
            results.add(0, new Classifier.Recognition("-1", "NONE", 0.0f, new RectF(0, 0, 0, 0)));
            return ("NONE");
        }

        return (results.get(0).getTitle());
    }*/

    private String interpretResultsDetector(List<Classifier.Recognition> results) {
        if (resultsFinal.size() != 1) {
            resultsFinal.clear();
            resultsFinal.add(0, new Classifier.Recognition("-1", "NONE", 0.0f, new RectF(0, 0, 0, 0)));
            return ("NONE");
        }

        return (resultsFinal.get(0).getTitle());
    }

    public List<Classifier.Recognition> getLastResults() {
        return (lastResults);
    }
}
