package sharpeye.sharpeye.utils

import android.util.Log
import sharpeye.sharpeye.signs.Sign
import sharpeye.sharpeye.signs.SignKind

//Made to log all the current state of the user/vehicule
public class CurrentState {
    var speed : Double = 0.0
    var speedLimit :Int = 0
    var previousSigns : MutableList<Sign> = mutableListOf()
    var GPSenabled : Boolean = true
    var GPSPermission : Boolean = true

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