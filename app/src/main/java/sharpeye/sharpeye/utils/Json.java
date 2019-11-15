package sharpeye.sharpeye.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Adds functions to deal with Json
 */
public class Json {

    /**
     * Load a Json object from a Stream
     *
     * @param inputStream the stream containing the Json file.
     */
    public static JSONObject getJsonFromInputStream(InputStream inputStream) throws JSONException, IOException {

        Log.d("JsonInputStream", "getJsonFromInputStream: start");
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        bufferedReader.close();
        Log.d("JsonInputStream", "getJsonFromInputStream: end");

        return (new JSONObject(stringBuilder.toString()));
    }

    /**
     * Load a Json array from a json file
     *
     * @param context the context.
     * @param fileName Name of the Json file.
     * @param arrayName Name of the loaded array in the json file.
     */
    public static JSONArray getJsonArray(Context context, String fileName, String arrayName) throws JSONException, IOException {

        Log.d("JsonArray", "getJsonArray: start");

        InputStream inputStream = context.getAssets().open(fileName);
        JSONObject jsonObject = getJsonFromInputStream(inputStream);
        JSONArray trafficSigns = jsonObject.getJSONArray(arrayName);
        inputStream.close();
        Log.d("JsonArray", "getJsonArray: start");

        return trafficSigns;
    }
}
