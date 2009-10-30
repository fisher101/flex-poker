package com.flexpoker.bso;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.flexpoker.model.Seat;
import com.flexpoker.model.Table;
import com.flexpoker.model.UserGameStatus;
import com.flexpoker.util.Context;
import com.flexpoker.util.DataUtilsForTests;


public class ValidationBsoImplTest {

    private ValidationBsoImpl bso = (ValidationBsoImpl) Context.instance()
            .getBean("validationBso");
    
    @Test
    public void testValidateTable() {
        Table table = new Table();
        List<Seat> seats = new ArrayList<Seat>();
        table.setSeats(seats);

        try {
            bso.validateTable(table);
            fail("Should have thrown an IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}

        Seat seat1 = new Seat();
        seat1.setPosition(0);
        seats.add(seat1);

        try {
            bso.validateTable(table);
            fail("Should have thrown an IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}

        Seat seat2 = new Seat();
        seat2.setPosition(1);
        seats.add(seat2);

        try {
            bso.validateTable(table);
            fail("Should have thrown an IllegalArgumentException.");
        } catch (IllegalArgumentException e) {}

        seat1.setUserGameStatus(new UserGameStatus());
        seat2.setUserGameStatus(new UserGameStatus());

        bso.validateTable(table);
    }

    @Test
    public void testValidateTableAssignment() {
        Set<UserGameStatus> userGameStatuses = null;
        try {
            bso.validateTableAssignment(userGameStatuses, 9);
            fail("An exception should have been thrown.  Can't send in an empty Set.");
        } catch (IllegalArgumentException e) {}

        userGameStatuses = DataUtilsForTests.createUserGameStatusSet(0);
        try {
            bso.validateTableAssignment(userGameStatuses, 9);
            fail("An exception should have been thrown.  Can't send in an empty Set.");
        } catch (IllegalArgumentException e) {}

        userGameStatuses = DataUtilsForTests.createUserGameStatusSet(7);
        try {
            bso.validateTableAssignment(userGameStatuses, 10);
            fail("An exception should have been thrown.  10 is too large of a table.");
        } catch (IllegalArgumentException e) {}
        try {
            bso.validateTableAssignment(userGameStatuses, 1);
            fail("An exception should have been thrown.  1 is too small of a table.");
        } catch (IllegalArgumentException e) {}
        try {
            bso.validateTableAssignment(userGameStatuses, 2);
            fail("A heads-up tournament must start with an even number of people.");
        } catch (IllegalArgumentException e) {}
    }

}
