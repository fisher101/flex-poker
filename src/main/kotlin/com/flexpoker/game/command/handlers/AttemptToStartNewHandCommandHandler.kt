package com.flexpoker.game.command.handlers

import com.flexpoker.framework.command.CommandHandler
import com.flexpoker.framework.event.EventPublisher
import com.flexpoker.game.command.commands.AttemptToStartNewHandCommand
import com.flexpoker.game.command.factory.GameFactory
import com.flexpoker.game.command.events.GameEvent
import com.flexpoker.game.command.repository.GameEventRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import javax.inject.Inject

@Component
class AttemptToStartNewHandCommandHandler @Inject constructor(
    private val gameFactory: GameFactory,
    private val eventPublisher: EventPublisher<GameEvent>,
    private val gameEventRepository: GameEventRepository
) : CommandHandler<AttemptToStartNewHandCommand> {

    @Async
    override fun handle(command: AttemptToStartNewHandCommand) {
        val gameEvents = gameEventRepository.fetchAll(command.aggregateId)
        val game = gameFactory.createFrom(gameEvents)
        game.attemptToStartNewHand(command.tableId, command.playerToChipsAtTableMap)
        val eventsWithVersions = gameEventRepository.setEventVersionsAndSave(gameEvents.size, game.fetchNewEvents())
        eventsWithVersions.forEach { eventPublisher.publish(it) }
    }

}