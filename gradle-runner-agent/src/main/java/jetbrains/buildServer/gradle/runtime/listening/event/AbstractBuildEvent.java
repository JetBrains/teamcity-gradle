package jetbrains.buildServer.gradle.runtime.listening.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractBuildEvent implements BuildEvent {

    private final String myEventId;
    private final long myEventTimestamp;
    private final String myMessage;

    public AbstractBuildEvent(@Nullable String eventId, long eventTimestamp, @NotNull String message) {
        myEventId = eventId;
        myEventTimestamp = eventTimestamp;
        myMessage = message;
    }

    @Override
    @Nullable
    public String getId() {
        return myEventId;
    }

    @Override
    public long getEventTimestamp() {
        return myEventTimestamp;
    }

    @Override
    @NotNull
    public String getMessage() {
        return myMessage;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
               "myEventId=" + myEventId +
               ", myEventTime=" + myEventTimestamp +
               ", myMessage='" + myMessage + '\'' +
               '}';
    }
}
