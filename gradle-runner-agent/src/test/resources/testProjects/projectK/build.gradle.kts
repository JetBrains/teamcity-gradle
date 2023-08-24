plugins {
    java
}

dependencies {
    testCompile(files("../lib/junit-4.8.1.jar"))
}
val teamcity: Map<String, String> by project
val test_system_property: String by project

task("printProperties") {
    doLast {
        project.getProperties().forEach { key, v ->
            println("##gradle-property name='${key}' value='${v}'")
        }

        teamcity.forEach { key, v ->
            println "##tc-property name='${key}' value='${v}'"
        }

    }
}

task("printTcProperty") {
    doLast {
        println("##tc-property ${teamcity["test_teamcity_property"]}")
    }
}
task("printSystemProperty") {
    doLast {
        println("##system-property ${test_system_property}")
    }
}
