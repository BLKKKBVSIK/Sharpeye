package sharpeye.sharpeye.processors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import sharpeye.sharpeye.R;
import sharpeye.sharpeye.Services.HeadSignService;
import sharpeye.sharpeye.data.SharedPreferencesHelper;
import sharpeye.sharpeye.utils.CurrentState;
import sharpeye.sharpeye.utils.PopUpFactory;
import sharpeye.sharpeye.utils.ServiceTools;

public class HeadUpSignProcessor extends DataProcessor {

    public static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private static Intent i = null;

    public HeadUpSignProcessor(Context _appContext, Activity _activityContext) {
        super(_appContext, _activityContext);
    }

    public void create()
    {

    }

    public void resume(CurrentState currentState)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(appContext,"sign_bubble",false) &&
                !Settings.canDrawOverlays(activityContext)) {

            new PopUpFactory(activityContext)
                    .setTitle(appContext.getString(R.string.sign_popout_title))
                    .setMessage(appContext.getString(R.string.sign_popout_message))
                    .setNegativeButton(appContext.getString(R.string.sign_popout_negative), () -> {
                        SharedPreferencesHelper.INSTANCE.setSharedPreferencesBoolean(appContext,"sign_bubble",false);
                        Toast.makeText(appContext, appContext.getString(R.string.sign_popout_negative_toast), Toast.LENGTH_SHORT).show();
                    })
                    .setPositiveButton(appContext.getString(R.string.sign_popout_positive), () -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + appContext.getPackageName()));
                        activityContext.startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                    })
                    .show();
        }
        if (i != null && ServiceTools.isServiceRunning("HeadSignService", appContext)) {
            appContext.stopService(i);
            i = null;
        }
    }

    public void pause(CurrentState currentState)
    {
        if (currentState.isSpeedLimit() && SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(appContext,"sign_bubble",false)) {
            appContext.startService(i = new Intent(activityContext, HeadSignService.class));
        }
    }

    public CurrentState process(CurrentState currentState)
    {
        return currentState;
    }

    public void clean() {
        if (i != null && ServiceTools.isServiceRunning("HeadSignService", appContext)) {
            appContext.stopService(i);
            i = null;
        }
        Log.d("HeadUpSign", "start");
    }
}
