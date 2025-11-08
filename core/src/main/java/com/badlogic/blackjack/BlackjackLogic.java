package com.badlogic.blackjack;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.blackjack.GameState;
import com.badlogic.blackjack.GameStateListener;
import com.badlogic.gdx.Gdx;

public class BlackjackLogic {
    private final Sequencer sequencer;
    int current_playerIndex;
    int numPlayers;
    Deck deck;
    Dealer dealer;
    List<Player> playersList;
    GameState gameState;
    UI gameUI;
    private boolean betsResolved = false;
    private float resolvingBetsDelay = 0f;
    private static final float RESOLVING_BETS_DELAY_TIME = 2.0f; // 2 second delay to view results
    private float cardReturnAnimationDelay = 0f;
    private static final float CARD_RETURN_ANIMATION_TIME = 1.75f; // Time to wait for card return animation to complete (0.25s per player + dealer)
    private float dealerTurnDelay = 0f;
    private static final float DEALER_TURN_DELAY_TIME = 1.0f; // 1 second delay before dealer turn starts
    private float playerActionTimer = 0f;
    private static final float PLAYER_ACTION_TIMEOUT = 15.0f; // 15 second timeout for player actions
    private int lastPlayerIndex = -1; // Track when player changes to reset timer

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
            // Last player finished, transition to dealer turn with delay
            gameState = GameState.DEALER_TURN;
            dealerTurnDelay = DEALER_TURN_DELAY_TIME; // Start 1 second delay
            if(gameUI != null) gameUI.showPlayerActionPanel(false); // Check for null
            notifyStateChanged(); // NOTIFY
        } else {
            // Move to next player, still in PLAYER_TURN state
            current_playerIndex=(current_playerIndex+1)%playersList.size();
            // Reset timer for new player's turn
            playerActionTimer = 0f;
            lastPlayerIndex = current_playerIndex;
            if(gameUI != null) gameUI.updateCurrentPlayerColor(playersList.get(current_playerIndex)); // Check for null
            notifyStateChanged(); // NOTIFY - important for multiplayer to sync player change
        }
    }


    public void setGameUI(UI ui) {
        this.gameUI = ui;
    }

    public void stand() {
        // Don't reset timer here - nextPlayer() will handle it
        // This prevents double-reset and ensures proper synchronization
        nextPlayer();
    }

    public void playerAddToBet(int amount)
    {
        Player currentPlayer = playersList.get(current_playerIndex);
        currentPlayer.addToBet(amount);
        if (gameUI != null) {
            gameUI.updatePlayerBalance(currentPlayer); // Update UI to show new potential bet
        }
        // Reset timer when player takes action
        playerActionTimer = 0f;
    }

    public void playerLockInBet() {
        Player currentPlayer = playersList.get(current_playerIndex);
        if (currentPlayer.getCurrentBet() > 0) {
            currentPlayer.lockInBet();
            if(gameUI != null) gameUI.updatePlayerBalance(currentPlayer); // Check for null
            // Reset timer when player takes action
            playerActionTimer = 0f;

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

        // Don't start card return animation here - wait for delay in RESOLVING_BETS state
        // The state machine will handle the delay and animation

        // Stay in RESOLVING_BETS - the state machine will transition to FINISHING_ROUND
        // after the delay and animation
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
        // Reset timer when player takes action
        playerActionTimer = 0f;

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
                betsResolved = false; // Reset flag for new round
                resolvingBetsDelay = 0f; // Reset delay
                cardReturnAnimationDelay = 0f; // Reset animation delay
                dealerTurnDelay = 0f; // Reset dealer turn delay
                playerActionTimer = 0f; // Reset player action timer
                lastPlayerIndex = -1; // Reset last player index tracker
                gameState = GameState.BETTING;
                if(gameUI != null) {
                    gameUI.showBettingPanel(true);
                    gameUI.setCurrentPlayer(playersList.get(current_playerIndex));
                }
                notifyStateChanged(); // NOTIFY (Initial BETTING state)
                break;
            case BETTING:
                // Reset timer when a new player's turn starts
                if (lastPlayerIndex != current_playerIndex) {
                    playerActionTimer = 0f;
                    lastPlayerIndex = current_playerIndex;
                }

                // Update timer (only on server in multiplayer, or always in local games)
                playerActionTimer += delta;

                // Auto-action after 15 seconds (only on server where sequencer is null)
                if (playerActionTimer >= PLAYER_ACTION_TIMEOUT && sequencer == null) {
                    Player currentPlayer = playersList.get(current_playerIndex);
                    int balance = currentPlayer.getBalance();

                    // Find the largest bet amount the player can afford (100, 50, or 10)
                    int betAmount = 0;
                    if (balance >= 100) {
                        betAmount = 100;
                    } else if (balance >= 50) {
                        betAmount = 50;
                    } else if (balance >= 10) {
                        betAmount = 10;
                    }

                    // If player can afford a bet and hasn't bet yet, add bet and lock in
                    if (betAmount > 0 && currentPlayer.getCurrentBet() == 0) {
                        playerAddToBet(betAmount);
                        playerLockInBet();
                        Gdx.app.log("BlackjackLogic", "Auto-bet: Player " + currentPlayer.getName() + " bet " + betAmount + " after timeout");
                    } else if (currentPlayer.getCurrentBet() > 0) {
                        // Player already has a bet, just lock it in
                        playerLockInBet();
                        Gdx.app.log("BlackjackLogic", "Auto-lock: Player " + currentPlayer.getName() + " locked in bet after timeout");
                    }
                    // Reset timer
                    playerActionTimer = 0f;
                }
                break;
            case DEALING_DEALER:
                if(sequencer == null || !sequencer.isBusy()) // Check for null
                {
                    dealer.addCard(deck.deal());
                    if(sequencer != null) sequencer.createDealCardToDealerAction(dealer); // Check for null
                    if(gameUI != null) gameUI.updateDealerScore(dealer); // Check for null
                    gameState = GameState.ANIMATIONS_IN_PROGRESS;
                    notifyStateChanged(); // NOTIFY - server will wait for animation
                }
                break;
            case DEALING_PLAYERS: {
                if(gameUI != null) gameUI.showPlayerActionPanel(false); // Check for null

                // Wait for sequencer if busy (client-side only)
                if (sequencer != null && sequencer.isBusy()) {
                    if (current_playerIndex < playersList.size()) {
                        if(gameUI != null) gameUI.updatePlayerScore(playersList.get(current_playerIndex));
                    }
                    return; // Wait for animation to complete
                }

                // Check if we've completed dealing initial cards (all players have 2 cards)
                boolean allPlayersHaveTwoCards = true;
                for (Player p : playersList) {
                    if (p.m_currentCards.size() < 2) {
                        allPlayersHaveTwoCards = false;
                        break;
                    }
                }

                if (allPlayersHaveTwoCards) {
                    // All players have 2 cards, move to dealer turn
                    current_playerIndex = 0;
                    gameState = GameState.ANIMATIONS_IN_PROGRESS;
                    notifyStateChanged(); // NOTIFY
                    return;
                }

                // Reset index if we've gone through all players (start next round)
                if (current_playerIndex >= playersList.size()) {
                    current_playerIndex = 0;
                }

                // Deal card to current player if they need more cards
                Player curentPlayer = playersList.get(current_playerIndex);
                if (curentPlayer.m_currentCards.size() < 2) {
                    curentPlayer.addCard(deck.deal());
                    if(sequencer != null) sequencer.createDealCardAction(curentPlayer, current_playerIndex); // Check for null
                    current_playerIndex++;

                    // Transition to ANIMATIONS_IN_PROGRESS to wait for animation
                    gameState = GameState.ANIMATIONS_IN_PROGRESS;
                    notifyStateChanged(); // NOTIFY - server will wait for animation
                } else {
                    // This player already has 2 cards, move to next player
                    current_playerIndex++;
                }
                break;
            }
            case ANIMATIONS_IN_PROGRESS: {
                if (sequencer == null || !sequencer.isBusy()) { // Check for null
                    // Check if dealer is in the middle of their turn (has more than 1 card and value < 17)
                    // This indicates we were dealing dealer cards
                    boolean dealerInProgress = dealer.m_currentCards.size() > 1 && dealer.totalValue() < 17;

                    if (dealerInProgress) {
                        // Dealer still needs cards, go back to dealer turn
                        gameState = GameState.DEALER_TURN;
                        // Don't notify here - let the dealer turn state handle it
                    } else {
                        // Check if we're still dealing initial cards
                        boolean allPlayersHaveTwoCards = true;
                        for (Player p : playersList) {
                            if (p.m_currentCards.size() < 2) {
                                allPlayersHaveTwoCards = false;
                                break;
                            }
                        }

                        // Check if dealer has their first card
                        boolean dealerHasCard = dealer.m_currentCards.size() > 0;

                        // Check if dealer is done (has card and value >= 17, or was in dealer turn)
                        boolean dealerDone = dealer.m_currentCards.size() > 1 && dealer.totalValue() >= 17;

                        if (dealerDone) {
                            // Dealer is done, resolve bets
                            gameState = GameState.RESOLVING_BETS;
                            notifyStateChanged(); // NOTIFY
                        } else if (allPlayersHaveTwoCards && dealerHasCard) {
                            // All players have 2 cards and dealer has 1 card, move to player turn
                            current_playerIndex = 0;
                            gameState = GameState.PLAYER_TURN;
                            notifyStateChanged(); // NOTIFY
                        } else if (!allPlayersHaveTwoCards) {
                            // Still dealing cards to players, go back to dealing players
                            gameState = GameState.DEALING_PLAYERS;
                            // Don't notify here - let the dealing state handle it
                        } else if (!dealerHasCard) {
                            // Dealer doesn't have a card yet, go to dealing dealer
                            gameState = GameState.DEALING_DEALER;
                            // Don't notify here - let the dealing state handle it
                        }
                    }
                }
                break;
            }
            case PLAYER_TURN:
                if (gameUI != null && !gameUI.ActionPanelIsVisible()) // Check for null
                {
                    gameUI.showPlayerActionPanel(true);
                    gameUI.setCurrentPlayer(playersList.get(current_playerIndex));
                }

                // Reset timer when a new player's turn starts
                if (lastPlayerIndex != current_playerIndex) {
                    playerActionTimer = 0f;
                    lastPlayerIndex = current_playerIndex;
                    Gdx.app.log("BlackjackLogic", "Player turn started: " + playersList.get(current_playerIndex).getName() + " (index: " + current_playerIndex + ")");
                }

                // Update timer (only on server in multiplayer, or always in local games)
                playerActionTimer += delta;

                // Auto-stand after 15 seconds (only on server where sequencer is null, or if sequencer is not busy)
                if (playerActionTimer >= PLAYER_ACTION_TIMEOUT && (sequencer == null || !sequencer.isBusy())) {
                    String playerName = playersList.get(current_playerIndex).getName();
                    Gdx.app.log("BlackjackLogic", "Auto-stand: Player " + playerName + " stood after timeout (timer was: " + playerActionTimer + ")");
                    stand();
                    // Timer is reset in nextPlayer() method
                    // Don't break here - let the state machine continue processing
                }
                break;
            case DEALER_TURN:
                // Wait for delay before starting dealer turn
                if (dealerTurnDelay > 0) {
                    dealerTurnDelay -= delta;
                    if (dealerTurnDelay <= 0) {
                        dealerTurnDelay = 0f; // Delay complete
                    }
                    return; // Wait for delay
                }

                if(sequencer == null || !sequencer.isBusy()) // Check for null
                {
                    if(dealer.totalValue() < 17)
                    {
                        dealer.addCard(deck.deal());
                        if(sequencer != null) sequencer.createDealCardToDealerAction(dealer); // Check for null
                        if(gameUI != null) gameUI.updateDealerScore(dealer); // Check for null
                        // Transition to ANIMATIONS_IN_PROGRESS to wait for animation
                        gameState = GameState.ANIMATIONS_IN_PROGRESS;
                        notifyStateChanged(); // NOTIFY - server will wait for animation
                    }
                    else
                    {
                        gameState = GameState.RESOLVING_BETS;
                        notifyStateChanged(); // NOTIFY
                    }
                }
                break;
            case RESOLVING_BETS:
                // Resolve bets only once when entering this state
                if (!betsResolved) {
                    // Only resolve bets on server (where sequencer is null)
                    // Clients receive updated balances from server
                    if (sequencer == null) {
                        // Server: calculate and update balances
                        resolveBets();
                    } else {
                        // Client: balances already synced from server, just update UI
                        if (gameUI != null) {
                            gameUI.updateDealerScore(dealer);
                            gameUI.showPlayerActionPanel(false);
                            for (Player p : playersList) {
                                gameUI.updatePlayerScore(p);
                            }
                        }
                    }
                    betsResolved = true;
                    resolvingBetsDelay = RESOLVING_BETS_DELAY_TIME; // Start 2 second delay
                }

                // Wait for delay to complete before starting card return animation
                if (resolvingBetsDelay > 0) {
                    resolvingBetsDelay -= delta;
                    if (resolvingBetsDelay <= 0) {
                        // Delay complete, start card return animation
                        if (sequencer != null) {
                            // Client: start animation
                            sequencer.moveCardsToDeck(playersList, dealer);
                        } else {
                            // Server: start delay timer to wait for clients to complete animation
                            cardReturnAnimationDelay = CARD_RETURN_ANIMATION_TIME;
                        }
                    }
                    return; // Wait for delay
                }

                // Wait for animation to complete before transitioning
                if (sequencer != null) {
                    // Client: wait for sequencer to finish
                    if (!sequencer.isBusy()) {
                        gameState = GameState.FINISHING_ROUND;
                        betsResolved = false; // Reset flag for next round
                        resolvingBetsDelay = 0f; // Reset delay
                        cardReturnAnimationDelay = 0f; // Reset animation delay
                        notifyStateChanged();
                    }
                } else {
                    // Server: wait for animation delay to complete (gives clients time to animate)
                    if (cardReturnAnimationDelay > 0) {
                        cardReturnAnimationDelay -= delta;
                        return; // Wait for animation delay
                    }
                    // Animation delay complete, transition to FINISHING_ROUND
                    gameState = GameState.FINISHING_ROUND;
                    betsResolved = false; // Reset flag for next round
                    resolvingBetsDelay = 0f; // Reset delay
                    cardReturnAnimationDelay = 0f; // Reset animation delay
                    notifyStateChanged();
                }
                break;
            case FINISHING_ROUND:
                if (sequencer == null || !sequencer.isBusy()) { // Check for null
                    // Clear card entities from the ECS
                    if(sequencer != null) sequencer.clearCardEntities(); // Check for null

                    // Reset all cards and bets
                    dealer.reset();
                    for (Player p : playersList) {
                        p.reset();
                        if (p.getBalance() < 1) {
                            p.toggleActive();
                        }
                    }

                    // Remove inactive players immediately (not just in STARTING)
                    int playersBeforeRemoval = playersList.size();
                    int oldCurrentIndex = current_playerIndex;
                    playersList.removeIf(p -> !p.isActive());
                    int playersAfterRemoval = playersList.size();

                    // Adjust current player index if needed
                    if (playersAfterRemoval < playersBeforeRemoval) {
                        // Players were removed, adjust index
                        // If current index is out of bounds, reset to 0
                        if (current_playerIndex >= playersList.size()) {
                            current_playerIndex = 0;
                        }
                        // If players before the current player were removed, the index might need adjustment
                        // Actually, since we're removing from the list, indices shift automatically
                        // We just need to make sure we're not out of bounds
                        if (current_playerIndex >= playersList.size()) {
                            current_playerIndex = Math.max(0, playersList.size() - 1);
                        }
                    }

                    // Update UI scores to reflect cleared state
                    if (gameUI != null) {
                        gameUI.updateDealerScore(dealer);
                        for (Player p : playersList) {
                            gameUI.updatePlayerScore(p);
                        }
                        // Rebuild layout if players were removed
                        if (playersAfterRemoval < playersBeforeRemoval) {
                            gameUI.rebuildLayout(playersList);
                        }
                    }

                    // Move to STARTING state to begin new round
                    gameState = GameState.STARTING;
                    notifyStateChanged(); // NOTIFY
                }
                break;
            case GAME_OVER:
                if(gameUI != null) {
                    // Pass isHost flag - sequencer is null on server (host), not null on clients
                    boolean isHost = (sequencer == null);
                    gameUI.showGameOverMenu(isHost);
                }
                break;
        }
    }

    /**
     * Removes a player from the game by name.
     * Used for exit match functionality.
     */
    public void removePlayerByName(String playerName) {
        playersList.removeIf(p -> p.getName().equals(playerName));
        numPlayers = playersList.size();

        // Adjust current player index if needed
        if (current_playerIndex >= playersList.size()) {
            current_playerIndex = 0;
        }

        // If no players left, end game
        if (playersList.isEmpty()) {
            gameState = GameState.GAME_OVER;
            notifyStateChanged();
        }

        Gdx.app.log("BlackjackLogic", "Removed player: " + playerName + " (Remaining: " + playersList.size() + ")");
    }
}
