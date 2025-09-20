package com.badlogic.blackjack;

import com.badlogic.blackjack.actions.Action;
import ECS.Entity;
import com.badlogic.blackjack.actions.MoveToAction;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Sequencer {
    private ECS ecs;
    private int worldWidth;
    private int worldHeight;
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

    public void dealCardToPlayer(Card card) {
        // Now, the Sequencer translates the command into ECS actions.
        Entity cardEntity = ecs.createCardEntity(card.getSuit(), card.getRank(), worldWidth, worldHeight);

        Vector2 targetPosition = new Vector2(100, 50); // Player hand position
        addAction(new MoveToAction(cardEntity, targetPosition, 30f));
    }
}
