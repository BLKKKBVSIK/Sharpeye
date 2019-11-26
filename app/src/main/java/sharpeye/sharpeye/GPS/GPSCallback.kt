package sharpeye.sharpeye.GPS

import android.location.Location

/**
 * CallBack for gps
 */
interface GPSCallback {
    /**
     * Called on GPS Update
     */
    fun onGPSUpdate(location: Location)
}