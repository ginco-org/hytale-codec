package gg.ginco.jellyparty.codec.annotations

/**
 * Specifies a custom codec class to use for a property instead of auto-detection.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class SerialWithCodec(val codecClass: String)
