package sharpeye.sharpeye.GPS;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.List;

public class GPSManager implements android.location.GpsStatus.Listener
{
    private static final int gpsMinTime = 500;
    private static final int gpsMinDistance = 0;
    private static LocationManager locationManager = null;
    private static LocationListener locationListener = null;
    private static GPSCallback gpsCallback = null;
    Context mcontext;
    public GPSManager(Context context) {
        mcontext=context;
        GPSManager.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                Log.d("GPS onLocationChanged", "onLocationChanged callback");
                if (GPSManager.gpsCallback != null) {
                    GPSManager.gpsCallback.onGPSUpdate(location);
                }
            }
            @Override
            public void onProviderDisabled(final String provider) {
            }
            @Override
            public void onProviderEnabled(final String provider) {
            }
            @Override
            public void onStatusChanged(final String provider, final int status, final Bundle extras) {
            }
        };
    }
    public GPSCallback getGPSCallback()
    {
        return GPSManager.gpsCallback;
    }
    public void setGPSCallback(final GPSCallback gpsCallback) {
        GPSManager.gpsCallback = gpsCallback;
    }
    public void startListening(final Context context) {
        if (GPSManager.locationManager == null) {
            GPSManager.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        final String bestProvider = GPSManager.locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
            if (bestProvider != null && bestProvider.length() > 0) {
                GPSManager.locationManager.requestLocationUpdates(bestProvider, GPSManager.gpsMinTime,
                        GPSManager.gpsMinDistance, GPSManager.locationListener);
            }
            else {
                final List<String> providers = GPSManager.locationManager.getProviders(true);
                for (final String provider : providers)
                {
                    GPSManager.locationManager.requestLocationUpdates(provider, GPSManager.gpsMinTime,
                            GPSManager.gpsMinDistance, GPSManager.locationListener);
                }
            }
        }
        Log.i("GPS Manager startListening", "end");
    }

    public void stopListening() {
        try
        {
            if (GPSManager.locationManager != null && GPSManager.locationListener != null) {
                GPSManager.locationManager.removeUpdates(GPSManager.locationListener);
            }
            GPSManager.locationManager = null;
        }
        catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onGpsStatusChanged(int event) {
        int Satellites = 0;
        int SatellitesInFix = 0;
        if (ActivityCompat.checkSelfPermission(mcontext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mcontext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            int timeToFix = locationManager.getGpsStatus(null).getTimeToFirstFix();
            Log.i("GPS Manager onGpsStatusChanged", "Time to first fix = "+ timeToFix);
            for (GpsSatellite sat : locationManager.getGpsStatus(null).getSatellites()) {
                if(sat.usedInFix()) {
                    SatellitesInFix++;
                }
                Satellites++;
            }
        }
        Log.i("GPS Manager onGpsStatusChanged", Satellites + " Used In Last Fix ("+SatellitesInFix+")");
    }
}