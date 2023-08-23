package jetbrains.buildServer.gradle.runtime.listening;

import jetbrains.buildServer.gradle.runtime.listening.event.BuildEvent;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLogger;
import jetbrains.buildServer.gradle.runtime.service.GradleProgressEventConverter;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;

/**
 * Listens to events from the Gradle Tooling API
 */
public class GradleToolingApiProgressListener implements ProgressListener {

    private final BuildLifecycleListener myListener;
    private final GradleToolingLogger myLogger;
    private final String myOperationId;

    public GradleToolingApiProgressListener(BuildLifecycleListener buildLifecycleListener,
                                            GradleToolingLogger logger,
                                            String buildNumber) {
        myListener = buildLifecycleListener;
        myLogger = logger;
        myOperationId = String.format("GradleTAPIBuild-%s", buildNumber);
    }

    @Override
    public void statusChanged(ProgressEvent event) {
        BuildEvent buildEvent = GradleProgressEventConverter.createTaskNotificationEvent(event, myOperationId);
        if (buildEvent != null) {
            myListener.onStatusChange(buildEvent);
        } else {
            myLogger.warn("Unknown Gradle Tooling API event: " + event.getClass().getSimpleName() + " " + event);
        }
    }
}