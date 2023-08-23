package jetbrains.buildServer.gradle.runtime.listening.event;

import org.jetbrains.annotations.NotNull;

public interface FailureResult extends EventResult {

    @NotNull
    FailureKind getFailureKind();
}
