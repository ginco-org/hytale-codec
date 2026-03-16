# hytale-codec

KSP-based codec generation for Hytale assets. Annotate your asset classes and let the processor generate the boilerplate `AssetBuilderCodec` and `BuilderCodec` wiring at compile time.

## Modules

| Artifact | Purpose |
|---|---|
| `hytale-codec-annotations` | Annotations you put on your classes |
| `hytale-codec-processor` | KSP processor that reads those annotations and generates codec code |
| `hytale-codec-runtime` | `AssetBase` — the base class your asset classes extend |

## Setup

### 1. GitHub Packages credentials

Add to `~/.gradle/gradle.properties` (not your project — keep credentials out of source control):

```properties
githubActor=your-github-username
githubToken=your-PAT-with-read:packages
```

### 2. Add the repository

In your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ginco-org/hytale-codec")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("githubActor").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("githubToken").orNull
            }
        }
    }
}
```

### 3. Add dependencies

In `build.gradle.kts`, alongside the KSP plugin (`id("com.google.devtools.ksp")`):

```kotlin
dependencies {
    implementation("gg.ginco:hytale-codec-annotations:1.0.5")
    implementation("gg.ginco:hytale-codec-runtime:1.0.5")
    ksp("gg.ginco:hytale-codec-processor:1.0.5")
}
```

## Usage

### Asset classes

Extend `AssetBase` and annotate with `@SerializableAsset`. The `path` is the asset store path Hytale loads from.

```kotlin
import gg.ginco.jellyparty.codec.AssetBase
import gg.ginco.jellyparty.codec.annotations.SerializableAsset

@SerializableAsset(path = "MyPlugin/Things")
class Thing : AssetBase<Thing>() {
    var displayName: String? = null
    var count: Int = 0
}
```

The processor generates `ThingCodec` and registers it in a `registerAssets()` function. Call that from your plugin's `onLoad` or equivalent.

### Non-asset objects

For nested objects that aren't top-level assets, use `@SerializableObject`:

```kotlin
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class Reward {
    var coins: Int = 0
    var itemId: String? = null
}
```

Generates `RewardCodec` as a `BuilderCodec`.

### Field annotations

| Annotation | Target | Effect |
|---|---|---|
| `@SerialName("key")` | Property | Use a custom key in the JSON instead of the property name |
| `@SerialIgnore` | Property | Skip this field during serialization entirely |
| `@SerialWithCodec("MyCodec")` | Property | Use a specific codec expression instead of auto-detection |

```kotlin
@SerializableAsset(path = "MyPlugin/Things")
class Thing : AssetBase<Thing>() {
    @SerialName("Name")
    var displayName: String? = null

    @SerialIgnore
    var cachedValue: Int = 0

    @SerialWithCodec("ArrayCodec(RewardCodec) { size -> arrayOfNulls<Reward>(size) }")
    var rewards: Array<Reward>? = null
}
```

### Supported field types

Auto-detected without any annotation:

- `String`, `Int`, `Float`, `Double`, `Boolean`
- `IntArray`, `Array<String>`, `List<String>`
- Enums (generates an `EnumCodec`)
- `Vector3f`, `Vector2f`, `Direction`, `Position` (Hytale protocol types)

For anything else, use `@SerialWithCodec`.

### Extra imports

If your `@SerialWithCodec` expression references types that wouldn't otherwise be imported in the generated file, pass them via `extraImports`:

```kotlin
@SerializableAsset(
    path = "MyPlugin/Boards",
    extraImports = ["com.hypixel.hytale.codec.codecs.array.ArrayCodec"]
)
class Board : AssetBase<Board>() { ... }
```

## Publishing a new version

Bump `version` in `gradle.properties`, commit, then push a matching `v*` tag:

```bash
git tag v1.0.4
git push origin main v1.0.4
```

The GitHub Actions workflow publishes all three artifacts to GitHub Packages automatically.
