package sharpeye.sharpeye.signs.frontManagers;

import android.content.Context;

import sharpeye.sharpeye.signs.frontViews.IFrontViews;
import sharpeye.sharpeye.utils.CurrentState;

/**
 * Abstract class to create FrontElements
 */
public abstract class FrontElementManager {

    protected IFrontViews frontViews = null;
    protected boolean isVisible = false;
    protected Context context;

    /**
     * called at the object creation
     */
    public FrontElementManager(Context _context, IFrontViews _frontViews, Boolean _isVisible)
    {
        frontViews = _frontViews;
        isVisible = _isVisible;
        context = _context;
    }

    /**
     * called for updating the front element
     * @param currentState instance of current state object
     */
    public abstract void update(CurrentState currentState);

}
