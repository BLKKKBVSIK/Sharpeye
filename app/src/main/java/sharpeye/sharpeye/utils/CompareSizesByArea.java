package sharpeye.sharpeye.utils;

import android.util.Size;

import java.util.Comparator;

/**
 * Compares two {@code Size}s based on their areas.
 */
public class CompareSizesByArea implements Comparator<android.util.Size> {
    @Override
    public int compare(final android.util.Size lhs, final Size rhs) {
        // We cast here to ensure the multiplications won't overflow
        return Long.signum(
                (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
    }
}