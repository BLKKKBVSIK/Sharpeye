package sharpeye.sharpeye.utils

import android.util.Log
import sharpeye.sharpeye.signs.Sign
import sharpeye.sharpeye.signs.SignKind
import kotlin.math.E

//Made to log all the current state of the user/vehicule
public class CurrentState {
    var speed : Double = 0.0
    var speedLimit :Int = 1000
    var isSpeedLimit : Boolean = false
    var previousSigns : MutableList<Sign> = mutableListOf()
    var GPSenabled : Boolean = true
    var GPSPermission : Boolean = true

    fun addSign(sign : Sign){

        if (sign.kind == SignKind.SPEEDLIMIT)
        {
            if (previousSigns.any() && previousSigns[previousSigns.lastIndex] == sign)
            {
                speedLimit = sign.speed
                isSpeedLimit = true
            }
            previousSigns.add(sign)
            Log.d("current state",
                "speedlimit: $speedLimit / currentSpeed: $speed + $previousSigns"
            )
        }
        if (previousSigns.size >= 10)
        {
            //add to db for stats
            previousSigns.removeAt(0)
        }
    }
}