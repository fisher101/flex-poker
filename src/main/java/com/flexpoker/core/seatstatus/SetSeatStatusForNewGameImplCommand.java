package com.flexpoker.core.seatstatus;

import com.flexpoker.config.Command;
import com.flexpoker.core.api.seatstatus.SetSeatStatusForNewGameCommand;
import com.flexpoker.model.Game;
import com.flexpoker.model.Table;

@Command
public class SetSeatStatusForNewGameImplCommand extends BaseSeatStatusCommand implements
        SetSeatStatusForNewGameCommand {

    @Override
    public void execute(Game game, Table table) {
        table.resetStillInHand();
        assignNewGameButton(table);
        assignNewGameSmallBlind(table);
        assignNewGameBigBlind(table);
        assignNewHandActionOn(game, table);
    }

}
