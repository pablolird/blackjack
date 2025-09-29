package com.badlogic.blackjack;
import java.util.ArrayList;
import java.util.List;

enum GameState { STARTING, DEALING, PLAYER_TURN, ANIMATIONS_IN_PROGRESS, DEALER_TURN }

public class BlackjackLogic {
    private final Sequencer sequencer;
    int current_playerIndex;
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
        Player p5 = new Player("Alvaro", 100);
        Player p6 = new Player("Ale", 100);
        Player p7 = new Player("XD", 100);

        playersList.add(p1);
        playersList.add(p2);
        playersList.add(p3);
        playersList.add(p4);
        playersList.add(p5);
        playersList.add(p6);
        playersList.add(p7);

        this.current_playerIndex = 0;
    }

    public void nextPlayer() {
        // SHOULD ONLY BE USED FOR HIT/STAND
        if (current_playerIndex==playersList.size()-1) {
            // LAST PLAYER PRESSED STAND, GO TO NEXT GAME PHASE
            gameState = GameState.DEALER_TURN;
            gameUI.showPlayerActionPanel(false);
        }
        current_playerIndex=(current_playerIndex+1)%playersList.size();
        gameUI.updateCurrentPlayerColor(playersList.get(current_playerIndex));
    }


    public void setGameUI(UI ui) {
        this.gameUI = ui;
    }

    public void stand() {
        nextPlayer();
    }

    public void dealInitialCards() {
        if (current_playerIndex == playersList.size()) {
            current_playerIndex=0;
            gameState = GameState.ANIMATIONS_IN_PROGRESS;
            return;
        }

        Player curentPlayer = playersList.get(current_playerIndex);
        if (sequencer.isBusy()) {
            gameUI.updatePlayerScore(playersList.get(current_playerIndex));

            if (curentPlayer.m_currentCards.size() > 1 ) {
                // CONSIDER USING MODULAR ARITHMETIC IF POSSIBLE AT SOME MOMENT
                current_playerIndex++;
            }
            return;
        }

        curentPlayer.addCard(deck.drawCard());
        sequencer.createDealCardAction(curentPlayer, current_playerIndex);
    }

    public void hit() {
        if (!gameState.equals(GameState.PLAYER_TURN) || sequencer.isBusy()) {
            return;
        }

        Player p = playersList.get(current_playerIndex);
        p.addCard(deck.drawCard());
        sequencer.createDealCardAction(p,current_playerIndex);
        gameUI.updatePlayerScore(p);

        if (p.totalValue()>21) {
            nextPlayer();
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
                break;
            case ANIMATIONS_IN_PROGRESS:
                if (!sequencer.isBusy()) {
                    gameState = GameState.PLAYER_TURN;
                }
                break;
            case PLAYER_TURN:
                if (!gameUI.ActionPanelIsVisible()) {
                    gameUI.showPlayerActionPanel(true);
                    gameUI.setCurrentPlayer(playersList.get(current_playerIndex));
                }
                break;
            case DEALER_TURN:
                break;
        }
    }
}
