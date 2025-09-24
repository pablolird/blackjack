package com.badlogic.blackjack;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

enum GameState { STARTING, DEALING, PLAYER_TURN, ANIMATIONS_IN_PROGRESS }

public class BlackjackLogic {
    private Sequencer sequencer;
    Deck deck;
    List<Player> playersList;
    GameState gameState;
    UI gameUI;


    public BlackjackLogic(Sequencer sequencer, UI gameUI) {
        this.sequencer = sequencer;
        this.gameUI = gameUI;
        this.deck = new Deck();
        gameState = GameState.STARTING;

        playersList = new ArrayList<>();

        Player p1 = new Player("Pedo", 100);
        Player p2 = new Player("Pedo", 100);
        Player p3 = new Player("Pedo", 100);
        Player p4 = new Player("Pedo", 100);
        Player p5 = new Player("Pedo", 100);
        Player p6 = new Player("Pedo", 100);
        Player p7 = new Player("Pedo", 100);

        playersList.add(p1);
        playersList.add(p2);
        playersList.add(p3);
        playersList.add(p4);
        playersList.add(p5);
        playersList.add(p6);
        playersList.add(p7);
    }

    public void dealInitialCards() {

        for (int i = 0; i < playersList.size(); i++) {
            Player p = playersList.get(i);

            p.addCard(deck.drawCard());
            p.addCard(deck.drawCard());
        }

        sequencer.DealInitialCards(playersList);



        // USAGE:
        //  - GENERATE CARDS USING deck.drawCard()
        //  - DEAL CARD TO PLAYER USING sequencer.dealCardToPlayer()
//        sequencer.DealAllPlayersOrdered("PLAYER4_CARD");
    }

    public void update(float delta) {
        switch (gameState) {
            case STARTING:
                gameState = GameState.DEALING;
                break;
            case DEALING:
                gameUI.showPlayerActionPanel(false);

                this.dealInitialCards();

                gameState = GameState.ANIMATIONS_IN_PROGRESS;
                break;
            case ANIMATIONS_IN_PROGRESS:
                if (!sequencer.isBusy()) {
                    gameState = GameState.PLAYER_TURN;
                }
                break;
            case PLAYER_TURN:

                gameUI.showPlayerActionPanel(true);
                break;
        }
    }
}
