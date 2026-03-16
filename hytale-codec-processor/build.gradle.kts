val server_version: String by project

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.5")
    implementation("com.squareup:kotlinpoet-ksp:2.1.0")
    implementation(project(":hytale-codec-annotations"))
    implementation(kotlin("reflect"))
    compileOnly("com.hypixel.hytale:Server:${server_version}")
}

mavenPublishing {
    coordinates(artifactId = "hytale-codec-processor")
}
