package jetbrains.buildServer.gradle.runtime.listening.event;

import org.jetbrains.annotations.NotNull;

public class TaskStartedEventImpl extends AbstractBuildEvent implements TaskStartedEvent {

    public TaskStartedEventImpl(@NotNull String eventId, long eventTimestamp, @NotNull String message) {
        super(eventId, eventTimestamp, message);
    }
}