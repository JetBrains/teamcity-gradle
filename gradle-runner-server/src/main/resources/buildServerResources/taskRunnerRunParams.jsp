<%@ page import="jetbrains.buildServer.gradle.GradleRunnerConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<l:settingsGroup title="Gradle Parameters">
    <tr>
        <th><label for="ui.gradleRunner.gradle.tasks.names">Gradle tasks: </label></th>
        <td><props:textProperty name="<%=GradleRunnerConstants.GRADLE_TASKS%>" style="width:30em;" maxlength="256"/>
            <span class="smallNote">Enter tasks names separated by space character, blank to use the 'default' task.<br/>For example: ':myproject:clean :myproject:build' or 'clean build'.</span>
        </td>
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
        <td><props:textProperty name="<%=GradleRunnerConstants.GRADLE_PARAMS%>" style="width:30em;" maxlength="256"/>
            <span class="smallNote">Additional parameters will be added to the 'Gradle' command line.</span>
        </td>
    </tr>
    <tr>
        <th><label>Gradle Wrapper: <bs:help file="Gradle" anchor="GradleParameters"/></label></th>
        <td><props:checkboxProperty name="<%=GradleRunnerConstants.GRADLE_WRAPPER_FLAG%>"/>
          <label for="ui.gradleRunner.gradle.wrapper.useWrapper">Use gradle wrapper to build project</label>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Launching Parameters">
    <tr>
        <th><label>Debug: </label></th>
        <td><props:checkboxProperty name="<%=GradleRunnerConstants.DEBUG%>"/>
            <label for="ui.gradleRunner.gradle.debug.enabled">Log debug messages</label>
            <br/>
        </td>
    </tr>
    <tr>
        <th><label>Stacktrace: </label></th>
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
