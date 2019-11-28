package sharpeye.sharpeye.tflite;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FrameBuffer {

    private final static int BUFFERING_INTERVAL_MS = 100;

    private long lastBufferedImageTime = 0;

    public class Frame {
        public int[] bytes;
        public long timestamp;

        public Frame(int[] _bytes, long _timestamp) {
            bytes = _bytes;
            timestamp = _timestamp;
        }
    }

    List<Frame> frames;
    Frame detectionFrame;
    long previousTimestamp = 0;
    boolean deleteLocked = false;

    public FrameBuffer() {
        frames = new ArrayList<>();
    }

    public void addFrame(int[] bytes, long timestamp) {
        if (timestamp - lastBufferedImageTime < BUFFERING_INTERVAL_MS) {
            return;
        }

        lastBufferedImageTime = timestamp;
        frames.add(new Frame(bytes, timestamp));
    }

    public void setDetectionFrame(int[] bytes, long timestamp) {
        detectionFrame = new Frame(bytes, timestamp);
    }

    public void saveTimeStamp() {
        deleteUntilPreviousDetection();
        previousTimestamp = detectionFrame.timestamp;
    }

    public void setDeleteLocked(boolean value) {
        deleteLocked = value;
    }

    public void deleteUntil(long timestamp) {
        for (int i = 0; i < frames.size(); ++i) {
            if (frames.get(i).timestamp <= timestamp && frames.size() > 2) {
                frames.remove(i);
                --i;
            } else if (frames.get(i).timestamp > timestamp)
                break;
        }
    }

    public void deleteUntilPreviousDetection() {
        if (deleteLocked || previousTimestamp == 0)
            return;

        long timestamp = previousTimestamp;

        for (int i = 0; i < frames.size(); ++i) {
            if (frames.get(i).timestamp <= timestamp && frames.size() > 2) {
                frames.remove(i);
                --i;
            } else if (frames.get(i).timestamp > timestamp)
                break;
        }
    }

    public Frame getDetectionFrame() {
        return (detectionFrame);
    }

    public Frame getPreviousBufferedFrame(long relativeTimestamp) {
        for (int i = 0; i < frames.size(); ++i) {
            if (i + 1 < frames.size() && frames.get(i + 1).timestamp >= relativeTimestamp)
                return (frames.get(i));
        }

        return (null);
    }

    public Frame getNextBufferedFrame(long relativeTimestamp) {
        for (int i = 0; i < frames.size(); ++i) {
            if (i + 1 < frames.size() && frames.get(i + 1).timestamp > relativeTimestamp)
                return (frames.get(i + 1));
        }

        return (null);
    }
}
