dependencies {
    testImplementation(Dependencies.testNg)
}

tasks.test {
    useTestNG()
}