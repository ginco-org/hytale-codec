publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "hytale-codec-annotations"
            from(components["kotlin"])
        }
    }
}
