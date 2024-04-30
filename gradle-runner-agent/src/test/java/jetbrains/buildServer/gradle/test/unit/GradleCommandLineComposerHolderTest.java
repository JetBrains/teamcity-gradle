package jetbrains.buildServer.gradle.test.unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposer;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposerHolder;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleSimpleCommandLineComposer;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleToolingApiCommandLineComposer;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class GradleCommandLineComposerHolderTest {

  @DataProvider
  public Object[][] launchModeProvider() {
    GradleLaunchMode[] statuses = GradleLaunchMode.values();
    Object[][] data = new Object[statuses.length][1];
    for (int i = 0; i < statuses.length; i++) {
      data[i][0] = statuses[i];
    }
    return data;
  }
  @Test(dataProvider = "launchModeProvider")
  public void should_GetComposerByLaunchMode(GradleLaunchMode launchMode) throws RunBuildException {
    // arrange
    GradleTasksComposer tasksComposer = new GradleTasksComposer();
    List<GradleCommandLineComposer> composers = Arrays.asList(
      new GradleSimpleCommandLineComposer(tasksComposer), new GradleToolingApiCommandLineComposer(Collections.emptyList(), tasksComposer)
    );
    GradleCommandLineComposerHolder holder = new GradleCommandLineComposerHolder(composers);

    // act
    GradleCommandLineComposer result = holder.getCommandLineComposer(launchMode);

    // assert
    switch (launchMode) {
      case COMMAND_LINE:
        assertTrue(result instanceof GradleSimpleCommandLineComposer);
        break;
      case TOOLING_API:
        assertTrue(result instanceof GradleToolingApiCommandLineComposer);
        break;
      default:
        throw new IllegalStateException("GradleCommandLineComposer is required for each GradleLaunchMode");
    }
  }
}
