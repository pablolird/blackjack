package com.badlogic.blackjack;

import com.badlogic.blackjack.actions.Action;
import ECS.Entity;
import com.badlogic.blackjack.actions.MoveToAction;
import com.badlogic.blackjack.actions.SequenceAction;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Sequencer {
    private ECS ecs;
    private int worldWidth = 320;
    private int worldHeight = 180;
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

    // In Sequencer.java
    public void dealTwoCardsInOrder() {
        Entity card1 = ecs.createCardEntity("spades", "A",worldWidth, worldHeight);
        Entity card2 = ecs.createCardEntity("hearts", "10",worldWidth, worldHeight);

        Action move1 = new MoveToAction(card1, new Vector2(100, 50), 300f);
        Action move2 = new MoveToAction(card2, new Vector2(125, 50), 300f);

        // Create a single SequenceAction that will run move1, then move2.
        Action sequence = new SequenceAction(move1, move2);

        // Add only the one container action to the main sequencer list.
        this.addAction(sequence);
    }

    public void dealCardToPlayer(Card card, Vector2 targetPosition) {
        // Now, the Sequencer translates the command into ECS actions.
        Entity cardEntity = ecs.createCardEntity(card.getSuit(), card.getRank(), worldWidth, worldHeight);

        addAction(new MoveToAction(cardEntity, targetPosition, 30f));
    }

    public boolean isBusy() {
        return !actions.isEmpty();
    }
}
