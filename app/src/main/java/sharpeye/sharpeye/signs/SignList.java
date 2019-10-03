package sharpeye.sharpeye.signs;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

import sharpeye.sharpeye.utils.Json;

public class SignList {

    private Map<String ,Sign> signList;

    public SignList (Context context) { init(context);}

    private void init(Context context) {
        try {
            Log.d("SignList", "init: start loading");
            JSONArray trafficSigns = Json.getJsonArray(context, "traffic_signs.json", "traffic_signs");
            if (trafficSigns.length() > 0) {
                signList = new HashMap<>();
                for (int i = 0; i < trafficSigns.length(); i++) {
                    JSONObject trafficSign = trafficSigns.getJSONObject(i);
                    String name = trafficSign.getString("name");
                    int id = trafficSign.getInt("id");
                    String kind = trafficSign.getString("kind");
                    int speed = trafficSign.getInt("speed");
                    String additionalInfo = trafficSign.getString("additionalInfo");
                    signList.put(name, new Sign(name, id, SignKind.valueOf(kind), speed, additionalInfo));
                }
            }
            Log.d("SignList", "init: end loading");
        } catch (IOException | JSONException ex) {
            Log.e("SignList", ex.toString());
            signList = null;
        }
    }

    public Sign get(String key)
    {
        Sign ret = signList.get(key);
        return ret;
    }
}
