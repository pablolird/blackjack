package com.badlogic.blackjack;

import com.badlogic.blackjack.actions.*;
import ECS.Entity;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class Sequencer {
    private final Get g;
    private final ECS ecs;
    private final List<Action> actions = new ArrayList<>();
    //private HandLayoutManager handLayoutManager;


    public Sequencer(ECS ecs) {
        this.g = new Get();
        this.ecs = ecs;
        //this.handLayoutManager = new HandLayoutManager();
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void update(float delta) {
        // Remove element avoiding iterator invalidation!
        actions.removeIf(a -> a.update(delta));
    }

    /**
     * Creates a single, parallel action that animates a player receiving their newest card.
     * It finds all of the player's existing card entities and creates actions to shift them,
     * while the new card (which was just created) is animated from the deck to its final position.
     *
     * @param p The Player receiving the card.
     * @param playerIndex The index of the player (for positioning).
     * @return A ParallelAction containing all the necessary animations.
     */
    // MODIFICATIONS TO MAKE:
    //  - MAKE CARDS CENTERED WITH RESPECT TO PLAYER DECK POSITION
    public void createDealCardAction(Player p, int playerIndex) {
        List<Card> cards = p.m_currentCards;
        if (cards.isEmpty()) {
            return; // Cannot deal a card if the hand is empty.
        }

        // The newest card is the last one in the list.
        Card newCardObject = cards.get(cards.size() - 1);
        Entity newCardEntity = ecs.findCardEntity(newCardObject.m_id);

        // If the entity for the new card doesn't exist yet, create it.
        if (newCardEntity == null) {
            newCardEntity = ecs.createCardEntity(newCardObject);
        }

        // This action will hold all shifting and moving animations so they run at the same time.
        ParallelAction parallelAction = new ParallelAction();

        // Get the specific shift vector for this player position.
        Vector2 shiftAmount = g.shift.get("PLAYER" + (playerIndex + 1));

        // 1. Create ShiftToAction for all existing cards (from index 0 to n-2).
        for (int i = 0; i < cards.size() - 1; i++) {
            Card existingCard = cards.get(i);
            Entity existingEntity = ecs.findCardEntity(existingCard.m_id);

            if (existingEntity != null) {
                Action shiftAction = new ShiftToAction(existingEntity, shiftAmount, 0.25f);
                parallelAction.add(shiftAction);
            }
        }

        // 2. Create a MoveToAction for the new card.
        // The target position is the base position for that player's hand.
        Vector2 playerPosition = g.position.get("PLAYER" + (playerIndex + 1) + "_CARD");
        float playerRotation = g.rotation.get("PLAYER" + (playerIndex + 1));

        Action moveAction = new MoveToAction(newCardEntity, playerPosition.cpy(), playerRotation, 0.25f);
        parallelAction.add(moveAction);

        this.actions.add(parallelAction);
    }

    public SequenceAction DealCardToPlayer(Player p, int playerIndex) {
        List<Card> cards = p.m_currentCards;

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
        Action shiftFirstCard = new ShiftToAction(firstCardEntity, g.shift.get("PLAYER" + (playerIndex + 1)), 0.25f);

        Action parallelMoveAndShift = new ParallelAction(moveSecondCard, shiftFirstCard);

        Action inBetweenDelay = new DelayAction(0.2f);

        // 3. Create the final sequence
        return new SequenceAction(moveFirstCard, inBetweenDelay, parallelMoveAndShift);
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
