package jetbrains.buildServer.gradle.runtime.listening.event;

import org.jetbrains.annotations.NotNull;

public class FailureResultImpl implements FailureResult {

    private final FailureKind myFailureKind;

    public FailureResultImpl(FailureKind failureKind) {
        myFailureKind = failureKind;
    }

    @Override
    @NotNull
    public FailureKind getFailureKind() {
        return myFailureKind;
    }
}
