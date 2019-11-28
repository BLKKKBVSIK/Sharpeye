package sharpeye.sharpeye.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Adds functions to deal with Services
 */
public class ServiceTools {

    /**
     * Tells if a given service is running or not
     * @param serviceClassName the name of the service
     * @param context context of the app
     * @return boolean if the service is running or not
     */
    public static boolean isServiceRunning(String serviceClassName, Context context){
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            Log.d("isServiceRunning", runningServiceInfo.service.getClassName());
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)){
                return true;
            }
        }
        return false;
    }
}