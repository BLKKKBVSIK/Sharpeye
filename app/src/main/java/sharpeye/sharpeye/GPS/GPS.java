package sharpeye.sharpeye.GPS;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView textview;
    private GPSService mService;
    private boolean mBound = false;
    private LocationManager locationManager;

    public GPS(Context _context, TextView _textview)
    {
        context = _context;
        textview = _textview;
    }

    public void create()
    {
        textview.setVisibility(View.VISIBLE);
        locationManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context,"speed_display",false)) {
            initializeGPS();
        }
    }

    public void resume(CurrentState currentState)
    {
        Log.d("gpsresume", "start");
        if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context,"speed_display",false)) {
            textview.setVisibility(View.VISIBLE);
            textview.setText(context.getString(R.string.speed_counter));
            if (mBound) {
                mService.setCurrentState(currentState);
                currentState = mService.getCurrentState();
            }
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  && GpsOnAlertAlreadyInflated) {
                turnOffGpsPreferences();
                stopService();
                textview.setVisibility(View.INVISIBLE);
                Log.d("gpsresume", "gps not enabled");
            }
            else if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && GPSPermissionAlreadyInflated)
            {
                turnOffGpsPreferences();
                stopService();
                textview.setVisibility(View.INVISIBLE);
                Log.d("gpsresume", "unauthorized");
            } else if (!ServiceTools.isServiceRunning("GPSService", context)) {
                startService();
                textview.setVisibility(View.VISIBLE);
                Log.d("gpsresume", "restart service");
            }
        }
        else {
            textview.setVisibility(View.INVISIBLE);//TODO check tous les droits.
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
        textview.setText(context.getString(R.string.speed_counter));
        Log.d("gpsinitializeGPS", "end");
    }

    public CurrentState process(CurrentState currentState, Activity activity)
    {
        if (mBound) {
            mService.setCurrentState(currentState);
            currentState = mService.getCurrentState();
        }
        CurrentState finalCurrentState = currentState;
        Log.d("GPS process", "gps enables: " + currentState.getGPSenabled() + " " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) +
                "\nPermission: " + ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION));
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
            } else if (finalCurrentState.getGPSenabled() && finalCurrentState.getGPSPermission()) {
                textview.setText(finalCurrentState.getSpeed() +"km/h");
                if (finalCurrentState.getSpeed() > finalCurrentState.getSpeedLimit()) {
                    textview.setTextColor(Color.rgb(255, 0, 0));
                } else if (finalCurrentState.getSpeed() >= finalCurrentState.getSpeedLimit() * 0.95) {
                    textview.setTextColor(Color.rgb(255, 165, 0));
                } else {
                    textview.setTextColor(Color.rgb(255, 255, 255));
                }
            }
        });
        return currentState;
    }

    private void turnOffGpsPreferences()
    {
        SharedPreferencesHelper.INSTANCE.setSharedPreferencesBoolean(context,"speed_display",false);
        SharedPreferencesHelper.INSTANCE.setSharedPreferencesBoolean(context,"speed_control",false);
        textview.setVisibility(View.INVISIBLE);
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