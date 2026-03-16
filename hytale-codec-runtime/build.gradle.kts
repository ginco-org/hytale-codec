val server_version: String by project

dependencies {
    compileOnly("com.hypixel.hytale:Server:${server_version}")
}

mavenPublishing {
    coordinates(artifactId = "hytale-codec-runtime")
}
