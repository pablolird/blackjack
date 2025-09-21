package com.badlogic.blackjack;

import com.badlogic.blackjack.actions.*;
import ECS.Entity;
import ECS.CTransform;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class Sequencer {
    private Get g;
    private ECS ecs;
    private List<Action> actions = new ArrayList<>();
    private HandLayoutManager handLayoutManager;


    public Sequencer(ECS ecs) {
        this.g = new Get();
        this.ecs = ecs;
        this.handLayoutManager = new HandLayoutManager();
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void update(float delta) {
        // Remove element avoiding iterator invalidation!
        actions.removeIf(a -> a.update(delta));
    }

    public SequenceAction DealCardToPlayer(Player p, int playerIndex) {
        List<Card> cards = p.m_currentCards;
        if (cards.size() < 2) return null; // Should have 2 cards to deal

        // Get the first and second cards
        Card firstCardObject = cards.get(0);
        Card secondCardObject = cards.get(1);

        // Create entities for both cards
        Entity firstCardEntity = ecs.createCardEntity(firstCardObject);
        Entity secondCardEntity = ecs.createCardEntity(secondCardObject);

        // Define player position and card offsets
        Vector2 playerPosition = g.position.get("PLAYER" + (playerIndex + 1) + "_CARD");
        Vector2 firstCardPosition = playerPosition.cpy();
        // 1. Move the first card into position
        Action moveFirstCard = new MoveToAction(firstCardEntity, firstCardPosition, g.rotation.get("PLAYER" + (playerIndex + 1)) ,0.25f);

        // 2. Move the second card and shift the first card at the same time
        Action moveSecondCard = new MoveToAction(secondCardEntity, firstCardPosition, g.rotation.get("PLAYER" + (playerIndex + 1)) ,0.25f);
        Action shiftFirstCard = new ShiftToAction(firstCardEntity, new Vector2(-5, 0), 0.25f);

        Action parallelMoveAndShift = new ParallelAction(moveSecondCard, shiftFirstCard);

        Action inBetweenDelay = new DelayAction(0.2f);

        // 3. Create the final sequence
        SequenceAction dealInitialCardsAction = new SequenceAction(moveFirstCard, inBetweenDelay, parallelMoveAndShift);

        return dealInitialCardsAction;
    }

    public void DealInitialCards(List<Player> players) {

        SequenceAction dealInitialCardsAction = new SequenceAction();

        for(int i = 0; i < players.size(); i++)
        {
            dealInitialCardsAction.add(DealCardToPlayer(players.get(i), i));
        }

        this.addAction(dealInitialCardsAction);
    }


    public boolean isBusy() {
        return !actions.isEmpty();
    }
}
