package sharpeye.sharpeye.tracking;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import sharpeye.sharpeye.tflite.Classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tracker implements Parcelable {

    static {
        System.loadLibrary("native-lib");
    }

    private long trackerAddress;
    private HashMap<Integer, Classifier.Recognition> trackedObjects;

    public Tracker() {
        trackerAddress = -1;
        trackedObjects = new HashMap<>();
    }

    public  boolean needInit() {
        return (trackerAddress <= 0);
    }

    public void init() {
        trackerAddress = createTracker();
    }

    public void free() {
        deleteTracker(trackerAddress);
        trackerAddress = -1;
    }

    @SuppressWarnings("unchecked")
    private Tracker(Parcel in) {
        this.trackerAddress = in.readLong();
        this.trackedObjects = new HashMap<>();
        if (in.readByte() == 1) {
            int[] ids = in.createIntArray();
            List<Classifier.Recognition> recognitions = in.readArrayList(Classifier.Recognition.class.getClassLoader());
            for (int i = 0; i < ids.length; ++i) {
                trackedObjects.put(ids[i], recognitions.get(i));
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(trackerAddress);
        if (!trackedObjects.isEmpty()) {
            dest.writeByte((byte)1);
            int[] ids = new int[trackedObjects.size()];
            int i = -1;
            List<Classifier.Recognition> recognitions = new ArrayList<>();
            for (Map.Entry<Integer, Classifier.Recognition> entry : trackedObjects.entrySet()) {
                ids[++i] = entry.getKey();
                recognitions.add(entry.getValue());
            }
            dest.writeIntArray(ids);
            dest.writeList(recognitions);
        } else {
            dest.writeByte((byte)0);
        }
    }

    public static final Parcelable.Creator<Tracker> CREATOR = new Parcelable.Creator<Tracker>() {
        public Tracker createFromParcel(Parcel in) {
            return new Tracker(in);
        }

        public Tracker[] newArray(int size) {
            return new Tracker[size];
        }
    };

    private Mat bitmapToMat(Bitmap bmp) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bmp, mat);
        Mat noAlphaMat = new Mat();
        Imgproc.cvtColor(mat, noAlphaMat, Imgproc.COLOR_RGBA2RGB);
        return noAlphaMat;
    }

    private Classifier.Recognition findRecognitionObjectWithRect(List<Classifier.Recognition> initialList, Rect2f box) {
        for (Classifier.Recognition object: initialList) {
            RectF location = object.getLocation();
            if (location.left == box.x && location.top == box.y &&
                    location.width() == box.width && location.height() == box.height) {
                return object;
            }
        }
        throw new UnknownError("Cannot find the initial recognition object");
    }

    public void track(Bitmap frame, List<Classifier.Recognition> objects) {
        ArrayList<Rect2f> boxes = new ArrayList<>();
        for (Classifier.Recognition object: objects) {
            RectF location = object.getLocation();
            Rect2f box = new Rect2f(location.left, location.top, location.width(), location.height());
            boxes.add(box);
        }
        Mat matFrame = bitmapToMat(frame);
        long frameAddress = matFrame.nativeObj;
        HashMap<Integer, Rect2f> objectIDs = addBoxes(trackerAddress, frameAddress, boxes);
        HashMap<Integer, Classifier.Recognition> newTrackedObjects = new HashMap<>();
        for (HashMap.Entry<Integer, Rect2f> objectID: objectIDs.entrySet()) {
            Integer id = objectID.getKey();
            Rect2f box = objectID.getValue();
            Classifier.Recognition recognizedObject;
            recognizedObject = findRecognitionObjectWithRect(objects, box);
            recognizedObject.setOpencvID(id);
            newTrackedObjects.put(id, recognizedObject);
        }
        trackedObjects = newTrackedObjects;
    }

    public List<Classifier.Recognition> update(Bitmap frame) {
        Mat matFrame = bitmapToMat(frame);
        long frameAddress = matFrame.nativeObj;
        HashMap<Integer, Rect2f> objectIDs = updateBoxes(trackerAddress, frameAddress);
        Log.e("Debug", "after updateboxes and before alert");
        boolean isDangerous = isDangerous(trackerAddress);
        Log.e("isDangerous", String.valueOf(isDangerous));
        HashMap<Integer, Classifier.Recognition> newTrackedObjects = new HashMap<>();
        List<Classifier.Recognition> recognitionList = new ArrayList<>();
        for (HashMap.Entry<Integer, Rect2f> objectID: objectIDs.entrySet()) {
            Integer id = objectID.getKey();
            Rect2f box = objectID.getValue();
            if (trackedObjects.containsKey(id)) {
                Classifier.Recognition recognizedObject = trackedObjects.get(id);
                if (recognizedObject != null) {
                    recognizedObject.setOpencvID(id);
                    recognizedObject.setLocation(new RectF(box.x, box.y, box.width + box.x, box.height + box.y));
                    newTrackedObjects.put(id, recognizedObject);
                    recognitionList.add(recognizedObject);
                }
            }
        }
        trackedObjects = newTrackedObjects;
        return recognitionList;
    }

    public static class Rect2f {
        public float x;
        public float y;
        public float width;
        public float height;

        public Rect2f(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private native long createTracker();
    private native void deleteTracker(long ptr);
    private native HashMap<Integer, Rect2f> addBoxes(long trackerAddress, long frameAddress, ArrayList<Rect2f> boxes);
    private native HashMap<Integer, Rect2f> updateBoxes(long trackerAddress, long frameAddress);
    private native boolean isDangerous(long trackerAddress);

}
