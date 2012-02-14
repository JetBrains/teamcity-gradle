<%@ page import="jetbrains.buildServer.gradle.GradleRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<l:settingsGroup title="Gradle Parameters">
    <tr>
        <th><label for="ui.gradleRunner.gradle.tasks.names">Gradle tasks: </label></th>
        <td><props:textProperty name="<%=GradleRunnerConstants.GRADLE_TASKS%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Enter task names separated by space character, leave blank to use the 'default' task.<br/>For example: ':myproject:clean :myproject:build' or 'clean build'.</span>
        </td>
    </tr>
    <tr>
      <forms:workingDirectory />
    </tr>
    <tr>
        <th><label for="gradle.home">Gradle home path: </label></th>
        <td><props:textProperty name="<%=GradleRunnerConstants.GRADLE_HOME%>" className="longField"/>
            <span class="smallNote">Path to the Gradle home directory (parent of bin directory). Overrides agent GRADLE_HOME environment variable</span>
        </td>
    </tr>
    <tr>
        <th><label for="ui.gradleRunner.additional.gradle.cmd.params">Additional Gradle command line
            parameters: </label></th>
        <td><props:textProperty name="<%=GradleRunnerConstants.GRADLE_PARAMS%>"  className="longField" maxlength="256"/>
            <span class="smallNote">Additional parameters will be added to the 'Gradle' command line.</span>
        </td>
    </tr>
    <tr>
        <th><label>Gradle Wrapper: <bs:help file="Gradle" anchor="GradleParameters"/></label></th>
        <td><props:checkboxProperty name="<%=GradleRunnerConstants.GRADLE_WRAPPER_FLAG%>"/>
          <label for="ui.gradleRunner.gradle.wrapper.useWrapper">Use gradle wrapper to build project</label>
        </td>
    </tr>
    <tr id="ui.gradleRunner.gradle.wrapper.path.tr">
        <th><label for="ui.gradleRunner.gradle.wrapper.path">Path to Wrapper script: </label></th>
        <td><props:textProperty name="<%=GradleRunnerConstants.GRADLE_WRAPPER_PATH%>" className="longField"/>
            <span class="smallNote">Optional path to the Gradle wrapper script. This path is relative to working directory</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Launching Parameters">
    <tr>
        <th><label>Debug: <bs:help file="Gradle" anchor="LaunchingParameters"/></label></th>
        <td><props:checkboxProperty name="<%=GradleRunnerConstants.DEBUG%>"/>
            <label for="ui.gradleRunner.gradle.debug.enabled">Log debug messages</label>
            <br/>
        </td>
    </tr>
    <tr>
        <th><label>Stacktrace: <bs:help file="Gradle" anchor="LaunchingParameters"/></label></th>
        <td><props:checkboxProperty name="<%=GradleRunnerConstants.STACKTRACE%>"/>
            <label for="ui.gradleRunner.gradle.stacktrace.enabled">Print stacktrace</label>
            <br/>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Java Parameters">
    <props:editJavaHome/>
    <props:editJvmArgs/>
</l:settingsGroup>

<l:settingsGroup title="Incremental Building">
  <tr>
      <th><label>Enable incremental building:</label></th>
      <td>
        <props:checkboxProperty name="<%=GradleRunnerConstants.IS_INCREMENTAL%>"/>
        <label for="ui.gradleRunner.gradle.incremental">Build only projects affected by changes</label>
        <span class="smallNote">:buildDependents task will be run on affected projects</span>
      </td>
  </tr>
</l:settingsGroup>

<script type="text/javascript">
  var updateGradleHomeVisibility = function () {
    var useWrapper = $('ui.gradleRunner.gradle.wrapper.useWrapper').checked;
    if (useWrapper) {
      $('ui.gradleRunner.gradle.home').disabled = true;
      BS.Util.show('ui.gradleRunner.gradle.wrapper.path.tr');
    } else {
      $('ui.gradleRunner.gradle.home').disabled = false;
      BS.Util.hide('ui.gradleRunner.gradle.wrapper.path.tr');
    }
    BS.VisibilityHandlers.updateVisibility($('ui.gradleRunner.gradle.wrapper.path'));
  };

  var updateGradleTasksVisibility = function () {
    var useBuildDependents = $('ui.gradleRunner.gradle.incremental').checked;
    $('ui.gradleRunner.gradle.tasks.names').disabled = useBuildDependents;
  };

  $j("#ui\\.gradleRunner\\.gradle\\.wrapper\\.useWrapper").click(updateGradleHomeVisibility);
  $j("#ui\\.gradleRunner\\.gradle\\.incremental").click(updateGradleTasksVisibility);

  updateGradleTasksVisibility();
  updateGradleHomeVisibility();
</script>
