package com.badlogic.blackjack;

enum GameState { STARTING, DEALING, PLAYER_TURN }

public class BlackjackLogic {
    private Sequencer sequencer;
    Deck deck;
    GameState gameState;


    public BlackjackLogic(Sequencer sequencer) {
        this.sequencer = sequencer;
        this.deck = new Deck();
        gameState = GameState.STARTING;
    }

    public void dealInitialCards() {
        // ... logic to get card data from the deck ...
        Card playerCard1 = deck.drawCard();

        // Issue a high-level command to the Sequencer.
        // Notice we are not touching the ECS at all.
        sequencer.dealCardToPlayer(playerCard1);
    }

    public void update(float delta) {
        switch (gameState) {
            case STARTING:
                gameState = GameState.DEALING;
                break;
            case DEALING:

                this.dealInitialCards();

                gameState = GameState.PLAYER_TURN;
                break;
            case PLAYER_TURN:

                break;
        }
    }
}
