package com.flexpoker.core.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.ApplicationEventPublisher;

import com.flexpoker.bso.api.HandEvaluatorBso;
import com.flexpoker.bso.api.PotBso;
import com.flexpoker.config.Command;
import com.flexpoker.core.api.deck.CreateShuffledDeckCommand;
import com.flexpoker.core.api.game.InitializeAndStartGameCommand;
import com.flexpoker.core.api.seatstatus.SetSeatStatusForNewGameCommand;
import com.flexpoker.core.api.tablebalancer.AssignInitialTablesForNewGame;
import com.flexpoker.event.OpenTableForUserEvent;
import com.flexpoker.event.SendUserPocketCardsEvent;
import com.flexpoker.event.TableUpdatedEvent;
import com.flexpoker.model.Blinds;
import com.flexpoker.model.Game;
import com.flexpoker.model.GameEventType;
import com.flexpoker.model.Hand;
import com.flexpoker.model.HandDealerState;
import com.flexpoker.model.HandEvaluation;
import com.flexpoker.model.HandRanking;
import com.flexpoker.model.HandRoundState;
import com.flexpoker.model.Seat;
import com.flexpoker.model.Table;
import com.flexpoker.model.User;
import com.flexpoker.model.UserGameStatus;
import com.flexpoker.model.card.Deck;
import com.flexpoker.model.card.FlopCards;
import com.flexpoker.model.card.PocketCards;
import com.flexpoker.model.card.RiverCard;
import com.flexpoker.model.card.TurnCard;
import com.flexpoker.repository.api.GameRepository;
import com.flexpoker.util.ActionOnSeatPredicate;
import com.flexpoker.util.BigBlindSeatPredicate;
import com.flexpoker.util.SmallBlindSeatPredicate;

@Command
public class InitializeAndStartGameImplCommand implements InitializeAndStartGameCommand {

    private final AssignInitialTablesForNewGame assignInitialTablesForNewGame;
    
    private final SetSeatStatusForNewGameCommand setSeatStatusForNewGameCommand;
    
    private final GameRepository gameRepository;
    
    private final CreateShuffledDeckCommand createShuffledDeckCommand;

    private final PotBso potBso;
    
    private final HandEvaluatorBso handEvaluatorBso;
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Inject
    public InitializeAndStartGameImplCommand(
            AssignInitialTablesForNewGame assignInitialTablesForNewGame,
            SetSeatStatusForNewGameCommand setSeatStatusForNewGameCommand,
            GameRepository gameRepository,
            CreateShuffledDeckCommand createShuffledDeckCommand,
            PotBso potBso,
            HandEvaluatorBso handEvaluatorBso,
            ApplicationEventPublisher applicationEventPublisher) {
        this.assignInitialTablesForNewGame = assignInitialTablesForNewGame;
        this.setSeatStatusForNewGameCommand = setSeatStatusForNewGameCommand;
        this.gameRepository = gameRepository;
        this.createShuffledDeckCommand = createShuffledDeckCommand;
        this.potBso = potBso;
        this.handEvaluatorBso = handEvaluatorBso;
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    @Override
    public void execute(final UUID gameId) {
        assignInitialTablesForNewGame.execute(gameId);
        
        final Game game = gameRepository.findById(gameId);

        Set<UserGameStatus> userGameStatuses = game.getUserGameStatuses();

        for (UserGameStatus userGameStatus : userGameStatuses) {
            userGameStatus.setChips(1500);
        }
        
        for (Table table : game.getTables()) {
            for (Seat seat : table.getSeats()) {
                if (seat.getUserGameStatus() != null) {
                    applicationEventPublisher.publishEvent(new OpenTableForUserEvent(this,
                            seat.getUserGameStatus().getUser().getUsername(),
                            gameId, table.getId()));
                }
            }
        }

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (Table table: game.getTables()) {
                    setSeatStatusForNewGameCommand.execute(table);
                    Game game = gameRepository.findById(gameId);
                    resetTableStatus(game, table);
                    applicationEventPublisher.publishEvent(new TableUpdatedEvent(this, gameId, table));
                }
                timer.cancel();
            }
        }, 5000);
    }
    
    private void resetTableStatus(Game game, Table table) {
        potBso.createNewHandPot(game, table);
        createNewRealTimeHand(game, table);
        table.getCurrentHand().setPots(new HashSet<>(potBso.fetchAllPots(game, table)));
    }
    
    private void createNewRealTimeHand(Game game, Table table) {
        Blinds currentBlinds = game.getCurrentBlinds();
        int smallBlind = currentBlinds.getSmallBlind();
        int bigBlind = currentBlinds.getBigBlind();

        Deck deck = createShuffledDeckCommand.execute(table.getNumberOfPlayers());

        Hand realTimeHand = new Hand(table.getSeats(), deck);
        table.setCurrentHand(realTimeHand);

        Seat smallBlindSeat = (Seat) CollectionUtils.find(table.getSeats(),
                new SmallBlindSeatPredicate());
        Seat bigBlindSeat = (Seat) CollectionUtils.find(table.getSeats(),
                new BigBlindSeatPredicate());

        for (Seat seat : table.getSeats()) {
            if (seat.getUserGameStatus() == null) {
                continue;
            }

            int chipsInFront = 0;
            int callAmount = bigBlind;
            int raiseToAmount = bigBlind * 2;

            if (seat.equals(bigBlindSeat)) {
                chipsInFront = bigBlind;
                callAmount = 0;
            } else if (seat.equals(smallBlindSeat)) {
                chipsInFront = smallBlind;
                callAmount = smallBlind;
            }

            if (chipsInFront > seat.getUserGameStatus().getChips()) {
                seat.setChipsInFront(seat.getUserGameStatus().getChips());
            } else {
                seat.setChipsInFront(chipsInFront);
            }

            seat.getUserGameStatus().setChips(
                    seat.getUserGameStatus().getChips() - seat.getChipsInFront());

            if (callAmount > seat.getUserGameStatus().getChips()) {
                seat.setCallAmount(seat.getUserGameStatus().getChips());
            } else {
                seat.setCallAmount(callAmount);
            }

            int totalChips = seat.getUserGameStatus().getChips()
                    + seat.getChipsInFront();

            if (raiseToAmount > totalChips) {
                seat.setRaiseTo(totalChips);
            } else {
                seat.setRaiseTo(raiseToAmount);
            }

            table.getCurrentHand().addToTotalPot(seat.getChipsInFront());

            if (seat.getRaiseTo() > 0) {
                realTimeHand.addPossibleSeatAction(seat, GameEventType.RAISE);
            }

            if (seat.getCallAmount() > 0) {
                realTimeHand.addPossibleSeatAction(seat, GameEventType.CALL);
                realTimeHand.addPossibleSeatAction(seat, GameEventType.FOLD);
            } else {
                realTimeHand.addPossibleSeatAction(seat, GameEventType.CHECK);
            }
        }

        determineNextToAct(table, realTimeHand);
        realTimeHand.setLastToAct(bigBlindSeat);

        realTimeHand.setHandDealerState(HandDealerState.POCKET_CARDS_DEALT);
        realTimeHand.setHandRoundState(HandRoundState.ROUND_IN_PROGRESS);

        List<HandEvaluation> handEvaluations = determineHandEvaluations(game, table);
        realTimeHand.setHandEvaluationList(handEvaluations);
    }

    private List<HandEvaluation> determineHandEvaluations(Game game, Table table) {
        FlopCards flopCards = table.getCurrentHand().getDeck().getFlopCards();
        TurnCard turnCard = table.getCurrentHand().getDeck().getTurnCard();
        RiverCard riverCard = table.getCurrentHand().getDeck().getRiverCard();

        List<HandRanking> possibleHands = handEvaluatorBso.determinePossibleHands(flopCards, turnCard, riverCard);

        List<HandEvaluation> handEvaluations = new ArrayList<>();

        for (Seat seat : table.getSeats()) {
            if (seat.getUserGameStatus() != null
                    && seat.getUserGameStatus().getUser() != null) {
                User user = seat.getUserGameStatus().getUser();
                PocketCards pocketCards = table.getCurrentHand().getDeck()
                        .getPocketCards(seat.getPosition());
                HandEvaluation handEvaluation = handEvaluatorBso
                        .determineHandEvaluation(flopCards, turnCard, riverCard,
                                user, pocketCards, possibleHands);
                handEvaluations.add(handEvaluation);
                applicationEventPublisher.publishEvent(new SendUserPocketCardsEvent(this,
                        user.getUsername(), pocketCards, table.getId()));
            }
        }

        return handEvaluations;
    }

    private void determineNextToAct(Table table, Hand realTimeHand) {
        List<Seat> seats = table.getSeats();
        Seat actionOnSeat = (Seat) CollectionUtils.find(seats,
                new ActionOnSeatPredicate());

        int actionOnIndex = seats.indexOf(actionOnSeat);

        for (int i = actionOnIndex + 1; i < seats.size(); i++) {
            if (seats.get(i).isStillInHand()) {
                realTimeHand.setNextToAct(seats.get(i));
                return;
            }
        }

        for (int i = 0; i < actionOnIndex; i++) {
            if (seats.get(i).isStillInHand()) {
                realTimeHand.setNextToAct(seats.get(i));
                return;
            }
        }
    }

}
