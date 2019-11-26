package sharpeye.sharpeye.processors;

import java.util.ArrayList;

import sharpeye.sharpeye.utils.CurrentState;
import sharpeye.sharpeye.utils.Logger;

/**
 * Handles multiple processors
 */
public class ProcessorsManager {

    private ArrayList<DataProcessor> dataProcessors;
    private Logger logger;

    /**
     * Constructor
     */
    public ProcessorsManager(Logger _logger)
    {
        logger = _logger;
        dataProcessors = new ArrayList<>();
    }

    /**
     * Adds a processor to a list of processor
     * the calls cans be chained as following
     * => .add(firstProcessor).add(secondProcessor)
     * @param processor the processor the add
     * @see DataProcessor
     * @return itself
     */
    public ProcessorsManager add(DataProcessor processor)
    {
        dataProcessors.add(processor);
        logger.i("processor added");
        return this;
    }

    /**
     * To call with the onCreate of an activity
     */
    public void create()
    {
        logger.i("create");
        for (DataProcessor processor :
             dataProcessors) {
            processor.create();
        }
    }

    /**
     * To call with the onResume of an activity
     * @param currentState the currentState
     */
    public void resume(CurrentState currentState)
    {
        logger.i("resume");
        for (DataProcessor processor :
             dataProcessors) {
            processor.resume(currentState);
        }
    }

    /**
     * To call with the onPause of an activity
     * @param currentState the currentState
     */
    public void pause(CurrentState currentState)
    {
        logger.i("pause");
        for (DataProcessor processor :
                dataProcessors) {
            processor.pause(currentState);
        }
    }

    /**
     * To call at refresh of the activity
     * @param currentState the current state
     * @return the current state
     */
    public CurrentState process(CurrentState currentState)
    {
        for (DataProcessor processor :
                dataProcessors) {
            currentState = processor.process(currentState);
        }
        return currentState;
    }

    /**
     * To call with the onDestroy of an activity
     */
    public void clean()
    {
        logger.i("clean");
        for (DataProcessor processor :
                dataProcessors) {
            processor.clean();
        }
    }
}
