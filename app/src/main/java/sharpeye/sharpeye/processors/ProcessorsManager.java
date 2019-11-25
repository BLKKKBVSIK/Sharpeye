package sharpeye.sharpeye.processors;

import java.util.ArrayList;

import sharpeye.sharpeye.utils.CurrentState;

/**
 * Handles multiple processors
 */
public class ProcessorsManager {

    private ArrayList<DataProcessor> dataProcessors;

    /**
     * Constructor
     */
    public ProcessorsManager()
    {
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
        return this;
    }

    /**
     * To call with the onCreate of an activity
     */
    public void create()
    {
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
        for (DataProcessor processor :
                dataProcessors) {
            processor.clean();
        }
    }
}
