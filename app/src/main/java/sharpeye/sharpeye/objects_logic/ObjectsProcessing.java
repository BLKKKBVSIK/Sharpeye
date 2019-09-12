package sharpeye.sharpeye.objects_logic;

import android.content.Context;
import android.util.Log;
import sharpeye.sharpeye.tflite.Classifier;
import sharpeye.sharpeye.utils.AudioPlayer;

import java.util.ArrayList;

public class ObjectsProcessing {

    private WarningEvent warningEvent;
    private ArrayList<Integer> objectsProcessed = new ArrayList<>();
    private AudioPlayer dangerSound = new AudioPlayer();

    public void init(Context context) {
        warningEvent = new WarningEvent(context);
    }

    public void release() {
        if (warningEvent != null) {
            warningEvent.clean();
            warningEvent = null;
        }
        dangerSound.release();
    }

    public void processDangerousObject(boolean isDangerous) {
        if (isDangerous) {
            //TODO start sound
        } else {
            //TODO stop sound
        }
    }

    public void processDetectedObject(Classifier.Recognition object) {
        try {
            if (warningEvent != null && !objectsProcessed.contains(object.getOpencvID())) {
                warningEvent.triggerWarning(object.getTitle());
                objectsProcessed.add(object.getOpencvID());
            }
        } catch (NullPointerException ex) {
            Log.e("Detector", "WarningEvent already cleaned");
        }
    }

}
