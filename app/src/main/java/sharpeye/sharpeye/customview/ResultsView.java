package sharpeye.sharpeye.customview;

import java.util.List;
import sharpeye.sharpeye.tflite.Classifier.Recognition;

public interface ResultsView {
    void setResults(final List<Recognition> results);
}
