package gg.ginco.jellyparty.codec.processortest

import com.hypixel.hytale.codec.EmptyExtraInfo
import com.hypixel.hytale.codec.util.RawJsonReader
import gg.ginco.jellyparty.codec.processortest.model.MarkerDefinition
import gg.ginco.jellyparty.codec.processortest.model.TeamDefinition
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CodecRoundTripTest {

    private val teamUuid1 = UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301")
    private val teamUuid2 = UUID.fromString("21f7f8de-8051-4b89-902c-4bd05e5c9e1b")

    private val json = """
        {
            "GameTypeId": "bedwars",
            "DisplayName": "Bed Wars",
            "MaxGames": 10,
            "Active": true,
            "Tags": ["pvp", "teams"],
            "Scores": [1500, 750, 0],
            "SpawnOffsets": [-12.5, 0.0, 33.75],
            "Weights": [0.5, 0.25, 0.125],
            "Mode": "Hardcore",
            "SessionToken": "${teamUuid1}",
            "TeamUuids": ["${teamUuid1}", "${teamUuid2}"],
            "Teams": [
                { "Name": "Red", "Color": 16711680, "MaxPlayers": 4 },
                { "Name": "Blue", "Color": 255 }
            ],
            "Markers": [
                {
                    "Type": "spawn",
                    "MinX": -12.5, "MinY": 0.0, "MinZ": -8.25,
                    "MaxX": 12.5, "MaxY": 64.0, "MaxZ": 33.75
                }
            ]
        }
    """.trimIndent()

    private val expected = MinigameDefinition(
        gameTypeId = "bedwars",
        displayName = "Bed Wars",
        maxGames = 10,
        active = true,
        tags = listOf("pvp", "teams"),
        scores = listOf(1500, 750, 0),
        spawnOffsets = mutableListOf(-12.5, 0.0, 33.75),
        weights = listOf(0.5f, 0.25f, 0.125f),
        mode = MinigameMode.HARDCORE,
        sessionToken = teamUuid1,
        teamUuids = mutableListOf(teamUuid1, teamUuid2),
        teams = listOf(
            TeamDefinition(name = "Red", color = 16711680, maxPlayers = 4),
            TeamDefinition(name = "Blue", color = 255, maxPlayers = -1)
        ),
        markers = mutableListOf(
            MarkerDefinition(
                type = "spawn",
                minX = -12.5, minY = 0.0, minZ = -8.25,
                maxX = 12.5, maxY = 64.0, maxZ = 33.75
            )
        )
    )

    @Test
    fun `json decode matches expected object`() {
        val decoded = MinigameDefinitionCodec.decodeJson(
            RawJsonReader.fromJsonString(json),
            EmptyExtraInfo.EMPTY
        )
        assertEquals(expected, decoded)
    }

    @Test
    fun `json to bson to object round trip is stable`() {
        val first = MinigameDefinitionCodec.decodeJson(
            RawJsonReader.fromJsonString(json),
            EmptyExtraInfo.EMPTY
        )
        val bson = MinigameDefinitionCodec.encode(first, EmptyExtraInfo.EMPTY)
        val second = MinigameDefinitionCodec.decode(bson, EmptyExtraInfo.EMPTY)
        assertEquals(first, second)
        assertEquals(expected, second)
    }

    @Test
    fun `bson json projection decodes back through decodeJson`() {
        val first = MinigameDefinitionCodec.decodeJson(
            RawJsonReader.fromJsonString(json),
            EmptyExtraInfo.EMPTY
        )
        val bson = MinigameDefinitionCodec.encode(first, EmptyExtraInfo.EMPTY)
        val second = MinigameDefinitionCodec.decodeJson(
            RawJsonReader.fromJsonString(bson.toJson()),
            EmptyExtraInfo.EMPTY
        )
        assertEquals(first, second)
    }
}
