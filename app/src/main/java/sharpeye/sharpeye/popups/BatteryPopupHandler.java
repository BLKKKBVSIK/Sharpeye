package sharpeye.sharpeye.popups;

import android.content.Context;
import android.util.Log;

import sharpeye.sharpeye.R;
import sharpeye.sharpeye.utils.Phone;
import sharpeye.sharpeye.utils.PopUpFactory;

/**
 * Handles the battery popup
 */
public class BatteryPopupHandler {
    private Context appContext;
    private Context activity;
    private boolean alreadyInflated = false;

    /**
     * constructor
     * @param _appContext app context
     * @param _activity activity context
     */
    public BatteryPopupHandler(Context _appContext, Context _activity)
    {
        appContext = _appContext;
        activity = _activity;
    }

    /**
     * call this method regularly
     * to check to battery level,
     * will remind the user to charge it's phone
     */
    public void update()
    {
        Log.d("BatteryPopupHandler update", alreadyInflated + " " + Phone.isCharging(appContext) + " " + Phone.getBatteryPercentage(appContext));
        if (!alreadyInflated && !Phone.isCharging(appContext)
                && Phone.getBatteryPercentage(appContext) < 0.30)
        {
            new PopUpFactory(activity).setTitle(appContext.getString(R.string.battery_title)).setMessage(appContext.getString(R.string.battery_message))
                    .setPositiveButton("Ok", () -> {}).show();
            alreadyInflated = true;
        }
    }

}
