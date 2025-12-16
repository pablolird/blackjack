package com.badlogic.blackjack.game;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.blackjack.animation.Sequencer;
import com.badlogic.blackjack.ui.UI;
import com.badlogic.gdx.Gdx;

public class BlackjackLogic {
    private final Sequencer sequencer;
    private final boolean isLocalGame; // True = local
    int current_playerIndex;
    int numPlayers;
    Deck deck;
    public Dealer dealer;
    public List<Player> playersList;
    GameState gameState;
    UI gameUI;
    private int nextPlayerId = 0;
    private boolean betsResolved = false;
    // Delays are used to give clients time to show animations before the next transition.
    private float resolvingBetsDelay = 0f;
    private static final float RESOLVING_BETS_DELAY_TIME = 2.0f; // 2-second delay to view results
    private float cardReturnAnimationDelay = 0f;
    private static final float CARD_RETURN_ANIMATION_TIME = 1.75f; // Time to wait for card return animation to complete (0.25s per player + dealer)
    private float dealerTurnDelay = 0f;
    private static final float DEALER_TURN_DELAY_TIME = 1.0f; // 1-second delay before dealer turn starts
    private float playerActionTimer = 0f;
    public static final float PLAYER_ACTION_TIMEOUT = 15.0f; // 15 second timeout for player actions
    private int lastPlayerIndex = -1; // Track when player changes to reset timer

    private GameStateListener stateListener;

    public boolean isLocal() {
        return isLocalGame;
    }

    public BlackjackLogic(Sequencer sequencer, List<String> playerNames) {
        this(sequencer, playerNames, false);
    }

    // MULTIPLAYER CONSTRUCTOR
    public BlackjackLogic(Sequencer sequencer, List<String> playerNames, boolean isLocalGame) {
        this.numPlayers = playerNames.size();
        this.sequencer = sequencer;
        this.isLocalGame = isLocalGame;
        this.deck = new Deck();
        this.dealer = new Dealer();
        gameState = GameState.STARTING;

        playersList = new ArrayList<>();

        // Use player names from network
        for (String name : playerNames) {
            playersList.add(createPlayer(name));
        }

        this.current_playerIndex = 0;
    }

    //  LOCAL GAME CONSTRUCTOR
    public BlackjackLogic(Sequencer sequencer, int n) {
        // Create dummy names for local game initialization
        List<String> dummyNames = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            dummyNames.add("PLAYER" + (i + 1));
        }
        this.numPlayers = n;
        this.sequencer = sequencer;
        this.isLocalGame = true;
        this.deck = new Deck();
        this.dealer = new Dealer();
        gameState = GameState.STARTING;

        playersList = new ArrayList<>();

        for (String name : dummyNames) {
            playersList.add(createPlayer(name));
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

    private boolean isDealerFocusState(GameState state) {
        if (state == null) {
            return false;
        }
        switch (state) {
            case DEALING_DEALER:
            case DEALING_PLAYERS:
            case DEALER_TURN:
            case RESOLVING_BETS:
            case FINISHING_ROUND:
                return true;
            default:
                return false;
        }
    }

    private void updateDealerFocusUI() {
        if (gameUI == null) {
            return;
        }
        if (isDealerFocusState(gameState)) {
            gameUI.focusDealerOnly();
        } else {
            gameUI.resetDealerFocus();
        }
    }

    private void notifyStateChanged() {
        updateDealerFocusUI();
        if (stateListener != null) {
            stateListener.onGameStateChanged();
        }
    }

    public void nextPlayer() {
        // Used after HIT/STAND to advance turn order
        if (current_playerIndex==playersList.size()-1) {
            // Last player finished, transition to dealer turn with delay
            gameState = GameState.DEALER_TURN;
            dealerTurnDelay = DEALER_TURN_DELAY_TIME; // Start 1-second delay
            if(gameUI != null) gameUI.showPlayerActionPanel(false);
            notifyStateChanged();
        } else {
            // Move to next player, still in PLAYER_TURN state
            current_playerIndex=(current_playerIndex+1)%playersList.size();
            // Reset timer for new player's turn
            playerActionTimer = 0f;
            if(gameUI != null) gameUI.resetTimer();
            lastPlayerIndex = current_playerIndex;
            if(gameUI != null) gameUI.updateCurrentPlayerColor(playersList.get(current_playerIndex));
            notifyStateChanged();
        }
    }


    public void setGameUI(UI ui) {
        this.gameUI = ui;
        updateDealerFocusUI();
    }

    public void stand() {
        nextPlayer();
    }

    public void playerAddToBet(int amount)
    {
        Player currentPlayer = playersList.get(current_playerIndex);
        currentPlayer.addToBet(amount);
        if (gameUI != null) {
            gameUI.updatePlayerBalance(currentPlayer);
        }
        // Reset timer when player takes action
        playerActionTimer = 0f;
        if (gameUI != null) gameUI.resetTimer();
    }

    public void playerLockInBet() {
        Player currentPlayer = playersList.get(current_playerIndex);
        if (currentPlayer.getCurrentBet() > 0) {
            currentPlayer.lockInBet();
            // Reset timer when player takes action
            playerActionTimer = 0f;
            if(gameUI != null){
                gameUI.updatePlayerBalance(currentPlayer);
                gameUI.resetTimer();
            }

            if (current_playerIndex < playersList.size() - 1)
            {
                Player nextPlayer = playersList.get(current_playerIndex + 1);
                if(gameUI != null) gameUI.updateCurrentPlayerColor(nextPlayer);
                current_playerIndex++;
                notifyStateChanged();
            }
            else
            {
                current_playerIndex = 0;
                gameState = GameState.DEALING_DEALER;
                if(gameUI != null) {
                    gameUI.showBettingPanel(false);
//                    gameUI.updateCurrentPlayerColor(null);
                }
                notifyStateChanged();
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
                    multiplier = 0.0;
                }
                else if (playerTotal == 21 && p.m_currentCards.size() == 2)
                {
                    // Player blackjack
                    multiplier = 2.5;
                }
                else if (p.hasFiveCardCharlie())
                {
                    // Five-card charlie auto win
                    multiplier = 2.0;
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

        notifyStateChanged();
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

        Card dealtCard = deck.deal();
        curentPlayer.addCard(dealtCard);
        sequencer.createDealCardAction(curentPlayer, current_playerIndex, dealtCard.m_id, gameUI);
        if (gameUI != null) {
            gameUI.updatePlayerScore(curentPlayer);
        }
    }

    public void hit() {
        if (!gameState.equals(GameState.PLAYER_TURN) || (sequencer != null && sequencer.isBusy())) {
            return;
        }

        Player p = playersList.get(current_playerIndex);
        Card dealtCard = deck.deal();
        p.addCard(dealtCard);
        if(sequencer != null) sequencer.createDealCardAction(p, current_playerIndex, dealtCard.m_id, gameUI);
        if(gameUI != null) gameUI.updatePlayerScore(p);

        if (checkForFiveCardCharlie(p)) {
            Gdx.app.log("BlackjackLogic", "Auto-standing " + p.getName() + " due to 5-card charlie");
            nextPlayer();
            return;
        }

        int total = p.totalValue();
        if (total > 21) {
            nextPlayer();
            return;
        }

        if (total == 21) {
            Gdx.app.log("BlackjackLogic", "Auto-standing " + p.getName() + " at 21");
            nextPlayer();
            return;
        }
    }

    public void update(float delta) {
        switch (gameState) {
            case STARTING:
                // Reset round state and rebuild table before entering betting
                deck.reset();
                playersList.removeIf(p -> !p.isActive());
                if(gameUI != null) gameUI.rebuildLayout(playersList, dealer);
                if (playersList.isEmpty()) {
                    gameState = GameState.GAME_OVER;
                    notifyStateChanged();
                    return;
                }

                // Reset player index just in case the last player was removed
                current_playerIndex = 0;
                betsResolved = false; // Reset flag for new round
                resolvingBetsDelay = 0f; // Reset delay
                cardReturnAnimationDelay = 0f; // Reset animation delay
                dealerTurnDelay = 0f; // Reset dealer turn delay
                playerActionTimer = 0f; // Reset player action timer
                if (gameUI != null) gameUI.resetTimer();
                lastPlayerIndex = -1; // Reset last player index tracker
                gameState = GameState.BETTING;
                if(gameUI != null) {
                    gameUI.showBettingPanel(true);
                    gameUI.setCurrentPlayer(playersList.get(current_playerIndex));
                }
                notifyStateChanged();
                break;
            case BETTING:
                // No betting timer in local game
                if (isLocalGame && gameUI != null) {
                    gameUI.hideTimer();
                }

                // Reset timer when a new player's turn starts
                if (lastPlayerIndex != current_playerIndex) {
                    playerActionTimer = 0f;

                    if (gameUI != null && !isLocalGame) {
                        gameUI.resetTimer();
                    }
                    lastPlayerIndex = current_playerIndex;
                }

                // Update timer
                if (!isLocalGame) {
                    playerActionTimer += delta;
                    if (gameUI != null) gameUI.updateTimer(delta);
                }

                // Auto-action after 15 seconds
                if (playerActionTimer >= PLAYER_ACTION_TIMEOUT && sequencer == null) {
                    if (gameUI != null) gameUI.hideTimer();
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
                    if (gameUI != null) gameUI.resetTimer();
                }
                break;
            case DEALING_DEALER:
                if(sequencer == null || !sequencer.isBusy())
                {
                    dealer.addCard(deck.deal());
                    if(sequencer != null) sequencer.createDealCardToDealerAction(dealer);
                    if(gameUI != null) gameUI.updateDealerScore(dealer);
                    gameState = GameState.ANIMATIONS_IN_PROGRESS;
                    notifyStateChanged();
                }
                break;
            case DEALING_PLAYERS: {
                // Hand out two opening cards to each active player
                if(gameUI != null) gameUI.showPlayerActionPanel(false);

                if (sequencer != null && sequencer.isBusy()) {
                    if (current_playerIndex < playersList.size()) {
                        if(gameUI != null) gameUI.updatePlayerScore(playersList.get(current_playerIndex));
                    }
                    return;
                }

                // Check if dealing initial cards is completed (all players have 2 cards)
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
                    notifyStateChanged();
                    return;
                }

                // Next round
                if (current_playerIndex >= playersList.size()) {
                    current_playerIndex = 0;
                }

                // Deal card to current player if they need more cards
                Player curentPlayer = playersList.get(current_playerIndex);
                if (curentPlayer.m_currentCards.size() < 2) {
                    Card dealtCard = deck.deal();
                    curentPlayer.addCard(dealtCard);
                    if(sequencer != null) sequencer.createDealCardAction(curentPlayer, current_playerIndex, dealtCard.m_id, gameUI); // Check for null
                    if(gameUI != null) gameUI.updatePlayerScore(curentPlayer);
                    current_playerIndex++;

                    gameState = GameState.ANIMATIONS_IN_PROGRESS;
                    notifyStateChanged();
                } else {
                    // Move to next player if current has 2 cards
                    current_playerIndex++;
                }
                break;
            }
            case ANIMATIONS_IN_PROGRESS: {
                if (sequencer == null || !sequencer.isBusy()) {
                    // Check if dealer is in the middle of their turn (has more than 1 card and value < 17)
                    boolean dealerInProgress = dealer.m_currentCards.size() > 1 && dealer.totalValue() < 17;

                    if (dealerInProgress)
                    {
                        // Dealer still needs cards, go back to dealer turn
                        gameState = GameState.DEALER_TURN;
                    } else {
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
                        } else if (!dealerHasCard) {
                            // Dealer doesn't have a card yet, go to dealing dealer
                            gameState = GameState.DEALING_DEALER;
                        }
                    }
                }
                break;
            }
            case PLAYER_TURN:
                // Keep players moving if their state is already resolved
                if (autoAdvanceIfCurrentPlayerResolved()) {
                    break;
                }

                if (gameUI != null && !gameUI.ActionPanelIsVisible())
                {
                    gameUI.showPlayerActionPanel(true);
                    gameUI.setCurrentPlayer(playersList.get(current_playerIndex));
                }

                // Reset timer when a new player's turn starts
                if (lastPlayerIndex != current_playerIndex) {
                    playerActionTimer = 0f;
                    if (gameUI != null) {
                        gameUI.resetTimer();
                        gameUI.showTimer(); // Show timer when entering player turn
                    }
                    lastPlayerIndex = current_playerIndex;
                    Gdx.app.log("BlackjackLogic", "Player turn started: " + playersList.get(current_playerIndex).getName() + " (index: " + current_playerIndex + ")");
                }

                // Update timer
                playerActionTimer += delta;
                if (gameUI != null) gameUI.updateTimer(delta);


                // Auto-stand after 15 seconds
                if (playerActionTimer >= PLAYER_ACTION_TIMEOUT && (sequencer == null || !sequencer.isBusy())) {
                    if (gameUI != null) gameUI.hideTimer();
                    String playerName = playersList.get(current_playerIndex).getName();
                    Gdx.app.log("BlackjackLogic", "Auto-stand: Player " + playerName + " stood after timeout (timer was: " + playerActionTimer + ")");
                    stand();
                }
                break;
            case DEALER_TURN:
                // Wait for delay before starting dealer turn
                if (dealerTurnDelay > 0) {
                    dealerTurnDelay -= delta;
                    if (dealerTurnDelay <= 0) {
                        dealerTurnDelay = 0f;
                    }
                    return;
                }

                if(sequencer == null || !sequencer.isBusy())
                {
                    if(dealer.totalValue() < 17)
                    {
                        dealer.addCard(deck.deal());
                        if(sequencer != null) sequencer.createDealCardToDealerAction(dealer);
                        if(gameUI != null) gameUI.updateDealerScore(dealer);
                        gameState = GameState.ANIMATIONS_IN_PROGRESS;
                        notifyStateChanged();
                    }
                    else
                    {
                        gameState = GameState.RESOLVING_BETS;
                        notifyStateChanged();
                    }
                }
                break;
            case RESOLVING_BETS:
                // Resolve bets only once when entering this state
                if (!betsResolved) {
                    // Clients in multiplayer receive updated balances from server
                    if (sequencer == null || isLocalGame) {
                        resolveBets();
                    } else {
                        if (gameUI != null) {
                            gameUI.updateDealerScore(dealer);
                            gameUI.showPlayerActionPanel(false);
                            for (Player p : playersList) {
                                gameUI.updatePlayerScore(p);
                            }
                        }
                    }
                    betsResolved = true;
                    resolvingBetsDelay = RESOLVING_BETS_DELAY_TIME; // Start 2-second delay
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
                    return;
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
                        return;
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
                if (sequencer == null || !sequencer.isBusy()) {
                    // Clear card entities from the ECS
                    if(sequencer != null) sequencer.clearCardEntities();

                    // Reset all cards and bets
                    dealer.reset();
                    for (Player p : playersList) {
                        p.reset();
                        if (p.getBalance() < 1) {
                            p.toggleActive();
                        }
                    }

                    // Remove inactive players
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
                            gameUI.rebuildLayout(playersList, dealer);
                        }
                    }

                    // Move to STARTING state to begin new round
                    gameState = GameState.STARTING;
                    notifyStateChanged(); // NOTIFY
                }
                break;
            case GAME_OVER:
                if(gameUI != null) {
                    boolean isHost = (sequencer == null) || isLocalGame;
                    gameUI.showGameOverMenu(isHost);
                }
                break;
        }
    }

    // Remove player
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

    private Player createPlayer(String name) {
        return new Player(name, nextPlayerId++, 100);
    }

    private boolean checkForFiveCardCharlie(Player player) {
        if (player.hasFiveCardCharlie()) {
            return true;
        }

        if (player.m_currentCards.size() >= 5 && player.totalValue() < 21) {
            player.setFiveCardCharlie(true);
            return true;
        }
        return false;
    }

    private boolean autoAdvanceIfCurrentPlayerResolved() {
        if (playersList.isEmpty() || current_playerIndex >= playersList.size()) {
            return false;
        }

        Player currentPlayer = playersList.get(current_playerIndex);
        if (checkForFiveCardCharlie(currentPlayer) || currentPlayer.totalValue() == 21) {
            Gdx.app.log("BlackjackLogic", "Skipping turn for " + currentPlayer.getName() + " (auto-resolved)");
            nextPlayer();
            return true;
        }
        return false;
    }
}
