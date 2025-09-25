package com.badlogic.blackjack;
import java.util.ArrayList;
import java.util.List;

enum GameState { STARTING, DEALING, PLAYER_TURN, ANIMATIONS_IN_PROGRESS }

public class BlackjackLogic {
    private final Sequencer sequencer;
    Deck deck;
    List<Player> playersList;
    GameState gameState;
    UI gameUI;


    public BlackjackLogic(Sequencer sequencer) {
        this.sequencer = sequencer;
        this.deck = new Deck();
        gameState = GameState.STARTING;

        playersList = new ArrayList<>();

        Player p1 = new Player("Pablo", 100);
        Player p2 = new Player("Pedro", 100);
        Player p3 = new Player("Sebas", 100);
        Player p4 = new Player("Carlos", 100);
        Player p5 = new Player("Damn", 100);
        Player p6 = new Player("Lol", 100);
        Player p7 = new Player("XD", 100);

        playersList.add(p1);
        playersList.add(p2);
        playersList.add(p3);
        playersList.add(p4);
        playersList.add(p5);
        playersList.add(p6);
        playersList.add(p7);
    }

    public void setGameUI(UI ui) {
        this.gameUI = ui;
    }

    public void dealInitialCards() {
        // EVENTUALLY CHANGE THIS, I DON'T LIKE HOW DEALINITIALCARDS() WORKS, WAY TOO SPECIFIC AND HARDCODED
        for (Player p : playersList) {
            p.addCard(deck.drawCard());
            gameUI.updatePlayerScore(p);
            p.addCard(deck.drawCard());
            gameUI.updatePlayerScore(p);
        }

        sequencer.DealInitialCards(playersList);
    }

    public void dealNewCard() {
        if (!gameState.equals(GameState.PLAYER_TURN) || sequencer.isBusy()) {
            return;
        }

        for (int i = 0; i < playersList.size(); i++) {
            Player p = playersList.get(i);
            p.addCard(deck.drawCard());
            sequencer.createDealCardAction(p,i);
            gameUI.updatePlayerScore(p);
        }
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
                if (!gameUI.ActionPanelIsVisible()) {
                    gameUI.showPlayerActionPanel(true);
                }
                break;
        }
    }
}
