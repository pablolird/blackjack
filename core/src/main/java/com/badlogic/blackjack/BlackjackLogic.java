package com.badlogic.blackjack;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.blackjack.GameState;
import com.badlogic.blackjack.GameStateListener;

public class BlackjackLogic {
    private final Sequencer sequencer;
    int current_playerIndex;
    int numPlayers;
    Deck deck;
    Dealer dealer;
    List<Player> playersList;
    GameState gameState;
    UI gameUI;


    private GameStateListener stateListener;

    public BlackjackLogic(Sequencer sequencer, List<String> playerNames) {
        this.numPlayers = playerNames.size();
        this.sequencer = sequencer;
        this.deck = new Deck();
        this.dealer = new Dealer();
        gameState = GameState.STARTING;

        playersList = new ArrayList<>();

        // Use the actual player names from the network
        for (String name : playerNames) {
            Player p = new Player(name, 100);
            playersList.add(p);
        }

        this.current_playerIndex = 0;
    }

    // --- OVERLOADED CONSTRUCTOR (Kept for compatibility with old local game calls) ---
    public BlackjackLogic(Sequencer sequencer, int n) {
        // Create dummy names for local game initialization
        List<String> dummyNames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            dummyNames.add("Player " + (i+1));
        }
        this.numPlayers = n;
        this.sequencer = sequencer;
        this.deck = new Deck();
        this.dealer = new Dealer();
        gameState = GameState.STARTING;

        playersList = new ArrayList<>();


        for (String name : dummyNames) {
            Player p = new Player(name, 100);

            playersList.add(p);
        }

        this.current_playerIndex = 0;
    }

    public GameState getGameState() {
        return gameState;
    }

    public int getCurrentPlayerIndex() {
        return current_playerIndex;
    }

    public List<Player> getPlayersList() {
        return playersList;
    }

    public Dealer getDealer() {
        return dealer;
    }

    public void setGameStateListener(GameStateListener listener) {
        this.stateListener = listener;
    }

    private void notifyStateChanged() {
        if (stateListener != null) {
            stateListener.onGameStateChanged();
        }
    }

    public void nextPlayer() {
        if (current_playerIndex==playersList.size()-1) {
            gameState = GameState.DEALER_TURN;
            if(gameUI != null) gameUI.showPlayerActionPanel(false); // Check for null
            notifyStateChanged(); // NOTIFY
        }
        current_playerIndex=(current_playerIndex+1)%playersList.size();
        if(gameUI != null) gameUI.updateCurrentPlayerColor(playersList.get(current_playerIndex)); // Check for null
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
        if (gameUI != null) {
            gameUI.updatePlayerBalance(currentPlayer); // Update UI to show new potential bet
        }
    }

    public void playerLockInBet() {
        Player currentPlayer = playersList.get(current_playerIndex);
        if (currentPlayer.getCurrentBet() > 0) {
            currentPlayer.lockInBet();
            if(gameUI != null) gameUI.updatePlayerBalance(currentPlayer); // Check for null

            if (current_playerIndex < playersList.size() - 1)
            {
                Player nextPlayer = playersList.get(current_playerIndex + 1);
                if(gameUI != null) gameUI.updateCurrentPlayerColor(nextPlayer); // Check for null
                current_playerIndex++;
                notifyStateChanged(); // NOTIFY (Next player's turn to bet)
            }
            else
            {
                current_playerIndex = 0;
                gameState = GameState.DEALING_DEALER;
                if(gameUI != null) {
                    gameUI.showBettingPanel(false);
                    gameUI.updateCurrentPlayerColor(null);
                }
                notifyStateChanged(); // NOTIFY (Betting finished)
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
                if (gameUI != null) {
                    gameUI.updatePlayerBalance(p);
                }
            }
            if (gameUI != null) {
                gameUI.updatePlayerScore(p);
            }
        }

        if (gameUI != null) {
            gameUI.updateDealerScore(dealer);
            gameUI.showPlayerActionPanel(false);
        }

        if (sequencer != null) sequencer.moveCardsToDeck(playersList, dealer); // Check for null

        dealer.reset();
        for (Player p : playersList) {
            p.reset();
            if (p.getBalance() < 1) {
                p.toggleActive();
            }
        }

        gameState = GameState.FINISHING_ROUND;
        notifyStateChanged(); // NOTIFY
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

        curentPlayer.addCard(deck.deal());
        sequencer.createDealCardAction(curentPlayer, current_playerIndex);
    }

    public void hit() {
        if (!gameState.equals(GameState.PLAYER_TURN) || (sequencer != null && sequencer.isBusy())) { // Check for null
            return;
        }

        Player p = playersList.get(current_playerIndex);
        p.addCard(deck.deal());
        if(sequencer != null) sequencer.createDealCardAction(p,current_playerIndex); // Check for null
        if(gameUI != null) gameUI.updatePlayerScore(p); // Check for null

        if (p.totalValue()>21) {
            nextPlayer();
            // We notify inside nextPlayer()
        }
    }

    public void update(float delta) {
        switch (gameState) {
            case STARTING:
                deck.reset();
                playersList.removeIf(p -> !p.isActive());
                if(gameUI != null) gameUI.rebuildLayout(playersList); // Check for null
                if (playersList.isEmpty()) {
                    gameState = GameState.GAME_OVER; // Or a new "GAME_OVER" state
                    notifyStateChanged(); // NOTIFY
                    return; // Stop processing
                }

                // Reset player index just in case the last player was removed
                current_playerIndex = 0;
                gameState = GameState.BETTING;
                if(gameUI != null) {
                    gameUI.showBettingPanel(true);
                    gameUI.setCurrentPlayer(playersList.get(current_playerIndex));
                }
                notifyStateChanged(); // NOTIFY (Initial BETTING state)
                break;
            case BETTING:
                // Logic is now driven by playerLockInBet()
                break;
            case DEALING_DEALER:
                if(sequencer == null || !sequencer.isBusy()) // Check for null
                {
                    dealer.addCard(deck.deal());
                    if(sequencer != null) sequencer.createDealCardToDealerAction(dealer); // Check for null
                    if(gameUI != null) gameUI.updateDealerScore(dealer); // Check for null
                    gameState = GameState.DEALING_PLAYERS;

                    notifyStateChanged(); // NOTIFY (Dealing dealer finished)
                }
                break;
            case DEALING_PLAYERS:
                if(gameUI != null) gameUI.showPlayerActionPanel(false);

                if (sequencer != null && sequencer.isBusy()) {
                    // CLIENT-only logic: Wait for animations to finish.
                    if (current_playerIndex < playersList.size()) {
                        if(gameUI != null) gameUI.updatePlayerScore(playersList.get(current_playerIndex));
                    }
                    if (current_playerIndex > 0 && playersList.get(current_playerIndex-1).m_currentCards.size() > 1 ) {
                        current_playerIndex++;
                    } else if (current_playerIndex == 0 && playersList.get(0).m_currentCards.size() > 1) {
                        current_playerIndex++;
                    }
                    return;
                }

                // --- This logic runs if sequencer is NOT busy, or if sequencer is NULL ---

                // Check if all players have 2 cards. If so, we are done.
                if (playersList.get(playersList.size() - 1).m_currentCards.size() == 2) {
                    current_playerIndex = 0; // Reset for PLAYER_TURN
                    gameState = GameState.ANIMATIONS_IN_PROGRESS;
                    notifyStateChanged(); // Tell GameServer to auto-tick to ANIMATIONS_IN_PROGRESS
                    break; // We are done with this state
                }

                // If we're here, someone needs a card.

                // Check if we've looped through all players once
                if (current_playerIndex == playersList.size()) {
                    current_playerIndex = 0; // Reset for the second card pass
                }

                // Get the player who is next in line
                Player curentPlayer = playersList.get(current_playerIndex);

                // If they don't have 2 cards, deal one.
                if (curentPlayer.m_currentCards.size() < 2) {
                    curentPlayer.addCard(deck.deal());
                    if (sequencer != null) {
                        // CLIENT: Create animation
                        sequencer.createDealCardAction(curentPlayer, current_playerIndex);
                    }
                }

                // Move to the next player for the *next* tick
                current_playerIndex++;

                if (sequencer == null) {
                    // SERVER: No animation, so notify immediately to tick again.
                    notifyStateChanged();
                }
                // Client will 'break' and wait for sequencer.isBusy() next frame
                break;
            case ANIMATIONS_IN_PROGRESS:
                if (sequencer == null || !sequencer.isBusy()) { // Check for null
                    gameState = GameState.PLAYER_TURN;
                    notifyStateChanged(); // NOTIFY
                }
                break;
            case PLAYER_TURN:
                if (gameUI != null && !gameUI.ActionPanelIsVisible()) // Check for null
                {
                    gameUI.showPlayerActionPanel(true);
                    gameUI.setCurrentPlayer(playersList.get(current_playerIndex));
                }
                break;
            case DEALER_TURN:
                if(sequencer == null || !sequencer.isBusy()) // Check for null
                {
                    if(dealer.totalValue() < 17)
                    {
                        dealer.addCard(deck.deal());
                        if(sequencer != null) sequencer.createDealCardToDealerAction(dealer); // Check for null
                        if(gameUI != null) gameUI.updateDealerScore(dealer); // Check for null
                        // No notify, dealer might hit again
                    }
                    else
                    {
                        gameState = GameState.RESOLVING_BETS;
                        notifyStateChanged(); // NOTIFY
                    }
                }
                break;
            case RESOLVING_BETS:
                resolveBets();
                // We notify inside resolveBets()
                break;
            case FINISHING_ROUND:
                if (sequencer == null || !sequencer.isBusy()) { // Check for null
                    if(sequencer != null) sequencer.clearCardEntities(); // Check for null
                    gameState = GameState.STARTING;
                    notifyStateChanged(); // NOTIFY
                }
                break;
            case GAME_OVER:
                if(gameUI != null) gameUI.showGameOverMenu(); // Check for null
                break;
        }
    }
}
