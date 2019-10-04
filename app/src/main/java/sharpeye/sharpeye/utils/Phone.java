package sharpeye.sharpeye.utils;

import android.os.Build;

public class Phone {
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
