package com.badlogic.blackjack;

import com.badlogic.blackjack.actions.Action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Sequencer {
    private ECS ecs;
    private List<Action> actions = new ArrayList<>();


    public Sequencer(ECS ecs) {
        this.ecs = ecs;
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void update(float delta) {
        // Remove element avoiding iterator invalidation!
        actions.removeIf(a -> a.update(delta));
    }
}
