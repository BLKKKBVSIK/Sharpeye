package sharpeye.sharpeye;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;

public class SignClassifier {

    //Format d'image attendu par le classifier
    private static final int INPUT_SIZE = 32;
    private static final int IMAGE_MEAN = 0;
    private static final int IMAGE_STD = 118;

    //Nom des nodes d'input et d'output du classifier (à vérifier avec un parser de modèle)
    private static final String INPUT_NAME = "1";
    private static final String OUTPUT_NAME = "predictions";

    //Infos sur le modèle
    private static final String MODEL_FILE =
            "file:///android_asset/model_hacked.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/model_labels.txt";

    private static final float MINIMUM_CONFIDENCE_DIFFERENCE = 0.75f;

    List<Classifier.Recognition> lastResults;

    Classifier classifier;

    public SignClassifier(Context context) {
        classifier =
                TensorFlowImageClassifier.create(
                        context.getAssets(),
                        MODEL_FILE,
                        LABEL_FILE,
                        INPUT_SIZE,
                        IMAGE_MEAN,
                        IMAGE_STD,
                        INPUT_NAME,
                        OUTPUT_NAME);
    }

    public String checkForTrafficSign(Classifier.Recognition recognition, Bitmap imageDetected) {
        RectF rect = recognition.getLocation();

        int startX = (int)rect.left;
        int startY = (int)rect.top;
        int width = (int)(rect.right - rect.left);
        int height = (int)(rect.bottom - rect.top);
        List<Classifier.Recognition> results;

        Bitmap croppedBmp;
        Bitmap finalBmp;

        croppedBmp = imageDetected;
        croppedBmp = Bitmap.createBitmap(imageDetected, startX, startY, width, height);
        finalBmp = Bitmap.createScaledBitmap(croppedBmp, INPUT_SIZE, INPUT_SIZE, true);
        results = classifier.recognizeImage(finalBmp);
        lastResults = results;

        return (interpretResults(results));
    }

    public List<Classifier.Recognition> getLastResults() {
        return (lastResults);
    }

    private String interpretResults(List<Classifier.Recognition> results) {
        if (results.size() > 1 && results.get(0).getConfidence() - results.get(1).getConfidence() < MINIMUM_CONFIDENCE_DIFFERENCE) {
            results.add(0, new Classifier.Recognition("-1", "NONE", 0.0f, new RectF(0, 0, 0, 0)));
            return ("NONE");
        }

        return (results.get(0).getTitle());
    }
}
