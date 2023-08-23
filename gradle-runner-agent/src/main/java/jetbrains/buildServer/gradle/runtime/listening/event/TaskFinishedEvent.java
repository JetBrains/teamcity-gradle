package jetbrains.buildServer.gradle.runtime.listening.event;

public interface TaskFinishedEvent extends BuildEvent {
    EventResult getResult();
}
