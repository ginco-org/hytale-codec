package gg.ginco.jellyparty.codec.processortest

import gg.ginco.jellyparty.codec.annotations.SerializableObject
import gg.ginco.jellyparty.codec.processortest.model.MarkerDefinition
import gg.ginco.jellyparty.codec.processortest.model.TeamDefinition
import java.util.UUID

enum class MinigameMode { CLASSIC, HARDCORE }

@SerializableObject
data class MinigameDefinition(
    var gameTypeId: String = "",
    var displayName: String = "",
    var maxGames: Int = 0,
    var active: Boolean = false,
    var tags: List<String> = emptyList(),
    var mode: MinigameMode = MinigameMode.CLASSIC,
    var sessionToken: UUID? = null,
    var teamUuids: MutableList<UUID> = mutableListOf(),
    var teams: List<TeamDefinition> = emptyList(),
    var markers: MutableList<MarkerDefinition> = mutableListOf()
)
