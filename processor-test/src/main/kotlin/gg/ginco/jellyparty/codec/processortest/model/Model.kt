package gg.ginco.jellyparty.codec.processortest.model

import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
data class TeamDefinition(
    var name: String = "",
    var color: Int = 0xFFFFFF,
    var maxPlayers: Int = -1
)

@SerializableObject
data class MarkerDefinition(
    var type: String = "",
    var minX: Double = 0.0,
    var minY: Double = 0.0,
    var minZ: Double = 0.0,
    var maxX: Double = 0.0,
    var maxY: Double = 0.0,
    var maxZ: Double = 0.0
)
