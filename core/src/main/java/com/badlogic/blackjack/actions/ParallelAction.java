package com.badlogic.blackjack.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParallelAction implements Action {
    private List<Action> actions;

    public ParallelAction(Action... actions) {
        this.actions = new ArrayList<>(Arrays.asList(actions));
    }

    @Override
    public boolean update(float delta) {
        actions.removeIf(a -> a.update(delta));
        return actions.isEmpty();
    }
}
