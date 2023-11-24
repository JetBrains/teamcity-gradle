package jetbrains.buildServer.gradle.agent.gradleOptions;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleOptionValueFetchingParameters {

  @NotNull
  private final List<String> gradleTasks;
  @NotNull
  private final List<String> gradleParams;
  @Nullable
  private final File gradleUserHome;
  @NotNull
  private final File projectDirectory;
  @NotNull
  private final GradleOptionType gradleOptionType;
  @NotNull
  private final Collection<String> optionNames;
  @NotNull
  private final Collection<String> optionDisablingNames;
  @NotNull
  private final Collection<String> gradlePropertiesOptionNames;

  private GradleOptionValueFetchingParameters(@NotNull List<String> gradleTasks,
                                              @NotNull List<String> gradleParams,
                                              @Nullable File gradleUserHome,
                                              @NotNull File projectDirectory,
                                              @NotNull GradleOptionType gradleOptionType,
                                              @NotNull Collection<String> optionNames,
                                              @Nullable Collection<String> optionDisablingNames,
                                              @NotNull Collection<String> gradlePropertiesOptionNames) {
    this.gradleTasks = gradleTasks;
    this.gradleParams = gradleParams;
    this.gradleUserHome = gradleUserHome;
    this.projectDirectory = projectDirectory;
    this.gradleOptionType = gradleOptionType;
    this.optionNames = Collections.unmodifiableCollection(optionNames);
    this.optionDisablingNames = optionDisablingNames != null ? Collections.unmodifiableCollection(optionDisablingNames)
                                                             : Collections.emptyList();
    this.gradlePropertiesOptionNames = gradlePropertiesOptionNames;
  }

  @NotNull
  public List<String> getGradleTasks() {
    return gradleTasks;
  }

  @NotNull
  public List<String> getGradleParams() {
    return gradleParams;
  }

  @NotNull
  public Optional<File> getGradleUserHome() {
    return Optional.ofNullable(gradleUserHome);
  }

  @NotNull
  public File getProjectDirectory() {
    return projectDirectory;
  }

  @NotNull
  public GradleOptionType getGradleOptionType() {
    return gradleOptionType;
  }

  @NotNull
  public Collection<String> getOptionNames() {
    return optionNames;
  }

  @NotNull
  public Collection<String> getOptionDisablingNames() {
    return optionDisablingNames;
  }

  @NotNull
  public Collection<String> getGradlePropertiesOptionNames() {
    return gradlePropertiesOptionNames;
  }

  @NotNull
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private List<String> gradleTasks;
    private List<String> gradleParams;
    private File gradleUserHome;
    private File projectDirectory;
    private GradleOptionType gradleOptionType;
    private Collection<String> optionNames;
    private Collection<String> optionDisablingNames;
    private Collection<String> gradlePropertiesOptionNames;

    private Builder() {
    }

    public Builder withGradleTasks(@NotNull List<String> gradleTasks) {
      this.gradleTasks = gradleTasks;
      return this;
    }

    public Builder withGradleParams(@NotNull List<String> gradleParams) {
      this.gradleParams = gradleParams;
      return this;
    }

    public Builder withGradleUserHome(@Nullable File gradleUserHome) {
      this.gradleUserHome = gradleUserHome;
      return this;
    }

    public Builder withProjectDirectory(@NotNull File projectDirectory) {
      this.projectDirectory = projectDirectory;
      return this;
    }

    public Builder withGradleOptionType(@NotNull GradleOptionType gradleOptionType) {
      this.gradleOptionType = gradleOptionType;
      return this;
    }

    public Builder withOptionNames(@NotNull Collection<String> optionNames) {
      this.optionNames = optionNames;
      return this;
    }

    public Builder withOptionDisablingNames(@Nullable Collection<String> optionDisablingNames) {
      this.optionDisablingNames = optionDisablingNames;
      return this;
    }

    public Builder withGradlePropertiesOptionNames(@NotNull Collection<String> gradlePropertiesOptionNames) {
      this.gradlePropertiesOptionNames = gradlePropertiesOptionNames;
      return this;
    }

    @NotNull
    public GradleOptionValueFetchingParameters build() {
      return new GradleOptionValueFetchingParameters(gradleTasks, gradleParams, gradleUserHome, projectDirectory,
                                                     gradleOptionType, optionNames, optionDisablingNames,
                                                     gradlePropertiesOptionNames);
    }
  }
}
