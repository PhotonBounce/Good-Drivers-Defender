tasks.register("publishInternal") {
    dependsOn("bundleRelease") // Assemble the AAB
    doLast {
        println("AAB built. Use the Play Publisher plugin to upload with `./gradlew publishReleaseBundle`.")
    }
}
