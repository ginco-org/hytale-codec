val server_version: String by project

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.6")
    implementation("com.squareup:kotlinpoet-ksp:2.1.0")

    implementation(project(":hytale-codec-annotations"))

    compileOnly("com.hypixel.hytale:Server:${server_version}")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "hytale-codec-processor"
            from(components["kotlin"])
        }
    }
}
