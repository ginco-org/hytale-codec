package gg.ginco.jellyparty.codec

import com.hypixel.hytale.assetstore.AssetExtraInfo
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap

abstract class AssetBase<T : AssetBase<T>> : JsonAssetWithMap<String, DefaultAssetMap<String, T>> {
    var data: AssetExtraInfo.Data? = null
    internal var _id: String? = null
    override fun getId(): String = _id ?: throw IllegalStateException("${this::class.simpleName} ID not set")
}
