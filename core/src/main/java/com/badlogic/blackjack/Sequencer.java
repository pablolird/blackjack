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
    private Get g;
    private ECS ecs;
    private List<Action> actions = new ArrayList<>();


    public Sequencer(ECS ecs) {
        this.g = new Get();
        this.ecs = ecs;
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void update(float delta) {
        // Remove element avoiding iterator invalidation!
        actions.removeIf(a -> a.update(delta));
    }

    public void DealAllPlayersOrdered(String... players) {
        SequenceAction s = new SequenceAction();

        for (String p : players) {
            Action a = dealTwoCardsInOrder(p);
            s.add(a);
        }

        this.addAction(s);
    }

    // In Sequencer.java
    private Action dealTwoCardsInOrder(String player) {
        Entity card1 = ecs.createCardEntity("spades", "A");
        Entity card2 = ecs.createCardEntity("hearts", "10");

        Action move1 = new MoveToAction(card1, g.position.get(player), 300f);
        Action move2 = new MoveToAction(card2, new Vector2(g.position.get(player)).add(5f, 5f), 300f);
        // Create a single SequenceAction that will run move1, then move2.
        return new SequenceAction(move1, move2);
    }

    public void dealCardToPlayer(Card card, Vector2 targetPosition) {
        // Now, the Sequencer translates the command into ECS actions.
        Entity cardEntity = ecs.createCardEntity(card.getSuit(), card.getRank());

        addAction(new MoveToAction(cardEntity, targetPosition, 30f));
    }

    public boolean isBusy() {
        return !actions.isEmpty();
    }
}
