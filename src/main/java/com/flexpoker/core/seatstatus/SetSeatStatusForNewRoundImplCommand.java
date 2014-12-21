package com.flexpoker.core.seatstatus;

import com.flexpoker.config.Command;
import com.flexpoker.core.api.seatstatus.SetSeatStatusForNewRoundCommand;
import com.flexpoker.model.Game;
import com.flexpoker.model.Table;

@Command
public class SetSeatStatusForNewRoundImplCommand extends BaseSeatStatusCommand implements
        SetSeatStatusForNewRoundCommand {

    @Override
    public void execute(Game game, Table table) {
        table.resetChipsInFront();
        assignNewRoundActionOn(game, table);
    }

}
