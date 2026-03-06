package gg.ginco.jellyparty.codec.annotations

/**
 * Customizes the name used for serialization/deserialization of a property.
 * If not specified, the property name is used.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class SerialName(val value: String)
