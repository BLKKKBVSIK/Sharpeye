package sharpeye.sharpeye.processors;

import java.util.ArrayList;

import sharpeye.sharpeye.utils.CurrentState;

public class ProcessorsManager {

    private ArrayList<DataProcessor> dataProcessors;

    public ProcessorsManager()
    {
        dataProcessors = new ArrayList<>();
    }

    public ProcessorsManager add(DataProcessor processor)
    {
        dataProcessors.add(processor);
        return this;
    }

    public void create()
    {
        for (DataProcessor processor :
             dataProcessors) {
            processor.create();
        }
    }

    public void resume(CurrentState currentState)
    {
        for (DataProcessor processor :
             dataProcessors) {
            processor.resume(currentState);
        }
    }

    public void pause(CurrentState currentState)
    {
        for (DataProcessor processor :
                dataProcessors) {
            processor.pause(currentState);
        }
    }

    public CurrentState process(CurrentState currentState)
    {
        for (DataProcessor processor :
                dataProcessors) {
            currentState = processor.process(currentState);
        }
        return currentState;
    }

    public void clean()
    {
        for (DataProcessor processor :
                dataProcessors) {
            processor.clean();
        }
    }
}
