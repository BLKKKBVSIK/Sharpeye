package sharpeye.sharpeye.tflite;

import java.util.ArrayList;
import java.util.List;

public class FalsePositiveManager {

    List<PendingRecog> recogs;
    private final static long maxDetectTime = 600;
    private final static float detectNbrThreshold = 2;

    public class PendingRecog {
        Classifier.Recognition detection;
        int detectNbr;
        float detectTime;
    }

    public FalsePositiveManager() {
        recogs = new ArrayList<>();
    }

    public List<Classifier.Recognition> checkDetections(List<Classifier.Recognition> detections) {
        List<Classifier.Recognition> validRecogs = new ArrayList<>();
        PendingRecog recog;
        long time = System.currentTimeMillis();

        for (Classifier.Recognition detection : detections) {
            if ((recog = getRecogByName(detection.getTitle())) != null) {
                if (time - recog.detectTime <= maxDetectTime && time != recog.detectTime) {
                    recog.detectTime = time;
                    recog.detectNbr++;
                } else {
                    addRecog(detection, time);
                }
            } else {
                addRecog(detection, time);
            }
        }

        for (PendingRecog pendingRecog : recogs) {
            if (time - pendingRecog.detectTime > maxDetectTime) {
                recogs.remove(pendingRecog);
            } else if (pendingRecog.detectNbr >= detectNbrThreshold) {
                validRecogs.add(pendingRecog.detection);
                recogs.remove(pendingRecog);
            }
        }

        return (validRecogs);
    }

    private void addRecog(Classifier.Recognition detection, long time) {
        PendingRecog newRecog = new PendingRecog();

        newRecog.detection = detection;
        newRecog.detectNbr = 1;
        newRecog.detectTime = time;
    }

    private PendingRecog getRecogByName(String recogName) {
        for (PendingRecog recog : recogs) {
            if (recog.detection.getTitle().equals(recogName))
                return (recog);
        }
        return (null);
    }

}
