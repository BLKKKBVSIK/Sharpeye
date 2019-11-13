package sharpeye.sharpeye.GPS;

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
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import sharpeye.sharpeye.signs.frontManagers.FrontElementManager;
import sharpeye.sharpeye.utils.CurrentState;
import sharpeye.sharpeye.R;
import sharpeye.sharpeye.Services.GPSService;
import sharpeye.sharpeye.data.SharedPreferencesHelper;
import sharpeye.sharpeye.utils.ServiceTools;

public class GPS {

    private boolean GpsOnAlertAlreadyInflated = false;
    private boolean GPSPermissionAlreadyInflated = false;
    private Intent i;
    private Context context;

    private GPSService mService;
    private boolean mBound = false;
    private LocationManager locationManager;

    private List<FrontElementManager> frontManagers;

    public GPS(Context _context, List<FrontElementManager> _frontManagers)
    {
        context = _context;
        frontManagers = _frontManagers;
    }

    public void create()
    {
        locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context,"speed_display",false)) {
            initializeGPS();
        }
    }

    public void resume(CurrentState currentState)
    {
        Log.d("gpsresume", "start");
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context,"speed_display",false)) {
            if (mBound) {
                mService.setCurrentState(currentState);
                currentState = mService.getCurrentState();
            }
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  && GpsOnAlertAlreadyInflated) {
                turnOffGpsPreferences();
                stopService();
                Log.d("gpsresume", "gps not enabled");
            }
            else if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && GPSPermissionAlreadyInflated)
            {
                turnOffGpsPreferences();
                stopService();
                Log.d("gpsresume", "unauthorized");
            } else if (!ServiceTools.isServiceRunning("GPSService", context)) {
                startService();
                Log.d("gpsresume", "restart service");
            }
        }
        else {
            stopService();
        }
        Log.d("gpsresume", "end");
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
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
        Log.d("gpsstartService", "start");
        i= new Intent(context, GPSService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
        Intent intent = new Intent(context, GPSService.class);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Log.d("gpsstartService", "stop");
    }

    private void stopService()
    {
        Log.d("gpsstopService", "start");
        if (mService != null && mBound) {
            context.unbindService(connection);
            mBound = false;
            context.stopService(i);
        }
        Log.d("gpsstopService", "stop");
    }

    public void initializeGPS(){
        Log.d("gpsinitializeGPS", "start");
        startService();
        Log.d("gpsinitializeGPS", "end");
    }

    public CurrentState process(CurrentState currentState, Activity activity)
    {
        if (mBound) {
            mService.setCurrentState(currentState);
            currentState = mService.getCurrentState();
        }

        Log.d("GPS process", "gps enables: " + currentState.getGPSenabled() + " " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) +
                "\nPermission: " + ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION));

        CurrentState finalCurrentState = currentState;
        activity.runOnUiThread(() -> {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !GpsOnAlertAlreadyInflated)
            {
                stopService();
                showSettingsAlert();
                GpsOnAlertAlreadyInflated = true;
            }
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && !GPSPermissionAlreadyInflated)
            {
                stopService();
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
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
        SharedPreferencesHelper.INSTANCE.setSharedPreferencesBoolean(context,"speed_display",false);
        SharedPreferencesHelper.INSTANCE.setSharedPreferencesBoolean(context,"speed_control",false);
    }

    private void showSettingsAlert(){
        Log.d("showSettingsAlert", "start");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(R.string.gps_settings);
        alertDialog.setMessage(R.string.gps_go_settings);
        alertDialog.setPositiveButton(R.string.settings, (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            context.startActivity(intent);
        });
        alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.cancel();
            turnOffGpsPreferences();
            CharSequence text = context.getString(R.string.disable_speed_features);
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
        });
        alertDialog.show();
        Log.d("showSettingsAlert", "end");
    }

    public void clean() {
        Log.d("cleanGPS", "start");
        stopService();
        Log.d("cleanGPS", "end");
    }
}