package sharpeye.sharpeye.signs

import android.media.AudioManager
import android.media.ToneGenerator

class BipGenerator
{
    fun bip(duration : Int, volume : Int)
    {
        val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, duration)
    }
}