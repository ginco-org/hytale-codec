package gg.ginco.jellyparty.codec.annotations

/**
 * Marks a class as serializable and generates a codec for it.
 * The generated codec will be placed in a companion object as `CODEC`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SerializableAsset(val path: String = "", val extraImports: Array<String> = [])
