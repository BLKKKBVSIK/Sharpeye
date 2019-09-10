package sharpeye.sharpeye

import android.util.Log
import sharpeye.sharpeye.signs.Sign
import sharpeye.sharpeye.signs.SignKind

//Made to log all the current state of the user/vehicule
public class CurrentState {
    var speed : Double = 0.0
    var speedLimit :Int = 0
    var previousSigns : MutableList<Sign> = mutableListOf()

    /*fun addSign(sign : TrafficSign){
        previousSigns.add(sign)
        if (sign.toString().contains("SPEEDLIMIT"))
        {
            speedLimit = when(sign) {
                TrafficSign.SPEEDLIMIT30 -> 30
                TrafficSign.SPEEDLIMIT50 -> 50
                TrafficSign.SPEEDLIMIT70 -> 70
                TrafficSign.SPEEDLIMIT80 -> 80
                TrafficSign.SPEEDLIMIT90 -> 90
                TrafficSign.SPEEDLIMIT110 -> 110
                TrafficSign.SPEEDLIMIT130 -> 130
                else -> throw IndexOutOfBoundsException("Speed limit unknown")
            }
            Log.e(speed.toString() + " : " + speedLimit.toString(), speed.toString() + " : " + speedLimit.toString());
        }
        if (previousSigns.size >= 10)
        {
            //add to db for stats
            previousSigns.removeAt(9)
        }
    }*/

    fun addSign(sign : Sign){
        previousSigns.add(sign)
        if (sign.kind == SignKind.SPEEDLIMIT)
        {
            speedLimit = sign.speed
            Log.e("current state", "speedlimit: $speedLimit / currentSpeed: $speed")
        }
        if (previousSigns.size >= 10)
        {
            //add to db for stats
            previousSigns.removeAt(9)
        }
    }
}