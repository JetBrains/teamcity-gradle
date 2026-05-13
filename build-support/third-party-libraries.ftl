<#function json value>
  <#return (value!"")?json_string>
</#function>
<#function normalizedLicense licenses project>
  <#assign name = "">
  <#if licenses?size gt 0>
    <#assign name = licenses[0]>
  <#elseif project.licenses?? && project.licenses?size gt 0>
    <#assign name = project.licenses[0].name!"">
  </#if>
  <#if name?contains("Apache") && name?contains("2.0")>
    <#return "Apache 2.0">
  </#if>
  <#if name?contains("MIT")>
    <#return "MIT">
  </#if>
  <#return name>
</#function>
<#function normalizedLicenseUrl license project>
  <#assign url = "">
  <#if project.licenses?? && project.licenses?size gt 0>
    <#assign url = project.licenses[0].url!"">
  </#if>
  <#if license == "Apache 2.0">
    <#return "https://www.apache.org/licenses/LICENSE-2.0">
  </#if>
  <#if license == "MIT">
    <#return "https://opensource.org/licenses/MIT">
  </#if>
  <#return url>
</#function>
{
<#assign selectedCoordinates = ["commons-cli:commons-cli", "org.gradle:gradle-tooling-api", "org.slf4j:slf4j-api"]>
<#assign selectedDependencies = []>
<#list selectedCoordinates as coordinate>
  <#list dependencyMap as e>
    <#assign project = e.getKey()>
    <#if project.groupId + ":" + project.artifactId == coordinate>
      <#assign selectedDependencies = selectedDependencies + [e]>
    </#if>
  </#list>
</#list>
    "dependencies": [
<#list selectedDependencies as e>
        <#assign project = e.getKey()>
        <#assign licenses = e.getValue()>
        <#assign license = normalizedLicense(licenses, project)>
        {
            "moduleName": "${json(project.groupId)}:${json(project.artifactId)}",
            "moduleUrl": "${json(project.url)}",
            "moduleVersion": "${json(project.version)}",
            "moduleLicense": "${json(license)}",
            "moduleLicenseUrl": "${json(normalizedLicenseUrl(license, project))}"
        }<#sep>,</#sep>
</#list>
    ]
}
