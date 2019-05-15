package sharpeye.sharpeye.warning;

import android.os.SystemClock;
import android.support.annotation.NonNull;

public class Event {

    private String  eventText;
    private int     delay;
    private long    lastTrigger;


    public Event(@NonNull String eventText, int delay) {
        this.eventText = eventText;
        this.delay = delay;
        lastTrigger = 0;
    }

    public void triggerEventIfTimeLimitReached(@NonNull Speech speech) {
        // Get the elapsed time in second
        long actualTime = SystemClock.elapsedRealtime() / 1000;
        if ((actualTime - lastTrigger) >= delay) {
            speech.speak(eventText);
            lastTrigger = actualTime;
        }
    }
}
