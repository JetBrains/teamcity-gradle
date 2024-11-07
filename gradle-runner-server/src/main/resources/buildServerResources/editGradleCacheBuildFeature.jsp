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
    <div style="margin-top: 10px;">
      The feature tracks <a href="https://docs.gradle.org/current/userguide/directory_layout.html">shared cache</a> directories
      (<code>&lt;gradle_user_home&gt;/caches/modules-2</code>)
      used by Gradle steps and caches dependencies in the artifact storage. The cache is automatically updated when dependencies of corresponding Gradle projects change.
    </div>
    <c:set var="showEphemeralAgentsRestrictionWarning"
           value='<%= TeamCityProperties.getBoolean(DependencyCacheConstants.DEPENDENCY_CACHE_EPHEMERAL_AGENTS_ONLY, DependencyCacheConstants.DEPENDENCY_CACHE_EPHEMERAL_AGENTS_ONLY_DEFAULT) %>'/>
    <c:if test="${showEphemeralAgentsRestrictionWarning}">
      <div class="attentionComment">
        <bs:buildStatusIcon type="red-sign" className="warningIcon"/>
        Currently, Gradle caching is only performed on
        <a href="<bs:helpUrlPrefix/>predefined-build-parameters#Predefined+Agent+Environment+Parameters" target="_blank" rel="noreferrer noopener"
           showdiscardchangesmessage="false">ephemeral agents</a>
        (cloud agents terminated after their first build). Builds running on non-ephemeral agents neither cache nor reuse previously cached dependencies.
      </div>
    </c:if>
  </td>
</tr>