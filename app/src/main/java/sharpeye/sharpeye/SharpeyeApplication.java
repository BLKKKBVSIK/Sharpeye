package sharpeye.sharpeye;

import android.app.Application;
import android.preference.PreferenceManager;

public class SharpeyeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.pref_dangers, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_dashcam, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_signs, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_vocal, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, true);
    }
}
