dependencies {
    implementation(Dependencies.gson)
    implementation(Dependencies.hadoop)
    implementation(Dependencies.tikaApp)
    implementation(Dependencies.awsJavaSdkBundle)

    testImplementation(Dependencies.junit)
}

tasks.test {
    useJUnit()
}