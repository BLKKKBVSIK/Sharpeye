package sharpeye.sharpeye.data

import android.content.Context
import android.preference.PreferenceManager


object SharedPreferencesHelper {
    private val PREF_FILE = "PREF"

    /**
     * Set a string shared preference
     * @param key - Key to set shared preference
     * @param value - Value for the key
     */
    fun setSharedPreferencesString(context: Context, key: String, value: String) {
        //val settings = context.getSharedPreferences(PREF_FILE, 0)
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        editor.putString(key, value)
        editor.apply()
    }

    /**
     * Set a integer shared preference
     * @param key - Key to set shared preference
     * @param value - Value for the key
     */
    fun setSharedPreferencesInt(context: Context, key: String, value: Int) {
        //val settings = context.getSharedPreferences(PREF_FILE, 0)
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    /**
     * Set a Boolean shared preference
     * @param key - Key to set shared preference
     * @param value - Value for the key
     */
    fun setSharedPreferencesBoolean(context: Context, key: String, value: Boolean) {
        //val settings = context.getSharedPreferences(PREF_FILE, 0)
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    /**
     * Get a string shared preference
     * @param key - Key to look up in shared preferences.
     * @param defValue - Default value to be returned if shared preference isn't found.
     * @return value - String containing value of the shared preference if found.
     */
    fun getSharedPreferencesString(context: Context, key: String, defValue: String): String? {
        //val settings = context.getSharedPreferences(PREF_FILE, 0)
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        return settings.getString(key, defValue)
    }

    /**
     * Get a integer shared preference
     * @param key - Key to look up in shared preferences.
     * @param defValue - Default value to be returned if shared preference isn't found.
     * @return value - String containing value of the shared preference if found.
     */
    fun getSharedPreferencesInt(context: Context, key: String, defValue: Int): Int {
        //val settings = context.getSharedPreferences(PREF_FILE, 0)
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        return settings.getInt(key, defValue)
    }

    /**
     * Get a boolean shared preference
     * @param key - Key to look up in shared preferences.
     * @param defValue - Default value to be returned if shared preference isn't found.
     * @return value - String containing value of the shared preference if found.
     */
    fun getSharedPreferencesBoolean(context: Context, key: String, defValue: Boolean): Boolean {
        //val settings = context.getSharedPreferences(PREF_FILE, 0)
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        return settings.getBoolean(key, defValue)
    }
}