package sharpeye.sharpeye.tflite;

import android.graphics.RectF;

public class CropTracker {

    public enum Direction {
        Horizontal,
        Vertical
    }

    public class Target {
        String name;
        RectF rect;
        long lastSeen;
    }

    private final static long TIME_LIMIT = 2000;

    int width;
    int height;
    int cropSize;
    int interval;
    int limit;
    int start;

    boolean tracking = false;
    boolean idle = true;
    Target target = null;
    String lastDetected;

    private int offset;
    private int offDirPos = 1;
    private int offsetSaved = 0;

    Direction idleDirection = Direction.Horizontal;

    private RectF cropRect;

    public CropTracker(Direction idleDir, int _width, int _height, int _cropSize, int _interval, boolean targetable, int _limit, int _start) {
        idleDirection = idleDir;
        width = _width;
        height = _height;
        cropSize = _cropSize;
        interval = _interval;
        tracking = targetable;
        cropRect = new RectF();
        limit = _limit;
        start = _start;
        offset = start;

        if ((limit == -1 || limit > width) && idleDirection == Direction.Horizontal) {
            limit = width;
        } else if (limit == -1 || limit > height && idleDirection == Direction.Vertical)
            limit = height;
    }

    public String getTarget() {
        return (lastDetected);
    }

    public void setOffPos(int pos) {
        offDirPos = pos;
        if (idleDirection == Direction.Horizontal) {
            cropRect.top = offDirPos;
            cropRect.bottom = offDirPos + cropSize;
        } else {
            cropRect.left = offDirPos;
            cropRect.right = offDirPos + cropSize;
        }
    }

    public void updateTarget(String targetName, RectF pos) {
        if (target == null || (target.name.equals(targetName))) {
            lastDetected = targetName;
            target = new Target();
            target.lastSeen = System.currentTimeMillis();
            target.name = targetName;
            target.rect = pos;
            target.rect.left = (int)target.rect.left;
            target.rect.right = (int)target.rect.right;
            target.rect.top = (int)target.rect.top;
            target.rect.bottom = (int)target.rect.bottom;
        }
    }

    public void trackTarget() {
        idle = false;
    }

    public void cancelTarget() {
        target = null;
        idle = true;
    }

    private void cropOnTarget() {

        cropRect.left = (((int)(target.rect.right - target.rect.left) * 0.5f)) + target.rect.left - (cropSize / 2.0f);
        if (cropRect.left <= 1) {
            cropRect.left = 1;
            cropRect.right = cropRect.left + cropSize;
        }


        cropRect.right = cropRect.left + cropSize;
        if (cropRect.right > width) {
            cropRect.right = width;
            cropRect.left = width - cropSize;
        }

        cropRect.top = (((int)(target.rect.bottom - target.rect.top) * 0.5f)) + target.rect.top - cropSize / 2.0f;
        if (cropRect.top <= 1) {
            cropRect.top = 1;
            cropRect.bottom = cropRect.top + cropSize;
        }

        cropRect.bottom = cropRect.top + cropSize;
        if (cropRect.bottom > height) {
            cropRect.bottom = height;
            cropRect.top = height - cropSize;
        }

        offsetSaved = (int)cropRect.top;
    }

    public boolean hasNextOffset() {
        float nextOffset;

        nextOffset = offset;
        return (!(nextOffset + cropSize >= limit) || idleDirection != Direction.Horizontal) && (!(nextOffset + cropSize >= limit) || idleDirection != Direction.Vertical);
    }

    public void resetOffset() {
        offset = start;
    }

    public void updateTrack() {
        if (target == null || System.currentTimeMillis() - target.lastSeen > TIME_LIMIT) {
            target = null;
            idle = true;
            if (idleDirection == Direction.Vertical) {
                cropRect.left = offDirPos;
                cropRect.right = offDirPos + cropSize;
            } else {
                cropRect.top = offDirPos;
                cropRect.bottom = offDirPos + cropSize;
            }
        }

        if (idle) {
            if (idleDirection == Direction.Horizontal) {
                cropRect.left = offset;
                cropRect.right = offset + cropSize;
            } else {
                cropRect.top = offset;
                cropRect.bottom = offset + cropSize;
            }
            offsetSaved = offset;
            offset += interval;
        } else {
            cropOnTarget();
        }
    }

    public int getOffsetSaved() {
        return (offsetSaved);
    }

    public RectF getCropRect() {
        RectF rectCopy = new RectF(cropRect);

        return (rectCopy);
    }

}
