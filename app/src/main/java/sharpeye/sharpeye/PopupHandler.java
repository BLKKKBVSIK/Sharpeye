package sharpeye.sharpeye;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sharpeye.BooleanKeyValueModel;
import sharpeye.sharpeye.data.DBHelper;
import sharpeye.sharpeye.utils.Json;
import sharpeye.sharpeye.utils.PopUpFactory;
import sharpeye.sharpeye.utils.Str;

public class PopupHandler {

    private Context context;
    private List<PopUpFactory> popups;
    private List<String> labels;
    private DBHelper database;

    PopupHandler (Context _context, String jsonFile, DBHelper _database)
    {
        context = _context;
        database = _database;
        popups = new ArrayList<>();
        labels = new ArrayList<>();
        LoadJson(jsonFile);
    }

    private void LoadJson(String jsonFile)
    {

        try {
            Log.d("PopupHandler LoadJson", "init: start loading");
            JSONArray tempPopups = Json.getJsonArray(context, jsonFile + ".json", jsonFile);
            if (tempPopups.length() > 0)
            {
                for(int i = 0; i < tempPopups.length(); i++)
                {
                    PopUpFactory tmp = new PopUpFactory(context);
                    JSONObject tempPopup = tempPopups.getJSONObject(i);
                    String label = tempPopup.getString("label");
                    ArrayList<BooleanKeyValueModel> kv = database.read(label);
                    Log.d("PopupHandler LoadJson", kv.toString());
                    if (kv.isEmpty() || (!kv.isEmpty() && !kv.get(0).getValue()))
                    {
                        String title = tempPopup.getString("title");
                        if (!Str.IsNullOrEmpty(title))
                            tmp.setTitle(title);
                        String message = tempPopup.getString("message");
                        if (!Str.IsNullOrEmpty(message))
                            tmp.setMessage(message);
                        String negative = tempPopup.getString("negative");
                        if (!Str.IsNullOrEmpty(negative))
                            tmp.setNegativeButton(negative , () -> {});
                        String positive = tempPopup.getString("positive");
                        if (!Str.IsNullOrEmpty(positive))
                            tmp.setPositiveButton(positive, () -> NextPopup(1));
                        else
                            tmp.setPositiveButton("Next", () -> NextPopup(1));

                        labels.add(label);
                        popups.add(tmp);
                    }
                    Log.d("PopupHandler LoadJson", "for i: " + i);
                }
            }
            Log.d("PopupHandler LoadJson", "init: end loading");
        } catch (IOException | JSONException ex) {
            Log.e("PopupHandler LoadJson", ex.toString());
        }
    }

    public void NextPopup(int factor)
    {
        Log.d("PopupHandler NextPopup", "start");
        if (!labels.isEmpty()) {
            if (factor == 1) {
                database.insert(new BooleanKeyValueModel(labels.get(0), true));
                labels.remove(0);
            }
        }
        if (!popups.isEmpty()) {
            Log.d("PopupHandler NextPopup", "is not empty");
            PopUpFactory tmp = popups.get(0);
            popups.remove(0);
            tmp.show();
        }
        Log.d("PopupHandler NextPopup", "end");
        return;
    }
}
