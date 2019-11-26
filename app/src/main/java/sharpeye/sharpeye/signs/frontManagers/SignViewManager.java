package sharpeye.sharpeye.signs.frontManagers;

import android.content.Context;
import android.util.Log;

import sharpeye.sharpeye.data.SharedPreferencesHelper;
import sharpeye.sharpeye.signs.frontViews.IFrontViews;
import sharpeye.sharpeye.utils.CurrentState;
import sharpeye.sharpeye.utils.Font;

/**
 * holds the logic associated with the speedsign views
 */
public class SignViewManager extends FrontElementManager {


    /**
     * called at the object creation
     *
     * @param _frontViews
     */
    public SignViewManager(Context _context, IFrontViews _frontViews, Boolean _isVisible) {
        super(_context, _frontViews, _isVisible);
        frontViews.setFont(Font.FontList.CHARACTERE);
    }

    /**
     * called for updating the front element
     * @param currentState instance of current state object
     */
    @Override
    public void update(CurrentState currentState) {
        boolean pref = getSharedPreferences();
        if (isVisible) {
            if (!pref) {
                frontViews.setInvisible();
                isVisible = false;
                return;
            }
        }
        else {
            if (pref) {
                frontViews.setVisible();
                isVisible = true;
            } else { return; }
        }
        if (currentState.isSpeedLimit()) {
            frontViews.setVisible();
            frontViews.setText(String.valueOf(currentState.getSpeedLimit()));
        } else {
            frontViews.setInvisible();
        }
    }

    /**
     * @return A boolean value if/not the feature is allowed
     */
    private Boolean getSharedPreferences()
    {
        return SharedPreferencesHelper.INSTANCE.getSharedPreferencesBoolean(context, "sign_display", false);
    }
}
