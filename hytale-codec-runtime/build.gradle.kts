val server_version: String by project

dependencies {
    compileOnly("com.hypixel.hytale:Server:${server_version}")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "hytale-codec-runtime"
            from(components["kotlin"])
        }
    }
}
