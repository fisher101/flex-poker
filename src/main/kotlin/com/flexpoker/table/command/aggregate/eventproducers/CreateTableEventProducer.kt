package com.flexpoker.table.command.aggregate.eventproducers

import com.flexpoker.table.command.events.TableCreatedEvent
import com.flexpoker.table.command.events.TableEvent
import com.flexpoker.util.toPMap
import java.util.UUID
import kotlin.random.Random

fun createTable(tableId: UUID, gameId: UUID, numberOfPlayersPerTable: Int, playerIds: Set<UUID>): List<TableEvent> {
    require(playerIds.size >= 2) { "must have at least two players" }
    require(playerIds.size <= numberOfPlayersPerTable) { "player list can't be larger than the number of players per table" }

    val seatMap = mutableMapOf<Int, UUID?>()
    (0 until numberOfPlayersPerTable).forEach { seatMap[it] = null }
    playerIds.forEach {
        do {
            val attemptedSeatPosition = Random.nextInt(playerIds.size)
            if (seatMap[attemptedSeatPosition] == null) {
                seatMap[attemptedSeatPosition] = it
            }
        } while (seatMap[attemptedSeatPosition] != it)
    }

    return listOf(TableCreatedEvent(tableId, gameId, numberOfPlayersPerTable, seatMap.toPMap(), 1500))
}
