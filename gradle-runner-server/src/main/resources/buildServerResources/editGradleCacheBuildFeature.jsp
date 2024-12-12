<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ page import="jetbrains.buildServer.serverSide.TeamCityProperties" %>
<%@ page import="jetbrains.buildServer.cache.depcache.DependencyCacheConstants" %>

<tr>
  <td colspan="2" height="5">
    <em>
      Caches dependencies downloaded by Gradle steps to speed up the consequent builds.<bs:help file="Dependency+Caches"/>
    </em>
    <div style="margin-top: 1em;">
      The feature tracks <a href="https://docs.gradle.org/current/userguide/directory_layout.html">shared cache</a> directories
      (<code>&lt;gradle_user_home&gt;/caches/modules-2</code>)
      used by Gradle steps and caches dependencies in the artifact storage. The cache is automatically updated when dependencies of corresponding Gradle projects change.
    </div>
    <c:set var="restrictedToEphemeralAgents"
           value='<%= TeamCityProperties.getBoolean(DependencyCacheConstants.DEPENDENCY_CACHE_EPHEMERAL_AGENTS_ONLY, DependencyCacheConstants.DEPENDENCY_CACHE_EPHEMERAL_AGENTS_ONLY_DEFAULT) %>'/>
    <c:choose>
      <c:when test="${restrictedToEphemeralAgents}">
        <div class="attentionComment">
          <bs:buildStatusIcon type="red-sign" className="warningIcon"/>
          Currently, Gradle caching is only performed on
          <a href="<bs:helpUrlPrefix/>predefined-build-parameters#Predefined+Agent+Environment+Parameters" target="_blank" rel="noreferrer noopener"
             showdiscardchangesmessage="false">ephemeral agents</a>
          (cloud agents terminated after their first build). Builds running on non-ephemeral agents neither cache nor reuse previously cached dependencies.
        </div>
      </c:when>
      <c:otherwise>
        <div style="margin-top: 0.5em;">
          Dependency caching is most effective on short-lived agents. For permanent or long-lived cloud agents, periodically review hidden
          <code>.teamcity.build_cache</code> build artifacts to monitor cache size and contents. This helps prevent redundant dependencies and unnecessary cache bloat.
        </div>
      </c:otherwise>
    </c:choose>
    <div class="attentionComment">
      <bs:buildStatusIcon type="red-sign" className="warningIcon"/>
      This feature is not recommended for builds that require a clean environment, such as release builds.
    </div>
  </td>
</tr>