package sharpeye.sharpeye.popups;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import sharpeye.sharpeye.R;
import sharpeye.sharpeye.data.SharedPreferencesHelper;
import sharpeye.sharpeye.utils.Phone;
import sharpeye.sharpeye.utils.PopUpFactory;

/**
 * Handles the battery popup
 */
public class BatteryPopupHandler {
    private Context appContext;
    private Context activity;
    private boolean alreadyInflated = false;

    private BroadcastReceiver mReceiver;

    /**
     * constructor
     * @param _appContext app context
     * @param _activity activity context
     */
    public BatteryPopupHandler(Context _appContext, Context _activity)
    {
        appContext = _appContext;
        activity = _activity;
        mReceiver = new BatteryBroadcastReceiver();
    }

    /**
     * Call to initiate BatteryPopupHandler
     */
    public void Start()
    {
        appContext.registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    /**
     * Call to terminate BatteryPopupHandler
     */
    public void Stop()
    {
        appContext.unregisterReceiver(mReceiver);
    }

    /**
     * Broadcast receiver monitoring the battery
     */
    private class BatteryBroadcastReceiver extends BroadcastReceiver {

        /**
         * Called every time it receives a broadcast about battery
         * @param context context of the app
         * @param intent an intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            Log.d("BatteryPopupHandler BatteryBroadcastReceiver onReceive", "battery level :" + batteryLevel);
            if (batteryLevel < 25
                    && !alreadyInflated
                    && !Phone.isCharging(appContext)
                    && SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context,"battery_popup_on",false))
            {
                final boolean[] checked = {false};
                new PopUpFactory(activity)
                        .setTitle(appContext.getString(R.string.battery_title))
                        .setMessage(appContext.getString(R.string.battery_message))
                        .setCheckbox(context.getString(R.string.battery_popup_dont_show_again), isChecked -> checked[0] = true)
                        .setPositiveButton(context.getString(R.string.battery_popup_ok), () -> {
                            if (checked[0]){
                                SharedPreferencesHelper.INSTANCE.setSharedPreferencesBoolean(context,"battery_popup_on",false);
                            }
                        }).show();
                alreadyInflated = true;
            }
        }
    }
}
