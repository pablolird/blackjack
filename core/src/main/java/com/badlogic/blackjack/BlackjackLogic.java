package com.badlogic.blackjack;

import com.badlogic.gdx.math.Vector2;

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
        // USAGE:
        //  - GENERATE CARDS USING deck.drawCard()
        //  - DEAL CARD TO PLAYER USING sequencer.dealCardToPlayer()
        sequencer.dealTwoCardsInOrder();
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
