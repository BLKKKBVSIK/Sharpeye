package sharpeye.sharpeye

import android.location.Location

interface GPSCallback {
    fun onGPSUpdate(location: Location)
}