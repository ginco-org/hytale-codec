package gg.ginco.jellyparty.codec.annotations

/**
 * Marks a non-asset class as serializable and generates a BuilderCodec for it.
 * The generated codec is placed in the class's companion object as `CODEC`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SerializableObject(val extraImports: Array<String> = [])
