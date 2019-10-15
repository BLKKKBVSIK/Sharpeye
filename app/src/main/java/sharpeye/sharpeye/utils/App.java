package sharpeye.sharpeye.utils;

import sharpeye.sharpeye.BuildConfig;

public class App {
    public static String BuildNumber()
    {
        return Integer.toString(BuildConfig.VERSION_CODE);
    }

    public  static String FullVersionName()
    {
        return VersionName() + "-" + BuildConfig.BUILD_TYPE;
    }

    public static String VersionName()
    {
        return BuildConfig.VERSION_NAME;
    }
}
