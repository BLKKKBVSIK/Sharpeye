package sharpeye.sharpeye.utils;

import sharpeye.sharpeye.BuildConfig;

/**
 * Made to get all the details about the app itself
 */
public class App {

    /**
     * returns the build number of the app
     * @return The build number
     */
    public static String BuildNumber()
    {
        return Integer.toString(BuildConfig.VERSION_CODE);
    }

    /**
     * returns the full version name format VersionName - Type of the build (debug/release...)
     * @return the version name
     */
    public  static String FullVersionName()
    {
        return VersionName() + "-" + BuildConfig.BUILD_TYPE;
    }

    /**
     * returns the version name
     * @return the version name
     */
    public static String VersionName()
    {
        return BuildConfig.VERSION_NAME;
    }
}
