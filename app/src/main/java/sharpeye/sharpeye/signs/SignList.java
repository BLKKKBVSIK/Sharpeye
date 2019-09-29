package sharpeye.sharpeye.signs;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import sharpeye.sharpeye.utils.Json;

public class SignList {

    private Map<String ,Sign> signList;

    public SignList (Context context) { init(context);}

    private void init(Context context) {
        try {
            // TODO add integrity verification
            Log.d("SignList", "init: debut");
            InputStream inputStream = context.getAssets().open("traffic_signs.json");
            JSONObject jsonObject = Json.getJsonFromInputStream(inputStream);
            JSONArray trafficSigns = jsonObject.getJSONArray("traffic_signs");
            Log.d("SignList", "init: loaded json");
            if (trafficSigns.length() > 0) {
                signList = new HashMap<>();
                for (int i = 0; i < trafficSigns.length(); i++) {
                    JSONObject trafficSign = trafficSigns.getJSONObject(i);
                    String name = trafficSign.getString("name");
                    //Log.d("SignList", "name: " + name);
                    int id = trafficSign.getInt("id");
                    //Log.d("SignList", "id: " + id);
                    String kind = trafficSign.getString("kind");
                    //Log.d("SignList", "kind: " + kind);
                    int speed = trafficSign.getInt("speed");
                    //Log.d("SignList", "speed: " + speed);
                    String additionalInfo = trafficSign.getString("additionalInfo");
                    //Log.d("SignList", "additionalInfo: " + additionalInfo);
                    signList.put(name, new Sign(name, id, SignKind.valueOf(kind), speed, additionalInfo));
                }
            }
            Log.d("SignList", "init: end loading");
            /*for (Sign elem: signList.values()) {
                Log.d("SignList", elem.getName() + " / " + elem.getId() + " / " + elem.getSpeed()  + " / " + elem.getAdditionalInfos());
            }*/
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
