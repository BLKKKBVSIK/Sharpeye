package sharpeye.sharpeye.GPS

import android.location.Location

interface GPSCallback {
    fun onGPSUpdate(location: Location)
}