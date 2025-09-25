package com.badlogic.blackjack.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParallelAction implements Action {
    private final List<Action> actions;

    public ParallelAction(Action... actions) {
        this.actions = new ArrayList<>(Arrays.asList(actions));
    }

    public void add(Action a) {
        actions.add(a);
    }

    @Override
    public boolean update(float delta) {
        actions.removeIf(a -> a.update(delta));
        return actions.isEmpty();
    }
}
