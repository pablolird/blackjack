package com.badlogic.blackjack.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SequenceAction implements Action {
    private List<Action> actions;

    public SequenceAction(Action... actions) {
        // Takes a set of action parameters and converts it to an array
        this.actions = new ArrayList<>(Arrays.asList(actions));
    }

    @Override
    public boolean update(float delta) {
        if (actions.isEmpty()) return true;

        Action currentAction = actions.get(0);

        if (currentAction.update(delta)) {
            actions.remove(0);
        }

        return actions.isEmpty();
    }
}
