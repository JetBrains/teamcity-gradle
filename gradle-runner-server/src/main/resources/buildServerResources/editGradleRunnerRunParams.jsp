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
            <span class="smallNote">Enter task names separated by spaces, leave blank to use the 'default' task.<br/>Example: ':myproject:clean :myproject:build' or 'clean build'.</span>
        </td>
    </tr>
     <tr>
        <th><label for="ui.gradleRUnner.gradle.build.file">Gradle build file: </label></th>
        <td><props:textProperty name="<%=GradleRunnerConstants.PATH_TO_BUILD_FILE%>"  className="longField" maxlength="256"/>
            <bs:vcsTree fieldId="<%=GradleRunnerConstants.PATH_TO_BUILD_FILE%>"/>
            <span class="smallNote">Path to build file</span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label>Incremental building:</label></th>
        <td>
          <props:checkboxProperty name="<%=GradleRunnerConstants.IS_INCREMENTAL%>"/>
          <label for="ui.gradleRunner.gradle.incremental">Enable incremental building</label>
          <span class="smallNote">:buildDependents task will be run on projects affected by changes</span>
        </td>
    </tr>
    <tr>
      <forms:workingDirectory />
    </tr>
    <tr>
        <th><label for="gradle.home">Gradle home path: </label></th>
        <td><props:textProperty name="<%=GradleRunnerConstants.GRADLE_HOME%>" className="longField"/>
            <span class="smallNote">Path to the Gradle home directory (parent of 'bin' directory). Overrides agent GRADLE_HOME environment variable</span>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label for="ui.gradleRunner.additional.gradle.cmd.params">Additional Gradle command line
            parameters: </label></th>
        <td><props:textProperty name="<%=GradleRunnerConstants.GRADLE_PARAMS%>"  className="longField" expandable="true"/>
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
          <bs:vcsTree fieldId="<%=GradleRunnerConstants.GRADLE_WRAPPER_PATH%>" treeId="<%=GradleRunnerConstants.GRADLE_WRAPPER_PATH%>"/>
          <span class="smallNote">Optional path to the Gradle wrapper script relative to the working directory</span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Run Parameters" className="advancedSetting">
    <tr class="advancedSetting">
        <th><label>Debug: <bs:help file="Gradle" anchor="LaunchingParameters"/></label></th>
        <td><props:checkboxProperty name="<%=GradleRunnerConstants.DEBUG%>"/>
            <label for="ui.gradleRunner.gradle.debug.enabled">Log debug messages</label>
            <br/>
        </td>
    </tr>
    <tr class="advancedSetting">
        <th><label>Stacktrace: <bs:help file="Gradle" anchor="LaunchingParameters"/></label></th>
        <td><props:checkboxProperty name="<%=GradleRunnerConstants.STACKTRACE%>"/>
            <label for="ui.gradleRunner.gradle.stacktrace.enabled">Print stacktrace</label>
            <br/>
        </td>
    </tr>
</l:settingsGroup>

<props:javaSettings/>

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

  var userTaskNames;

  var updateGradleTasksVisibility = function () {
    var useBuildDependents = $('ui.gradleRunner.gradle.incremental').checked;
    var taskNames = $('ui.gradleRunner.gradle.tasks.names');
    taskNames.disabled = useBuildDependents;
    if (useBuildDependents) {
      userTaskNames = taskNames.value;
      taskNames.value = "buildDependents";
      taskNames.title = "Tasks are ignored when incremental building is enabled.";
    } else {
      if (userTaskNames != undefined) {
        taskNames.value = userTaskNames;
      }
      taskNames.title = "";
    }
  };

  $j(BS.Util.escapeId("ui.gradleRunner.gradle.wrapper.useWrapper")).click(updateGradleHomeVisibility);
  $j(BS.Util.escapeId("ui.gradleRunner.gradle.incremental")).click(updateGradleTasksVisibility);

  updateGradleTasksVisibility();
  updateGradleHomeVisibility();
</script>
