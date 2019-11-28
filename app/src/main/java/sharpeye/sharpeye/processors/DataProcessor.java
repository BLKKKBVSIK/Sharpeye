package sharpeye.sharpeye.processors;

import android.app.Activity;
import android.content.Context;

import sharpeye.sharpeye.utils.CurrentState;
import sharpeye.sharpeye.utils.Logger;

/**
 * Abstract to create a Processor
 */
public abstract class DataProcessor {

    protected Context appContext;
    protected Activity activityContext;
    protected Logger logger;

    /**
     * Constructor to be overwritten
     * @param _appContext context of the app
     * @param _activityContext context of the activity
     */
    public DataProcessor(Context _appContext, Activity _activityContext, Logger _logger)
    {
        appContext = _appContext;
        activityContext = _activityContext;
        logger = _logger;
    }

    /**
     * Create method to be overwritten
     * recreate an onCreate
     */
    public abstract void create();

    /**
     * Resume method to be overwritten
     * @param currentState the currentState
     */
    public abstract void resume(CurrentState currentState);

    /**
     * Pause method to be overwritten
     * @param currentState the currentState
     */
    public abstract void pause(CurrentState currentState);

    /**
     * CurrentState method to be overwritten
     * @param currentState the currentState
     */
    public abstract CurrentState process(CurrentState currentState);

    /**
     * clean method to be overwritten
     */
    public abstract void clean();
}
