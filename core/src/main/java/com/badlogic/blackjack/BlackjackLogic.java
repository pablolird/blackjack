package com.badlogic.blackjack;
import java.util.ArrayList;
import java.util.List;

enum GameState { STARTING, BETTING, DEALING_DEALER, DEALING_PLAYERS, PLAYER_TURN, ANIMATIONS_IN_PROGRESS, DEALER_TURN, RESOLVING_BETS,
                 FINISHING_ROUND, GAME_OVER}

public class BlackjackLogic {
    private final Sequencer sequencer;
    int current_playerIndex;
    int numPlayers;
    Deck deck;
    Dealer dealer;
    List<Player> playersList;
    GameState gameState;
    UI gameUI;


    public BlackjackLogic(Sequencer sequencer, int n) {
        this.numPlayers = n;
        this.sequencer = sequencer;
        this.deck = new Deck();
        this.dealer = new Dealer();
        gameState = GameState.STARTING;

        playersList = new ArrayList<>();


        for (int i = 0; i < n; i++) {
            Player p = new Player("player", 100);

            playersList.add(p);
        }

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

    public void playerAddToBet(int amount)
    {
        Player currentPlayer = playersList.get(current_playerIndex);
        currentPlayer.addToBet(amount);
        gameUI.updatePlayerBalance(currentPlayer); // Update UI to show new potential bet
    }

    public void playerLockInBet() {
        Player currentPlayer = playersList.get(current_playerIndex);
        if (currentPlayer.getCurrentBet() > 0) {
            currentPlayer.lockInBet();
            gameUI.updatePlayerBalance(currentPlayer);
            if (current_playerIndex < playersList.size() - 1)
            {
                Player nextPlayer = playersList.get(current_playerIndex + 1);
                gameUI.updateCurrentPlayerColor(nextPlayer);
                current_playerIndex++;
            }
            else
            {
                current_playerIndex = 0;
                gameState = GameState.DEALING_DEALER;
                gameUI.showBettingPanel(false);
                gameUI.updateCurrentPlayerColor(null);
            }
        }
    }

    public void resolveBets()
    {
        int dealerTotal = dealer.totalValue();
        boolean busted = (dealerTotal > 21);

        for (Player p : playersList)
        {
            if (p.getCurrentBet() > 0)
            {
                int playerTotal = p.totalValue();
                double multiplier = 0.0;

                if (playerTotal > 21)
                {
                    // Player bust
                    multiplier = 0.0; // Money was already removed in betting phase due to how balance is implemented now
                }
                else if (playerTotal == 21 && p.m_currentCards.size() == 2)
                {
                    // Player blackjack
                    multiplier = 2.5;
                }
                else if (busted)
                {
                    // Dealer busted
                    multiplier = 2.0;
                }
                else if (playerTotal > dealerTotal)
                {
                    // Player Win
                    multiplier = 2.0;
                }
                else if (playerTotal == dealerTotal)
                {
                    // Tie
                    multiplier = 1.0;
                } else
                {
                    // Player Loss
                    multiplier = 0.0;
                }

                int rawReturn = (int) (multiplier * p.getCurrentBet());
                int roundedReturn = (rawReturn / 10) * 10;

                p.addBalance(roundedReturn);
                gameUI.updatePlayerBalance(p);
            }
            gameUI.updatePlayerScore(p);
        }

        gameUI.updateDealerScore(dealer);
        gameUI.showPlayerActionPanel(false);

        sequencer.moveCardsToDeck(playersList, dealer);

        dealer.reset();
        for (Player p : playersList) {
            p.reset();
            if (p.getBalance() < 1) {
                p.toggleActive();
            }
        }

        gameState = GameState.FINISHING_ROUND;
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
                playersList.removeIf(p -> !p.isActive());
                gameUI.rebuildLayout(playersList);
                if (playersList.isEmpty()) {
                    gameState = GameState.GAME_OVER; // Or a new "GAME_OVER" state
                    return; // Stop processing
                }

                // Reset player index just in case the last player was removed
                current_playerIndex = 0;
                gameState = GameState.BETTING;
                gameUI.showBettingPanel(true);
                gameUI.setCurrentPlayer(playersList.get(current_playerIndex));
                break;
            case BETTING:
                break;
            case DEALING_DEALER:
                if(!sequencer.isBusy())
                {
                    dealer.addCard(deck.drawCard());
                    sequencer.createDealCardToDealerAction(dealer);
                    gameUI.updateDealerScore(dealer);
                    gameState = GameState.DEALING_PLAYERS;
                }
                break;
            case DEALING_PLAYERS:
                gameUI.showPlayerActionPanel(false);
                this.dealInitialCards();
                break;
            case ANIMATIONS_IN_PROGRESS:
                if (!sequencer.isBusy()) {
                    gameState = GameState.PLAYER_TURN;
                }
                break;
            case PLAYER_TURN:
                if (!gameUI.ActionPanelIsVisible())
                {
                    gameUI.showPlayerActionPanel(true);
                    gameUI.setCurrentPlayer(playersList.get(current_playerIndex));
                }
                break;
            case DEALER_TURN:
                if(!sequencer.isBusy())
                {
                    if(dealer.totalValue() < 17)
                    {
                        dealer.addCard(deck.drawCard());
                        sequencer.createDealCardToDealerAction(dealer);
                        gameUI.updateDealerScore(dealer);
                    }
                    else
                    {
                        gameState = GameState.RESOLVING_BETS;
                    }
                }
                break;
            case RESOLVING_BETS:
                resolveBets();
                break;
            case FINISHING_ROUND:
                if (!sequencer.isBusy()) {
                    sequencer.clearCardEntities();
                    gameState = GameState.STARTING;
                }
                break;
            case GAME_OVER:
                gameUI.showGameOverMenu();
                break;
        }
    }
}
