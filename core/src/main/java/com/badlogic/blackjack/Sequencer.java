package com.badlogic.blackjack;

import com.badlogic.blackjack.actions.*;
import ECS.Entity;
import com.badlogic.blackjack.audio.AudioManager;
import com.badlogic.blackjack.audio.SoundType;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.blackjack.actions.PlaySFXAction;

import java.util.ArrayList;
import java.util.List;

public class Sequencer {
    private final Get g;
    private final ECS ecs;
    private final List<Action> actions = new ArrayList<>();
    //private HandLayoutManager handLayoutManager;
    private AudioManager audioManager;

    public Sequencer(ECS ecs, AudioManager audioManager) {
        this.g = new Get();
        this.ecs = ecs;
        this.audioManager = audioManager;
        //this.handLayoutManager = new HandLayoutManager();
    }

    public void clearCardEntities() {
        ecs.clearCardEntities();
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void update(float delta) {
        // Remove element avoiding iterator invalidation
        actions.removeIf(a -> a.update(delta));
    }

    public void moveCardsToDeck(List<Player> players, Dealer dealer) {
        SequenceAction sequenceAction = new SequenceAction();

        for (int i = 0; i < players.size(); i++) {
            sequenceAction.add(CreateReturnCardsToDeckAction(players.get(i)));
        }

        sequenceAction.add(CreateReturnCardsToDeckAction(dealer));

        actions.add(sequenceAction);
    }

    public Action CreateReturnCardsToDeckAction(Dealer p) {
        float duration = 0.25f;
        List<Card> cards = p.m_currentCards;

        Vector2 deckPosition = g.position.get("DECK");
        float deckRotation = 0f;

        ParallelAction sequenceAction = new ParallelAction();

        for (Card c : cards) {
            Entity newCardEntity = ecs.findCardEntity(c.m_id);
            // Create entity if it doesn't exist (safety measure for synced cards)
            if (newCardEntity == null) {
                newCardEntity = ecs.createCardEntity(c);
            }
            // Only add action if entity exists (should always exist after creation attempt)
            if (newCardEntity != null) {
                Action moveCard = moveCardAction(newCardEntity, deckPosition, deckRotation, duration);
                sequenceAction.add(moveCard);
            }
        }
        return sequenceAction;
    }

    public Action moveCardAction(Entity cardEntity, Vector2 position, float rotation, float duration) {
        if (cardEntity == null) {
            // Return a no-op action if entity is null
            return new DelayAction(0f);
        }
        ParallelAction p = new ParallelAction();
        Action moveAction = new MoveToAction(cardEntity, position, rotation, duration);
        Action playCardSound = new PlaySFXAction(audioManager, SoundType.CARD_DEAL, 0.95f);
        p.add(moveAction);
        p.add(playCardSound);

        return p;
    }

    public void createDealCardToDealerAction(Dealer dealer) {
        float duration = 0.25f;
        List<Card> cards = dealer.m_currentCards;
        if (cards.isEmpty()) {
            return;
        }

        Card newCardObject = cards.get(cards.size() - 1);
        Entity newCardEntity = ecs.findCardEntity(newCardObject.m_id);

        if (newCardEntity == null) {
            newCardEntity = ecs.createCardEntity(newCardObject);
        }

        ParallelAction parallelAction = new ParallelAction();
        Vector2 shiftAmount = g.shift.get("DEALER");

        for (int i = 0; i < cards.size() - 1; i++) {
            Card existingCard = cards.get(i);
            Entity existingEntity = ecs.findCardEntity(existingCard.m_id);

            if (existingEntity != null) {
                Action shiftAction = new ShiftToAction(existingEntity, shiftAmount, duration);
                parallelAction.add(shiftAction);
            }
        }

        Vector2 dealerPosition = g.position.get("DEALER_CARD");
        float dealerRotation = g.rotation.get("DEALER");

        Action moveAction = new MoveToAction(newCardEntity, dealerPosition.cpy(), dealerRotation, duration);
        parallelAction.add(moveAction);

        Action playCardSound = new PlaySFXAction(audioManager, SoundType.CARD_DEAL, 0.95f);
        parallelAction.add(playCardSound);

        // Add a delay between dealer deal cards to add tension to game
        SequenceAction s = new SequenceAction(parallelAction,new DelayAction(2));

        this.actions.add(s);
    }

    /**
     * Creates a single, parallel action that animates a player receiving their newest card.
     * It finds all of the player's existing card entities and creates actions to shift them,
     * while the new card (which was just created) is animated from the deck to its final position.
     *
     * @param p The Player receiving the card.
     * @param playerIndex The index of the player (for positioning) - used as fallback if UI is null.
     * @param ui Optional UI reference to look up visual position index. If provided, uses visual position instead of array index.
     * @return A ParallelAction containing all the necessary animations.
     */
    // MODIFICATIONS TO MAKE:
    //  - MAKE CARDS CENTERED WITH RESPECT TO PLAYER DECK POSITION
    public void createDealCardAction(Player p, int playerIndex) {
        createDealCardAction(p, playerIndex, -1, null);
    }

    public void createDealCardAction(Player p, int playerIndex, int cardId) {
        createDealCardAction(p, playerIndex, cardId, null);
    }

    public void createDealCardAction(Player p, int playerIndex, int cardId, UI ui) {
        float duration = 0.25f;
        List<Card> cards = p.m_currentCards;
        if (cards.isEmpty()) {
            return; // Cannot deal a card if the hand is empty.
        }

        // Determine visual position index: use UI lookup if available, otherwise use array index
        int visualPositionIndex;
        if (ui != null) {
            int visualPos = ui.getVisualPositionIndex(p);
            visualPositionIndex = visualPos > 0 ? visualPos : (playerIndex + 1); // Fallback to array index if not found
        } else {
            visualPositionIndex = playerIndex + 1; // Use array index as fallback
        }

        // The newest card is the last one in the list.
        Card newCardObject = null;
        if (cardId >= 0) {
            for (Card c : cards) {
                if (c.m_id == cardId) {
                    newCardObject = c;
                    break;
                }
            }
        }
        if (newCardObject == null) {
            newCardObject = cards.get(cards.size() - 1);
        }
        Entity newCardEntity = ecs.findCardEntity(newCardObject.m_id);

        // If the entity for the new card doesn't exist yet, create it.
        if (newCardEntity == null) {
            newCardEntity = ecs.createCardEntity(newCardObject);
        }

        // This action will hold all shifting and moving animations so they run at the same time.
        ParallelAction parallelAction = new ParallelAction();

        // Get the specific shift vector for this player position using visual position index.
        Vector2 shiftAmount = g.shift.get("PLAYER" + visualPositionIndex);

        // 1. Create ShiftToAction for all existing cards (from index 0 to n-2).
        for (Card existingCard : cards) {
            if (existingCard == newCardObject) {
                continue;
            }
            Entity existingEntity = ecs.findCardEntity(existingCard.m_id);

            if (existingEntity != null) {
                Action shiftAction = new ShiftToAction(existingEntity, shiftAmount, duration);
                parallelAction.add(shiftAction);
            }
        }

        // 2. Create a MoveToAction for the new card.
        // The target position is the base position for that player's hand using visual position index.
        Vector2 playerPosition = g.position.get("PLAYER" + visualPositionIndex + "_CARD");
        float playerRotation = g.rotation.get("PLAYER" + visualPositionIndex);

        Action moveAction = new MoveToAction(newCardEntity, playerPosition.cpy(), playerRotation, duration);
        parallelAction.add(moveAction);

        Action playCardSound = new PlaySFXAction(audioManager, SoundType.CARD_DEAL, 0.95f);
        parallelAction.add(playCardSound);

        this.actions.add(parallelAction);
    }

    public SequenceAction DealCardToPlayer(Player p, int playerIndex) {
        return DealCardToPlayer(p, playerIndex, null);
    }

    public SequenceAction DealCardToPlayer(Player p, int playerIndex, UI ui) {
        float duration = 3f;
        List<Card> cards = p.m_currentCards;

        // Determine visual position index: use UI lookup if available, otherwise use array index
        int visualPositionIndex;
        if (ui != null) {
            int visualPos = ui.getVisualPositionIndex(p);
            visualPositionIndex = visualPos > 0 ? visualPos : (playerIndex + 1); // Fallback to array index if not found
        } else {
            visualPositionIndex = playerIndex + 1; // Use array index as fallback
        }

        // Get the first and second cards
        Card firstCardObject = cards.get(0);
        Card secondCardObject = cards.get(1);

        // Create entities for both cards
        Entity firstCardEntity = ecs.createCardEntity(firstCardObject);
        Entity secondCardEntity = ecs.createCardEntity(secondCardObject);

        // Define player position and card offsets using visual position index
        Vector2 playerPosition = g.position.get("PLAYER" + visualPositionIndex + "_CARD");
        Vector2 firstCardPosition = playerPosition.cpy();
        // 1. Move the first card into position
        Action moveFirstCard = new MoveToAction(firstCardEntity, firstCardPosition, g.rotation.get("PLAYER" + visualPositionIndex) ,duration);

        // 2. Move the second card and shift the first card at the same time
        Action moveSecondCard = new MoveToAction(secondCardEntity, firstCardPosition, g.rotation.get("PLAYER" + visualPositionIndex) ,duration);
        Action shiftFirstCard = new ShiftToAction(firstCardEntity, g.shift.get("PLAYER" + visualPositionIndex), duration);

        Action parallelMoveAndShift = new ParallelAction(moveSecondCard, shiftFirstCard);

        Action inBetweenDelay = new DelayAction(duration);

        // 3. Create the final sequence
        return new SequenceAction(moveFirstCard, inBetweenDelay, parallelMoveAndShift);
    }

    public void DealInitialCards(List<Player> players) {
        DealInitialCards(players, null);
    }

    public void DealInitialCards(List<Player> players, UI ui) {
        SequenceAction dealInitialCardsAction = new SequenceAction();

        for(int i = 0; i < players.size(); i++)
        {
            dealInitialCardsAction.add(DealCardToPlayer(players.get(i), i, ui));
        }

        this.addAction(dealInitialCardsAction);
    }


    public boolean isBusy() {
        return !actions.isEmpty();
    }
}
