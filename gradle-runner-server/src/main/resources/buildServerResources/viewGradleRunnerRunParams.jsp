<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<div class="parameter">
  Gradle tasks: <strong><props:displayValue name="ui.gradleRunner.gradle.tasks.names" emptyValue="default"/></strong>
</div>

<props:viewWorkingDirectory />

<div class="parameter">
    Gradle home path: <strong><props:displayValue name="ui.gradleRunner.gradle.home" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Additional Gradle command line parameters: <strong><props:displayValue name="ui.gradleRunner.additional.gradle.cmd.params" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Use Gradle Wrapper: <strong><props:displayCheckboxValue name="ui.gradleRunner.gradle.wrapper.useWrapper"/></strong>
</div>

<div class="parameter">
    Path to Gradle wrapper: <strong><props:displayValue name="ui.gradleRunner.gradle.wrapper.path" emptyValue="not specified"/></strong>
</div>

<div class="parameter">
    Incremental building enabled: <strong><props:displayCheckboxValue name="ui.gradleRunner.gradle.incremental"/></strong>
</div>

<div class="parameter">
    Run Parameters:
    <div class="nestedParameter">
      <ul style="list-style: none; padding-left: 0; margin-left: 0; margin-top: 0.1em; margin-bottom: 0.1em;">
          <li>Print stacktrace<strong><props:displayCheckboxValue name="ui.gradleRunner.gradle.stacktrace.enabled"/></strong></li>
          <li>Log debug messages<strong><props:displayCheckboxValue name="ui.gradleRunner.gradle.debug.enabled"/></strong></li>
      </ul>
    </div>
</div>

<props:viewJavaHome/>
<props:viewJvmArgs/>
