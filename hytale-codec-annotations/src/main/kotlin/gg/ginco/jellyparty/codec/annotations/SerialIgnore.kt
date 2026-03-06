package gg.ginco.jellyparty.codec.annotations

/**
 * Marks a property to be ignored during serialization/deserialization.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class SerialIgnore
