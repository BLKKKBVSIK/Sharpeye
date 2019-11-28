package sharpeye.sharpeye.Services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.math.BigDecimal;

import sharpeye.sharpeye.utils.CurrentState;
import sharpeye.sharpeye.GPS.GPSCallback;
import sharpeye.sharpeye.GPS.GPSManager;
import sharpeye.sharpeye.data.SharedPreferencesHelper;
import sharpeye.sharpeye.signs.BipGenerator;

public class GPSService extends Service implements GPSCallback {

    private final IBinder binder = new GPSBinder();
    private CurrentState currentState;
    private BipGenerator bipGenerator;
    private GPSManager gpsManager;
    private LocationManager locationManager;
    private long warningStopTime;
    private long warningDurationMS = 2500;
    private long nextTrigger;
    private long warningTriggerIntervalMS = 20000;
    private boolean canTriggerWarning = true;
    private float currSpeedLimit = 0;

    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        Log.d("GPS Service", "onStartCommand: GPS service starting");
        return Service.START_STICKY;
    }

    public class GPSBinder extends Binder {
        public GPSService getService() {
            return GPSService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onRebind (Intent intent)
    {
        super.onRebind(intent);
    }

    @Override
    public void onCreate ()
    {
        super.onCreate();
        Log.d("GPS Service", "onCreate: GPS service starting");
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_LOW);//avoid notification sound

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }
        currentState = new CurrentState();
        initializeGPS();
    }

    public void onSpeedTooBig() {
        if (canTriggerWarning) {
            warningStopTime = System.currentTimeMillis() + warningDurationMS;
            nextTrigger = System.currentTimeMillis() + warningTriggerIntervalMS;
        }
    }

    public void onSpeedChange() {
        canTriggerWarning = true;
    }

    @Override
    public void onDestroy ()
    {
        super.onDestroy();
        Log.d("GPS Service", "onDestroy: GPS service stops");
        cleanGPS();
    }

    @Override
    public void onGPSUpdate(Location location) {

        double speed = location.getSpeed() * 3.6f;
        currentState.setSpeed(round(speed, 3, BigDecimal.ROUND_HALF_UP));
        currentState.setSpeed(true);
        if (currentState != null && currentState.getSpeedLimit() != 0) {
            if (currentState.getSpeedLimit() != currSpeedLimit) {
                currSpeedLimit = currentState.getSpeedLimit();
                onSpeedChange();
            }
            if (currentState.getSpeed() > currentState.getSpeedLimit())
            {
                onSpeedTooBig();
                if (bipGenerator != null && System.currentTimeMillis() < warningStopTime) {
                    bipGenerator.bip(150, 100);
                } else if (bipGenerator != null && System.currentTimeMillis() > nextTrigger) {
                    warningStopTime = System.currentTimeMillis() + warningDurationMS;
                    nextTrigger = System.currentTimeMillis() + warningTriggerIntervalMS;
                }
            }
        }
        Log.d("GPS Service", "onGPSUpdate: " + speed);
    }

    private static double round(double unrounded, int precision, int roundingMode) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }

    public void initializeGPS(){
        Log.d("service initializeGPS", "start");
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                gpsManager = new GPSManager(this);

                currentState.setGPSenabled(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
                if (currentState.getGPSenabled()) {
                    gpsManager.startListening(this);
                    gpsManager.setGPSCallback(this);
                }
                if (SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(getApplicationContext(), "speed_control", false)) {
                    bipGenerator = new BipGenerator();
                } else {
                    bipGenerator = null;
                }
            } else {
                currentState.setGPSPermission(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("initializeGPS", "end");
    }

    private void cleanGPS() {
        Log.d("cleanGPS", "start");
        if (gpsManager != null) {
            gpsManager.stopListening();
            gpsManager.setGPSCallback(null);
            gpsManager = null;
        }
        if (bipGenerator != null)
        {
            bipGenerator = null;
        }
        Log.d("cleanGPS", "end");
    }

    public void setCurrentState(CurrentState _currentState)
    {
        double tmpSpeed = currentState.getSpeed();
        boolean tmpGPSEnabled = currentState.getGPSenabled();
        boolean tmpGPSPermission = currentState.getGPSPermission();
        currentState = _currentState;
        currentState.setSpeed(tmpSpeed);
        currentState.setGPSenabled(tmpGPSEnabled);
        currentState.setGPSPermission(tmpGPSPermission);
    }

    public CurrentState getCurrentState()
    {
        return currentState;
    }
}
