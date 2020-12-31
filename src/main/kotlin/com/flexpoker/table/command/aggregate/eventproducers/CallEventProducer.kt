package com.flexpoker.table.command.aggregate.eventproducers

import com.flexpoker.table.command.PlayerAction
import com.flexpoker.table.command.aggregate.HandState
import com.flexpoker.table.command.aggregate.TableState
import com.flexpoker.table.command.aggregate.applyEvents
import com.flexpoker.table.command.aggregate.checkActionOnPlayer
import com.flexpoker.table.command.aggregate.checkHandIsBeingPlayed
import com.flexpoker.table.command.aggregate.checkPerformAction
import com.flexpoker.table.command.aggregate.handleEndOfRound
import com.flexpoker.table.command.events.PlayerCalledEvent
import com.flexpoker.table.command.events.TableEvent
import java.util.UUID

fun call(state: TableState, playerId: UUID): List<TableEvent> {
    checkHandIsBeingPlayed(state)
    val playerCalledEvents = call(state.currentHand!!, playerId)
    val endOfRoundEvents = handleEndOfRound(applyEvents(state, *playerCalledEvents.toTypedArray()))
    return playerCalledEvents.plus(endOfRoundEvents)
}

fun call(state: HandState, playerId: UUID): List<TableEvent> {
    checkActionOnPlayer(state, playerId)
    checkPerformAction(state, playerId, PlayerAction.CALL)
    return listOf(PlayerCalledEvent(state.tableId, state.gameId, state.entityId, playerId))
}