package sharpeye.sharpeye.processors;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Debug;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import sharpeye.sharpeye.R;
import sharpeye.sharpeye.Services.GPSService;
import sharpeye.sharpeye.data.SharedPreferencesHelper;
import sharpeye.sharpeye.signs.frontManagers.FrontElementManager;
import sharpeye.sharpeye.signs.frontManagers.SignViewManager;
import sharpeye.sharpeye.signs.frontManagers.SpeedViewManager;
import sharpeye.sharpeye.signs.frontViews.SignView;
import sharpeye.sharpeye.signs.frontViews.SpeedView;
import sharpeye.sharpeye.utils.CurrentState;
import sharpeye.sharpeye.utils.Logger;
import sharpeye.sharpeye.utils.ServiceTools;

/**
 * Gps processor extends from DataProcessor
 */
public class GPSProcessor extends DataProcessor{

    private boolean GpsOnAlertAlreadyInflated = false;
    private boolean GPSPermissionAlreadyInflated = false;
    private Intent i;

    private GPSService mService;
    private boolean mBound = false;
    private LocationManager locationManager;

    private List<FrontElementManager> frontManagers;

    /**
     * Constructor
     * @param _appContext context of the app
     * @param _activityContext context of the activity
     */
    public GPSProcessor(Context _appContext, Activity _activityContext, Logger _logger)
    {
        super(_appContext, _activityContext, _logger);
        frontManagers = new ArrayList<>();
        frontManagers.add(new SignViewManager(activityContext, new SignView(activityContext), false));
        frontManagers.add(new SpeedViewManager(activityContext, new SpeedView(activityContext), false));
    }

    @Override
    public void create()
    {
        locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(appContext,"speed_display",false)) {
            initializeGPS();
        }
    }

    @Override
    public void resume(CurrentState currentState)
    {
        logger.d("Gps resume start");
        currentState.setSpeed(false);
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(appContext,"speed_display",false)) {
            if (mBound) {
                mService.setCurrentState(currentState);
                currentState = mService.getCurrentState();
            }
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  && GpsOnAlertAlreadyInflated) {
                turnOffGpsPreferences();
                stopService();
                logger.d("gps not enabled");
            }
            else if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && GPSPermissionAlreadyInflated)
            {
                turnOffGpsPreferences();
                stopService();
                logger.d("unauthorized");
            } else if (!ServiceTools.isServiceRunning("sharpeye.sharpeye.Services.GPSService", appContext)) {
                startService();
                logger.d("restart service");
            }
        }
        else {
            stopService();
        }
    }

    @Override
    public void pause(CurrentState currentState) {

    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            logger.d("onServiceConnected");
            GPSService.GPSBinder binder = (GPSService.GPSBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void startService()
    {
        logger.d("startService start");
        i= new Intent(appContext, GPSService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(i);
        } else {
            appContext.startService(i);
        }
        Intent intent = new Intent(appContext, GPSService.class);
        appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void stopService()
    {
        logger.d("stopService start");
        if (mService != null && mBound) {
            appContext.unbindService(connection);
            mBound = false;
            appContext.stopService(i);
        }
    }

    public void initializeGPS(){
        startService();
    }

    @Override
    public CurrentState process(CurrentState currentState)
    {
        if (mBound) {
            mService.setCurrentState(currentState);
            currentState = mService.getCurrentState();
        }
        CurrentState finalCurrentState = currentState;
        activityContext.runOnUiThread(() -> {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !GpsOnAlertAlreadyInflated)
            {
                stopService();
                showSettingsAlert();
                GpsOnAlertAlreadyInflated = true;
            }
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && !GPSPermissionAlreadyInflated)
            {
                stopService();
                ActivityCompat.requestPermissions(activityContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
                GPSPermissionAlreadyInflated = true;
            }
            for (FrontElementManager frontManager: frontManagers) {
                frontManager.update(finalCurrentState);
            }
        });
        return currentState;
    }

    private void turnOffGpsPreferences()
    {
        logger.d("turnOffGpsPreferences start");
        SharedPreferencesHelper.INSTANCE.setSharedPreferencesBoolean(appContext,"speed_display",false);
        SharedPreferencesHelper.INSTANCE.setSharedPreferencesBoolean(appContext,"speed_control",false);
    }

    private void showSettingsAlert(){
        logger.d("showSettingsAlert start");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activityContext);
        alertDialog.setTitle(R.string.gps_settings);
        alertDialog.setMessage(R.string.gps_go_settings);
        alertDialog.setPositiveButton(R.string.settings, (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activityContext.startActivity(intent);
        });
        alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.cancel();
            turnOffGpsPreferences();
            CharSequence text = appContext.getString(R.string.disable_speed_features);
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(appContext, text, duration).show();
        });
        alertDialog.show();
    }

    public void clean() {
        logger.d("clean start");
        stopService();
    }
}