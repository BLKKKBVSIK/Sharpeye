package sharpeye.sharpeye.objects_logic;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import sharpeye.sharpeye.utils.Json;

public class WarningEvent {

    private HashMap<String, Event> warningSpeeches;
    private Speech speech;

    public WarningEvent(Context context) {
        init(context);
    }

    public void init(Context context) {
        speech = new Speech(context);
        try {
            // TODO add integrity verification
            JSONArray warningEvents = Json.getJsonArray(context, "warning_events.json", "warning_events");
            if (warningEvents.length() > 0) {
                warningSpeeches = new HashMap<>();
                for (int i = 0; i < warningEvents.length(); i++) {
                    JSONObject warningEvent = warningEvents.getJSONObject(i);
                    String name = warningEvent.getString("name");
                    String text = warningEvent.getString("text");
                    int delay = warningEvent.getInt("delay");
                    warningSpeeches.put(name, new Event(text, delay));
                }
            }
        } catch (IOException | JSONException ex) {
            Log.e("WarningEvent", ex.toString());
            warningSpeeches = null;
        }
    }

    public boolean isTTSAvailable() {
        return speech.isAvailable();
    }

    public void clean() {
        speech.clean();
        speech = null;
        warningSpeeches = null;
    }

    public void triggerWarning(String warning) {
        if (speech != null && warningSpeeches != null && warningSpeeches.containsKey(warning)) {
            Event event = warningSpeeches.get(warning);
            try {
                event.triggerEventIfTimeLimitReached(speech);
            } catch (NullPointerException ex) {
                Log.e("WarningEvent", ex.toString());
            }
        }
    }
}
