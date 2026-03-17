package jetbrains.buildServer.gradle.runtime.listening.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Common interface for events during the build process
 */
public interface BuildEvent {

    /**
     * Returns an id that uniquely identifies the event.
     *
     * @return The event id.
     */
    @Nullable
    String getId();


    /**
     * Returns the time this event was triggered.
     *
     * @return The event time, in milliseconds since the epoch.
     */
    long getEventTimestamp();

    /**
     * Returns textual representation of the event.
     *
     * @return The event text message.
     */
    @NotNull
    String getMessage();
}

