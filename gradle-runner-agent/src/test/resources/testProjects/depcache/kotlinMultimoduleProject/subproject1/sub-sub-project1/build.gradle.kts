dependencies {
    implementation(Dependencies.commons)
    testImplementation(platform(Dependencies.junitBom))
    testImplementation(Dependencies.junitJupiter)
}

tasks.test {
    useJUnitPlatform()
}
