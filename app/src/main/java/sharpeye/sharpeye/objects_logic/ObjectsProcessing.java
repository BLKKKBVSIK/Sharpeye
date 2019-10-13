package sharpeye.sharpeye.objects_logic;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import sharpeye.sharpeye.tflite.Classifier;
import sharpeye.sharpeye.utils.AudioPlayer;

import java.util.ArrayList;

public class ObjectsProcessing {

    private WarningEvent warningEvent;
    private ArrayList<Integer> objectsProcessed = new ArrayList<>();
    private ArrayList<Integer> objectsFalsePositiveVerification = new ArrayList<>();
    private AudioPlayer dangerSound = new AudioPlayer();
    private ToneGenerator toneGenerator;

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
    public boolean isTTSAvailable() {
        return warningEvent.isTTSAvailable();
    }

    public void processDangerousObject(boolean isDangerous) {
        int volume = 100;
        if (isDangerous) {
            //TODO start sound
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, volume);
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP);
        } else {
            //TODO stop sound
            toneGenerator.stopTone();
        }
    }

    public void processDetectedObject(Classifier.Recognition object) {
        try {
            if (warningEvent != null && !objectsProcessed.contains(object.getOpencvID())) {
                if (objectsFalsePositiveVerification.contains(object.getOpencvID())) {
                    warningEvent.triggerWarning(object.getTitle());
                    objectsProcessed.add(object.getOpencvID());
                } else {
                    objectsFalsePositiveVerification.add(object.getOpencvID());
                }
            }
        } catch (NullPointerException ex) {
            Log.e("Detector", "WarningEvent already cleaned");
        }
    }

}
