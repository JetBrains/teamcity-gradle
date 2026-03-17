package jetbrains.buildServer.gradle.runtime.listening.event;

public class TaskFinishedEventImpl extends AbstractBuildEvent implements TaskFinishedEvent {

    private final EventResult myResult;

    public TaskFinishedEventImpl(String eventId,
                                 long eventTimestamp,
                                 String message,
                                 EventResult result) {
        super(eventId, eventTimestamp, message);
        myResult = result;
    }

    @Override
    public EventResult getResult() {
        return myResult;
    }
}
