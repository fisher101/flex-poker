package com.flexpoker.controller;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

import com.flexpoker.bso.PotBsoImpl;
import com.flexpoker.bso.RealTimeGameBsoImpl;
import com.flexpoker.dao.GameDaoImpl;
import com.flexpoker.dao.UserDaoImpl;
import com.flexpoker.exception.FlexPokerException;
import com.flexpoker.model.FlopCards;
import com.flexpoker.model.Game;
import com.flexpoker.model.GameEventType;
import com.flexpoker.model.HandDealerState;
import com.flexpoker.model.PocketCards;
import com.flexpoker.model.RealTimeHand;
import com.flexpoker.model.RiverCard;
import com.flexpoker.model.Seat;
import com.flexpoker.model.Table;
import com.flexpoker.model.TurnCard;
import com.flexpoker.model.User;
import com.flexpoker.util.ActionOnSeatPredicate;
import com.flexpoker.util.IntegrationContext;
import com.flexpoker.util.IntegrationTest;

/**
 * Integration test for FlexControllerImpl class.
 *
 * @author cwoolner
 */
public class FlexControllerImplIntTest extends IntegrationTest {

    private UserDaoImpl userDao = (UserDaoImpl) IntegrationContext.instance()
            .getBean("userDao");

    private GameDaoImpl gameDao = (GameDaoImpl) IntegrationContext.instance()
            .getBean("gameDao");

    private FlexControllerImpl flexController = (FlexControllerImpl)
            IntegrationContext.instance().getBean("flexController");

    private RealTimeGameBsoImpl realTimeGameBso = (RealTimeGameBsoImpl)
            IntegrationContext.instance().getBean("realTimeGameBso");

    private PotBsoImpl potBso = (PotBsoImpl)
            IntegrationContext.instance().getBean("potBso");

    @Test
    public void testCheck() {
        setEntityManagers(userDao, gameDao);
        User john = userDao.findByUsername("john").get(0);
        User guest = userDao.findByUsername("guest").get(0);

        Authentication johnAuth = new UsernamePasswordAuthenticationToken(john, "");
        Authentication guestAuth = new UsernamePasswordAuthenticationToken(guest, "");
        SecurityContextHolder.getContext().setAuthentication(johnAuth);

        Game game = new Game();
        game.setTotalPlayers(2);
        game.setMaxPlayersPerTable(2);

        entityTransaction.begin();
        flexController.createGame(game);
        entityTransaction.commit();

        flexController.joinGame(game);
        SecurityContextHolder.getContext().setAuthentication(guestAuth);
        flexController.joinGame(game);

        assertEquals(2, flexController.fetchAllUserGameStatuses(game).size());

        flexController.verifyRegistrationForGame(game);
        SecurityContextHolder.getContext().setAuthentication(johnAuth);
        flexController.verifyRegistrationForGame(game);

        flexController.verifyGameInProgress(game);
        SecurityContextHolder.getContext().setAuthentication(guestAuth);
        flexController.verifyGameInProgress(game);

        SecurityContextHolder.getContext().setAuthentication(johnAuth);

        Table table = flexController.fetchTable(game);
        RealTimeHand realTimeHand = realTimeGameBso.get(game).getRealTimeHand(table);

        Seat actionOnSeat = (Seat) CollectionUtils.find(table.getSeats(),
                new ActionOnSeatPredicate());
        Seat johnSeat;
        Seat guestSeat;

        if (table.getSeats().get(0).getUserGameStatus().getUser().equals(john)) {
            johnSeat = table.getSeats().get(0);
            guestSeat = table.getSeats().get(1);
        } else {
            johnSeat = table.getSeats().get(1);
            guestSeat = table.getSeats().get(0);
        }

        if (actionOnSeat.equals(johnSeat)) {
            testCheckCommon(johnSeat, guestSeat, johnAuth, guestAuth, game,
                    table, realTimeHand);
        } else if (actionOnSeat.equals(guestSeat)) {
            testCheckCommon(guestSeat, johnSeat, guestAuth, johnAuth, game,
                    table, realTimeHand);
        } else {
            fail("Neither seat belongs to guest or john.");
        }
    }

    private void testCheckCommon(Seat initialActionOnSeat, Seat initialActionOffSeat,
            Authentication actionOnAuth, Authentication actionOffAuth,
            Game game, Table table, RealTimeHand realTimeHand) {
        commonChipCheck(initialActionOnSeat, 1490, 10, 10, 40);
        commonChipCheck(initialActionOffSeat, 1480, 0, 20, 40);
        assertTrue(table.getTotalPotAmount() == 30);
        commonAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                GameEventType.CALL, GameEventType.FOLD, GameEventType.RAISE);
        commonAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                GameEventType.CHECK, GameEventType.RAISE);
        commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                GameEventType.CHECK);
        commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                GameEventType.FOLD, GameEventType.CALL);

        // initial call from the small blind
        SecurityContextHolder.getContext().setAuthentication(actionOnAuth);
        flexController.call(game, table);

        commonChipCheck(initialActionOnSeat, 1480, 0, 20, 0);
        commonChipCheck(initialActionOffSeat, 1480, 0, 20, 40);
        assertTrue(table.getTotalPotAmount() == 40);
        commonAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat);
        commonAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                GameEventType.CHECK, GameEventType.RAISE);
        commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                GameEventType.CHECK, GameEventType.CALL, GameEventType.FOLD,
                GameEventType.RAISE);
        commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                GameEventType.FOLD, GameEventType.CALL);

        // big blind checks
        SecurityContextHolder.getContext().setAuthentication(actionOffAuth);
        flexController.check(game, table);

        commonChipCheck(initialActionOnSeat, 1480, 0, 0, 20);
        commonChipCheck(initialActionOffSeat, 1480, 0, 0, 20);
        assertTrue(table.getTotalPotAmount() == 40);
        commonAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                GameEventType.CHECK, GameEventType.RAISE);
        commonAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                GameEventType.CHECK, GameEventType.RAISE);
        commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                GameEventType.FOLD, GameEventType.CALL);
        commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                GameEventType.FOLD, GameEventType.CALL);

        // call three times for flop, turn, and river
        checkRound(initialActionOnSeat, initialActionOffSeat, actionOnAuth,
                actionOffAuth, game, table, realTimeHand);
        checkRound(initialActionOnSeat, initialActionOffSeat, actionOnAuth,
                actionOffAuth, game, table, realTimeHand);
        checkRound(initialActionOnSeat, initialActionOffSeat, actionOnAuth,
                actionOffAuth, game, table, realTimeHand);
    }

    private void checkRound(Seat initialActionOnSeat, Seat initialActionOffSeat,
            Authentication actionOnAuth, Authentication actionOffAuth,
            Game game, Table table, RealTimeHand realTimeHand) {
        // big blind acts first in heads-up
        SecurityContextHolder.getContext().setAuthentication(actionOffAuth);
        flexController.check(game, table);

        commonChipCheck(initialActionOnSeat, 1480, 0, 0, 20);
        commonChipCheck(initialActionOffSeat, 1480, 0, 0, 0);
        assertTrue(table.getTotalPotAmount() == 40);
        commonAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                GameEventType.CHECK, GameEventType.RAISE);
        commonAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat);
        commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                GameEventType.FOLD, GameEventType.CALL);
        commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                GameEventType.CHECK, GameEventType.RAISE,
                GameEventType.FOLD, GameEventType.CALL);

        // small blind acts second in heads-up
        SecurityContextHolder.getContext().setAuthentication(actionOnAuth);
        flexController.check(game, table);

        if (realTimeHand.getHandDealerState().equals(HandDealerState.COMPLETE)) {
            List<Seat> winners = potBso.fetchAllPots(game, table).get(0).getWinners();

            int actionOnChips = 0;
            int actionOffChips = 0;

            if (winners.contains(initialActionOnSeat)
                    && winners.contains(initialActionOffSeat)) {
                actionOnChips = 1500;
                actionOffChips = 1500;
            } else if (winners.contains(initialActionOnSeat)) {
                actionOnChips = 1520;
                actionOffChips = 1480;
            } else if (winners.contains(initialActionOffSeat)) {
                actionOnChips = 1480;
                actionOffChips = 1520;
            }

            commonChipCheck(initialActionOnSeat, actionOnChips, 0, 0, 0);
            commonChipCheck(initialActionOffSeat, actionOffChips, 0, 0, 0);
            assertTrue(table.getTotalPotAmount() == 40);
            commonAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat);
            commonAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat);
            commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                    GameEventType.CHECK, GameEventType.RAISE,
                    GameEventType.FOLD, GameEventType.CALL);
            commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                    GameEventType.CHECK, GameEventType.RAISE,
                    GameEventType.FOLD, GameEventType.CALL);
        } else {
            commonChipCheck(initialActionOnSeat, 1480, 0, 0, 20);
            commonChipCheck(initialActionOffSeat, 1480, 0, 0, 20);
            assertTrue(table.getTotalPotAmount() == 40);
            commonAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                    GameEventType.CHECK, GameEventType.RAISE);
            commonAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                    GameEventType.CHECK, GameEventType.RAISE);
            commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOnSeat,
                    GameEventType.FOLD, GameEventType.CALL);
            commonNotAllowedToPerformActionCheck(realTimeHand, initialActionOffSeat,
                    GameEventType.FOLD, GameEventType.CALL);
        }
    }

    private void commonAllowedToPerformActionCheck(RealTimeHand realTimeHand,
            Seat seat, GameEventType...gameEventTypes) {
        for (GameEventType gameEventType : gameEventTypes) {
            assertTrue(realTimeHand.isUserAllowedToPerformAction(gameEventType, seat));
        }
    }

    private void commonNotAllowedToPerformActionCheck(RealTimeHand realTimeHand,
            Seat seat, GameEventType...gameEventTypes) {
        for (GameEventType gameEventType : gameEventTypes) {
            assertFalse(realTimeHand.isUserAllowedToPerformAction(gameEventType, seat));
        }
    }

    private void commonChipCheck(Seat seat, int chips, int callAmount,
            int chipsInFront, int raiseTo) {
        assertTrue(seat.getUserGameStatus().getChips() == chips);
        assertTrue(seat.getCallAmount() == callAmount);
        assertTrue(seat.getChipsInFront() == chipsInFront);
        assertTrue(seat.getRaiseTo() == raiseTo);
    }

    @Test
    public void testCardRetrieval() {
        setEntityManagers(userDao, gameDao);
        User john = userDao.findByUsername("john").get(0);
        User guest = userDao.findByUsername("guest").get(0);

        Authentication johnAuth = new UsernamePasswordAuthenticationToken(john, "");
        Authentication guestAuth = new UsernamePasswordAuthenticationToken(guest, "");
        SecurityContextHolder.getContext().setAuthentication(johnAuth);

        Game game = new Game();
        game.setTotalPlayers(2);
        game.setMaxPlayersPerTable(2);

        entityTransaction.begin();
        flexController.createGame(game);
        entityTransaction.commit();

        Table table = null;

        try {
            table = flexController.fetchTable(game);
            fail("Should throw a FlexPokerException.");
        } catch (FlexPokerException e) {}
        assertNull(table);

        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, PocketCards.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, FlopCards.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, TurnCard.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, RiverCard.class);

        SecurityContextHolder.getContext().setAuthentication(johnAuth);
        flexController.joinGame(game);
        SecurityContextHolder.getContext().setAuthentication(guestAuth);
        flexController.joinGame(game);

        try {
            table = flexController.fetchTable(game);
            fail("Should throw a FlexPokerException.");
        } catch (FlexPokerException e) {}
        assertNull(table);

        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, PocketCards.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, FlopCards.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, TurnCard.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, RiverCard.class);

        SecurityContextHolder.getContext().setAuthentication(guestAuth);
        flexController.verifyRegistrationForGame(game);
        SecurityContextHolder.getContext().setAuthentication(johnAuth);
        flexController.verifyRegistrationForGame(game);

        table = flexController.fetchTable(game);
        assertNotNull(table);

        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, PocketCards.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, FlopCards.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, TurnCard.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, RiverCard.class);

        SecurityContextHolder.getContext().setAuthentication(johnAuth);
        flexController.verifyGameInProgress(game);
        SecurityContextHolder.getContext().setAuthentication(guestAuth);
        flexController.verifyGameInProgress(game);

        table = flexController.fetchTable(game);
        assertNotNull(table);

        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, FlopCards.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, TurnCard.class);
        verifyUnableToRetrieveCards(johnAuth, guestAuth, game, table, RiverCard.class);

        PocketCards guestPocketCards = flexController.fetchPocketCards(game, table);
        SecurityContextHolder.getContext().setAuthentication(johnAuth);
        PocketCards johnPocketCards = flexController.fetchPocketCards(game, table);

        assertNotNull(johnPocketCards);
        assertNotNull(guestPocketCards);
        assertFalse(johnPocketCards.equals(guestPocketCards));

        Seat actionOnSeat = (Seat) CollectionUtils.find(table.getSeats(),
                new ActionOnSeatPredicate());
        Seat johnSeat;
        Seat guestSeat;

        if (table.getSeats().get(0).getUserGameStatus().getUser().equals(john)) {
            johnSeat = table.getSeats().get(0);
            guestSeat = table.getSeats().get(1);
        } else {
            johnSeat = table.getSeats().get(1);
            guestSeat = table.getSeats().get(0);
        }

        if (actionOnSeat.equals(johnSeat)) {
            testCardRetrievalCommon(johnAuth, guestAuth, game, table);
        } else if (actionOnSeat.equals(guestSeat)) {
            testCardRetrievalCommon(guestAuth, johnAuth, game, table);
        } else {
            fail("Neither seat belongs to guest or john.");
        }

    }

    private void verifyUnableToRetrieveCards(Authentication johnAuth,
            Authentication guestAuth, Game game, Table table, Class<?> cardClass) {
        if (cardClass.equals(PocketCards.class)) {

            SecurityContextHolder.getContext().setAuthentication(johnAuth);
            try {
                flexController.fetchPocketCards(game, table);
                fail("Should throw an Exception.");
            } catch (FlexPokerException e) {
            } catch (IllegalArgumentException e) {}
            SecurityContextHolder.getContext().setAuthentication(guestAuth);
            try {
                flexController.fetchPocketCards(game, table);
                fail("Should throw an Exception.");
            } catch (FlexPokerException e) {
            } catch (IllegalArgumentException e) {}

        } else if (cardClass.equals(FlopCards.class)) {

            SecurityContextHolder.getContext().setAuthentication(johnAuth);
            try {
                flexController.fetchFlopCards(game, table);
                fail("Should throw an Exception.");
            } catch (FlexPokerException e) {
            } catch (IllegalArgumentException e) {}
            SecurityContextHolder.getContext().setAuthentication(guestAuth);
            try {
                flexController.fetchFlopCards(game, table);
                fail("Should throw an Exception.");
            } catch (FlexPokerException e) {
            } catch (IllegalArgumentException e) {}

        } else if (cardClass.equals(TurnCard.class)) {

            SecurityContextHolder.getContext().setAuthentication(johnAuth);
            try {
                flexController.fetchTurnCard(game, table);
                fail("Should throw an Exception.");
            } catch (FlexPokerException e) {
            } catch (IllegalArgumentException e) {}
            SecurityContextHolder.getContext().setAuthentication(guestAuth);
            try {
                flexController.fetchTurnCard(game, table);
                fail("Should throw an Exception.");
            } catch (FlexPokerException e) {
            } catch (IllegalArgumentException e) {}

        } else if (cardClass.equals(RiverCard.class)) {

            SecurityContextHolder.getContext().setAuthentication(johnAuth);
            try {
                flexController.fetchRiverCard(game, table);
                fail("Should throw an Exception.");
            } catch (FlexPokerException e) {
            } catch (IllegalArgumentException e) {}
            SecurityContextHolder.getContext().setAuthentication(guestAuth);
            try {
                flexController.fetchRiverCard(game, table);
                fail("Should throw an Exception.");
            } catch (FlexPokerException e) {
            } catch (IllegalArgumentException e) {}

        } else {
            fail("Invalid card class.");
        }
    }

    private void testCardRetrievalCommon(Authentication actionOnAuth,
            Authentication actionOffAuth, Game game, Table table) {
        SecurityContextHolder.getContext().setAuthentication(actionOnAuth);
        flexController.call(game, table);
        SecurityContextHolder.getContext().setAuthentication(actionOffAuth);
        flexController.check(game, table);

        verifyUnableToRetrieveCards(actionOnAuth, actionOffAuth, game, table, TurnCard.class);
        verifyUnableToRetrieveCards(actionOnAuth, actionOffAuth, game, table, RiverCard.class);

        SecurityContextHolder.getContext().setAuthentication(actionOffAuth);
        FlopCards actionOffFlopCards = flexController.fetchFlopCards(game, table);
        SecurityContextHolder.getContext().setAuthentication(actionOnAuth);
        FlopCards actionOnFlopCards = flexController.fetchFlopCards(game, table);

        assertNotNull(actionOnFlopCards);
        assertNotNull(actionOffFlopCards);
        assertTrue(actionOnFlopCards.equals(actionOffFlopCards));

        SecurityContextHolder.getContext().setAuthentication(actionOffAuth);
        flexController.check(game, table);
        SecurityContextHolder.getContext().setAuthentication(actionOnAuth);
        flexController.check(game, table);

        verifyUnableToRetrieveCards(actionOnAuth, actionOffAuth, game, table, RiverCard.class);

        SecurityContextHolder.getContext().setAuthentication(actionOffAuth);
        TurnCard actionOffTurnCard = flexController.fetchTurnCard(game, table);
        SecurityContextHolder.getContext().setAuthentication(actionOnAuth);
        TurnCard actionOnTurnCard = flexController.fetchTurnCard(game, table);

        assertNotNull(actionOnTurnCard);
        assertNotNull(actionOffTurnCard);
        assertTrue(actionOnTurnCard.equals(actionOffTurnCard));

        SecurityContextHolder.getContext().setAuthentication(actionOffAuth);
        flexController.check(game, table);
        SecurityContextHolder.getContext().setAuthentication(actionOnAuth);
        flexController.check(game, table);

        SecurityContextHolder.getContext().setAuthentication(actionOffAuth);
        RiverCard actionOffRiverCard = flexController.fetchRiverCard(game, table);
        SecurityContextHolder.getContext().setAuthentication(actionOnAuth);
        RiverCard actionOnRiverCard = flexController.fetchRiverCard(game, table);

        assertNotNull(actionOnRiverCard);
        assertNotNull(actionOffRiverCard);
        assertTrue(actionOnRiverCard.equals(actionOffRiverCard));
    }

}