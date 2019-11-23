package sharpeye.sharpeye.processors;

import android.app.Activity;
import android.content.Context;

import sharpeye.sharpeye.utils.CurrentState;

public abstract class DataProcessor {

    protected Context appContext;
    protected Activity activityContext;

    public DataProcessor(Context _appContext, Activity _activityContext)
    {
        appContext = _appContext;
        activityContext = _activityContext;
    }

    public abstract void create();

    public abstract void resume(CurrentState currentState);

    public abstract void pause(CurrentState currentState);

    public abstract CurrentState process(CurrentState currentState);

    public abstract void clean();
}
