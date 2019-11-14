package sharpeye.sharpeye.utils;

import android.os.Build;

/**
 * Adds functions to deal with the Phone itself
 */
public class Phone {

    /**
     * Gets the name of the phone
     * @return A string with the phone name
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return model.toUpperCase();
        } else {
            return ((manufacturer) + " " + model).toUpperCase();
        }
    }
}
