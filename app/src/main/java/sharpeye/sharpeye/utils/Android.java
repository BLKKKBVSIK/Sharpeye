package sharpeye.sharpeye.utils;

import android.os.Build;

/**
 * Adds functions to get Android details
 */
public class Android {

    /**
     * Gets the android version of the phone
     * @return the android version
     */
    private static double getVersion()
    {
        return Double.parseDouble(Build.VERSION.RELEASE.replaceAll("(\\d+[.]\\d+)(.*)", "$1"));
    }

    /**
     * Gets the android versionCode (Oreo/Pie/Nougat...)
     * @return the android versionCode
     */
    private static String getAndroidVersionCode()
    {
        double release = getVersion();
        String codeName = "Unsupported"; //below Jelly bean OR above Oreo
        if (release < 6) codeName = "Unsupported";
        else if (release < 7) codeName = "Marshmallow";
        else if (release < 8) codeName = "Nougat";
        else if (release < 9) codeName = "Oreo";
        else if (release < 10) codeName = "Pie";
        else if (release < 11) codeName = "Ten";
        return codeName;
    }

    /**
     * Gets the android full version of the phone
     * @return a formatted string with android version
     */
    public static String getAndroidVersion() {
        double version = getVersion();
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        String codeName = Build.VERSION.CODENAME;
        return  version + " " + getAndroidVersionCode() +
                " (Build: " + release + " " + codeName + "/SDK: " + sdkVersion +")";
    }
}
