package com.badlogic.blackjack;

import com.badlogic.blackjack.network.GameServer;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.blackjack.audio.AudioManager;
import com.badlogic.blackjack.network.GameClient.LobbyUpdateListener;
import com.badlogic.blackjack.network.NetworkPacket;
import com.badlogic.blackjack.network.NetworkPacket.PlayerInfo;
import com.badlogic.blackjack.network.NetworkPacket.CardInfo;

import java.util.ArrayList;
import java.util.List;

// Implement the Screen interface
public class GameScreen implements Screen, LobbyUpdateListener {
    private final Main game; // Reference to the Main game class
    private final FitViewport gameViewport;
    private final Viewport uiViewport;
    private final SpriteBatch spriteBatch; // Will be the shared batch
    private final Assets assets; // Will be the shared assets

    private final ECS ecs;
    private final Sequencer sequencer;
    private final BlackjackLogic logic;
    private final UI ui;
    private final AudioManager audioManager;

    private final boolean isHost;
    private final List<String> playerNames;
    private final GameServer gameServer;

    // Animation queue system for sequential animations in multiplayer
    private static class PendingAnimation {
        enum Type { DEALER, PLAYER }
        Type type;
        CardInfo cardInfo;
        int playerIndex;
        String playerName;

        PendingAnimation(Type type, CardInfo cardInfo, int playerIndex, String playerName) {
            this.type = type;
            this.cardInfo = cardInfo;
            this.playerIndex = playerIndex;
            this.playerName = playerName;
        }
    }
    private final List<PendingAnimation> pendingAnimations = new ArrayList<>();
    private boolean cardReturnAnimationStarted = false;
    private float resolvingBetsDelay = 0f;
    private static final float RESOLVING_BETS_DELAY_TIME = 2.0f; // 2 second delay to view results
    private boolean isRestarting = false; // Flag to prevent multiple restarts

    public GameScreen(Main game, boolean isHost, List<String> playerNames) {
        this.game = game;
        this.assets = game.assets; // Get shared assets
        this.spriteBatch = game.spriteBatch; // Get shared sprite batch

        this.isHost = isHost;
        this.playerNames = playerNames;
        this.gameServer = isHost ? game.gameServer : null;

        // All clients (host and non-host) need to be listeners for state updates
        if (game.gameClient != null) {
            game.gameClient.addLobbyUpdateListener(this);
        }

        // Use the constants from Main
        gameViewport = new FitViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        uiViewport = new FitViewport(960, 540);

        // Assets are now loaded in Main.java
        audioManager = new AudioManager(assets);
        ecs = new ECS(assets);
        sequencer = new Sequencer(ecs, audioManager);


        logic = new BlackjackLogic(sequencer, playerNames);
        ui = new UI(uiViewport, spriteBatch, logic, audioManager, game);

        logic.setGameUI(ui);
        // Use constants from Main
        ecs.createBoardEntity(Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        audioManager.playMusic(assets.bgMusic1, 0f);
        
        // Set up exit match callback
        game.exitMatchCallback = this::handleExitMatch;
        
        // Set up restart match callback
        game.restartMatchCallback = this::handleRestartMatch;
    }
    
    private void handleRestartMatch() {
        if (isHost && game.gameClient != null) {
            // Host sends restart request to server
            game.gameClient.sendRestartMatchRequest();
        }
    }
    
    private void handleExitMatch() {
        if (game.gameClient != null) {
            // Send exit match request to server
            game.gameClient.sendExitMatchRequest();
            
            // Disconnect the client after sending the request
            // The server will handle closing the connection
            // We'll disconnect here to ensure clean exit
            game.gameClient.dispose();
            game.gameClient = null;
        }
        
        // If host, stop the server
        if (isHost && game.gameServer != null) {
            game.gameServer.dispose();
            game.gameServer = null;
        }
        
        // Return to start screen
        game.setScreen(new StartScreen(game));
        dispose();
    }

    // --- Overload for existing local game calls, passing dummy values ---
    public GameScreen(Main game, int numPlayers) {
        this(game, false, createDummyPlayerNames(numPlayers));
    }

    private static List<String> createDummyPlayerNames(int n) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            names.add("Player " + (i+1));
        }
        return names;
    }

    @Override
    public void render(float delta) {
        // --- Update Logic ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            ui.togglePauseMenu();
        }

        // Game logic continues even when pause menu is open (multiplayer)
        // If host, update server logic (handles card dealing and state management)
        if (isHost && gameServer != null) {
            gameServer.update(delta);
        }
        
        sequencer.update(delta); // Sequencer runs on all clients for smooth animations
        
        // Handle RESOLVING_BETS delay and animation on clients
        if (resolvingBetsDelay > 0) {
            resolvingBetsDelay -= delta;
            if (resolvingBetsDelay <= 0 && !cardReturnAnimationStarted) {
                // Delay complete, start card return animation
                sequencer.moveCardsToDeck(logic.getPlayersList(), logic.getDealer());
                cardReturnAnimationStarted = true;
                Gdx.app.log("GameScreen", "Started card return animation");
            }
        }
        
        // Process pending animations one at a time when sequencer is not busy
        processPendingAnimations();

        ecs.update(delta);
        ui.update(delta);

        // --- Render Logic ---
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        gameViewport.apply();
        spriteBatch.setProjectionMatrix(gameViewport.getCamera().combined);

        spriteBatch.begin();
        ecs.render(spriteBatch);
        spriteBatch.end();
        ui.render();
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        ui.resize(width, height);
    }

    @Override
    public void dispose() {
        // IMPORTANT: Do NOT dispose of shared assets or SpriteBatch here
        // Only dispose of things created *by this screen*
        
        // Remove listener to prevent duplicate handling of network events
        if (game.gameClient != null) {
            game.gameClient.removeLobbyUpdateListener(this);
        }
        
        ui.dispose();
    }

    // Other required Screen methods
    @Override
    public void show() {
        // Called when this screen becomes active
    }

    @Override
    public void hide() {
        // Called when this screen is no longer active
    }

    @Override
    public void pause() {
        // For handling game pause
    }

    @Override
    public void resume() {
        // For handling game resume
    }

    @Override
    public void onLobbyUpdate(NetworkPacket.LobbyUpdate update) {
        // Not used in GameScreen
    }

    @Override
    public void onGameStart(NetworkPacket.StartGame start) {
        // Not used in GameScreen
    }
    
    @Override
    public void onRestartMatch(NetworkPacket.RestartMatchResponse response) {
        if (isRestarting) {
            Gdx.app.log("GameScreen", "Already restarting, ignoring duplicate restart match response");
            return;
        }
        
        isRestarting = true;
        Gdx.app.log("GameScreen", "Received restart match response with players: " + response.playerNames);
        
        // Remove listener before disposing to prevent duplicate handling
        if (game.gameClient != null) {
            game.gameClient.removeLobbyUpdateListener(this);
        }
        
        // Restart the game with the same players
        game.setScreen(new GameScreen(game, isHost, response.playerNames));
        dispose();
    }
    
    @Override
    public void onExitMatch(NetworkPacket.ExitMatchResponse response) {
        Gdx.app.log("GameScreen", "Received exit match response: hostExited=" + response.hostExited + ", player=" + response.exitedPlayerName);
        
        // Disconnect client if still connected
        if (game.gameClient != null) {
            game.gameClient.dispose();
            game.gameClient = null;
        }
        
        // If host, stop the server
        if (isHost && game.gameServer != null) {
            game.gameServer.dispose();
            game.gameServer = null;
        }
        
        // Return to start screen
        game.setScreen(new StartScreen(game));
        dispose();
    }

    @Override
    public void onGameStateUpdate(NetworkPacket.GameStateUpdate update) {
        Gdx.app.log("GameScreen", "Received state update: " + update.currentGameState + " | Current Player: " + update.currentPlayerName);

        // --- THIS IS THE ENTIRE CLIENT-SIDE IMPLEMENTATION ---

        // 1. Update all local player data (balances, bets, active status) from the packet
        List<String> serverPlayerNames = new ArrayList<>();
        for (PlayerInfo serverPlayer : update.players) {
            serverPlayerNames.add(serverPlayer.name);
            
            // Find the matching local player
            Player localPlayer = null;
            for (Player p : logic.getPlayersList()) {
                if (p.getName().equals(serverPlayer.name)) {
                    localPlayer = p;
                    break;
                }
            }

            if (localPlayer != null) {
                // Force the local player's state to match the server's
                localPlayer.setBalance(serverPlayer.balance);
                localPlayer.setCurrentBet(serverPlayer.currentBet);

                // Update the UI with this new data
                ui.updatePlayerBalance(localPlayer);
            }
        }
        
        // Remove players that are no longer on the server (they were removed due to zero balance)
        boolean playersRemoved = false;
        List<Player> playersToRemove = new ArrayList<>();
        for (Player localPlayer : logic.getPlayersList()) {
            // Check if player is not in server list (server only sends active players)
            boolean foundOnServer = serverPlayerNames.contains(localPlayer.getName());
            if (!foundOnServer) {
                playersToRemove.add(localPlayer);
                playersRemoved = true;
            }
        }
        
        // Remove players that are no longer on the server
        if (playersRemoved) {
            int playersBeforeRemoval = logic.getPlayersList().size();
            logic.getPlayersList().removeAll(playersToRemove);
            int playersAfterRemoval = logic.getPlayersList().size();
            
            // Adjust current player index if needed
            int currentIndex = logic.getCurrentPlayerIndex();
            if (currentIndex >= playersAfterRemoval) {
                // Current player was removed or index is out of bounds, reset to 0
                // Note: We can't directly set the index, but the server will handle this
            }
            
            // Rebuild UI layout to reflect player removal
            ui.rebuildLayout(logic.getPlayersList());
            
            Gdx.app.log("GameScreen", "Removed " + (playersBeforeRemoval - playersAfterRemoval) + " player(s) with zero balance");
        }

        // 2. Update the UI panels based on the game state
        GameState state = GameState.valueOf(update.currentGameState);
        switch (state) {
            case BETTING:
                ui.showBettingPanel(true);
                ui.showPlayerActionPanel(false);
                break;
            case PLAYER_TURN:
                ui.showBettingPanel(false);
                ui.showPlayerActionPanel(true);
                break;
            case GAME_OVER:
                // Show game over menu on clients when GAME_OVER state is received
                ui.showGameOverMenu(isHost);
                ui.showBettingPanel(false);
                ui.showPlayerActionPanel(false);
                break;
            default:
                // Hide both panels during animations, dealing, etc.
                ui.showBettingPanel(false);
                ui.showPlayerActionPanel(false);
                break;
        }

        // 2.5. Handle STARTING state - ensure everything is cleared for new round
        if (state == GameState.STARTING || state == GameState.BETTING) {
            // Always clear cards and entities when starting a new round
            // Server should have empty card lists at this point
            Dealer localDealer = logic.getDealer();
            
            // Force clear everything when starting new round
            if (!localDealer.m_currentCards.isEmpty() || 
                logic.getPlayersList().stream().anyMatch(p -> !p.m_currentCards.isEmpty())) {
                // Clear all cards and entities
                localDealer.reset();
                for (Player p : logic.getPlayersList()) {
                    p.reset();
                }
                   sequencer.clearCardEntities();
                   // Update UI scores to reflect cleared state
                   ui.updateDealerScore(localDealer);
                   for (Player p : logic.getPlayersList()) {
                       ui.updatePlayerScore(p);
                   }
                   cardReturnAnimationStarted = false;
                   resolvingBetsDelay = 0f;
               }
           }

        // 3. Highlight the current player
        if (update.currentPlayerIndex >= 0 && update.currentPlayerIndex < logic.getPlayersList().size()) {
            Player currentPlayer = logic.getPlayersList().get(update.currentPlayerIndex);
            ui.updateCurrentPlayerColor(currentPlayer);
        } else {
            ui.updateCurrentPlayerColor(null); // No player is active, unhighlight all
        }

        // 4. Handle RESOLVING_BETS state - start delay and animation on clients
        if (state == GameState.RESOLVING_BETS) {
            // Start delay timer if not already started
            if (resolvingBetsDelay <= 0 && !cardReturnAnimationStarted) {
                resolvingBetsDelay = RESOLVING_BETS_DELAY_TIME;
                Gdx.app.log("GameScreen", "Started RESOLVING_BETS delay timer");
            }
        } else {
            // Reset delay and flag when not in RESOLVING_BETS
            resolvingBetsDelay = 0f;
            if (state != GameState.FINISHING_ROUND) {
                cardReturnAnimationStarted = false;
            }
        }

        // 5. Handle FINISHING_ROUND state - clear cards when server indicates they're cleared
        if (state == GameState.FINISHING_ROUND) {
            // Wait for animation to complete, then clear cards
            // Make sure card return animation has finished before clearing
            if (!sequencer.isBusy() && cardReturnAnimationStarted) {
                // Check if server has cleared cards (empty card lists)
                if (update.dealerCards.isEmpty() && update.players.stream().allMatch(p -> p.cards.isEmpty())) {
                    // Server has cleared cards, clear local state
                    logic.getDealer().reset();
                    for (Player p : logic.getPlayersList()) {
                        p.reset();
                    }
                    sequencer.clearCardEntities();
                    // Update UI scores to reflect cleared state
                    ui.updateDealerScore(logic.getDealer());
                    for (Player p : logic.getPlayersList()) {
                        ui.updatePlayerScore(p);
                    }
                    // Reset flag for next round
                    cardReturnAnimationStarted = false;
                } else {
                    // Server still has cards, but animation is done - force sync removal
                    Dealer localDealer = logic.getDealer();
                    
                    // Remove dealer cards that are no longer on server
                    localDealer.m_currentCards.removeIf(card -> {
                        boolean found = update.dealerCards.stream().anyMatch(ci -> ci.id == card.m_id);
                        return !found;
                    });
                    
                    // Remove player cards that are no longer on server
                    for (PlayerInfo serverPlayer : update.players) {
                        Player localPlayer = null;
                        for (Player p : logic.getPlayersList()) {
                            if (p.getName().equals(serverPlayer.name)) {
                                localPlayer = p;
                                break;
                            }
                        }
                        
                        if (localPlayer != null) {
                            localPlayer.m_currentCards.removeIf(card -> {
                                boolean found = serverPlayer.cards.stream().anyMatch(ci -> ci.id == card.m_id);
                                return !found;
                            });
                        }
                    }
                }
            }
        }
        
        // Reset flag when starting a new round
        if (state == GameState.STARTING || state == GameState.BETTING) {
            cardReturnAnimationStarted = false;
        }

        // 6. Synchronize Cards and Queue Animations (only if not in FINISHING_ROUND)
        if (state != GameState.FINISHING_ROUND && state != GameState.RESOLVING_BETS) {
            // Check for new dealer cards
            Dealer localDealer = logic.getDealer();
            for (CardInfo serverCardInfo : update.dealerCards) {
                if (!localDealer.hasCard(serverCardInfo.id)) {
                    // New dealer card detected - add to local data and queue animation
                    Card newCard = new Card(serverCardInfo.id, serverCardInfo.rank, serverCardInfo.suit);
                    localDealer.addCard(newCard);
                    pendingAnimations.add(new PendingAnimation(PendingAnimation.Type.DEALER, serverCardInfo, -1, null));
                    ui.updateDealerScore(localDealer);
                    break; // Only one new card per update
                }
            }

            // Check for new player cards
            int playerIndex = 0;
            for (PlayerInfo serverPlayer : update.players) {
                Player localPlayer = null;
                for (Player p : logic.getPlayersList()) {
                    if (p.getName().equals(serverPlayer.name)) {
                        localPlayer = p;
                        break;
                    }
                }

                if (localPlayer != null) {
                    // Check for new cards for this player
                    for (CardInfo serverCardInfo : serverPlayer.cards) {
                        if (!localPlayer.hasCard(serverCardInfo.id)) {
                            // New player card detected - add to local data and queue animation
                            Card newCard = new Card(serverCardInfo.id, serverCardInfo.rank, serverCardInfo.suit);
                            localPlayer.addCard(newCard);
                            pendingAnimations.add(new PendingAnimation(PendingAnimation.Type.PLAYER, serverCardInfo, playerIndex, serverPlayer.name));
                            ui.updatePlayerScore(localPlayer);
                            break; // Only one new card per player per update
                        }
                    }
                }
                playerIndex++;
            }
        } else {
            // In FINISHING_ROUND or RESOLVING_BETS, synchronize card removal
            Dealer localDealer = logic.getDealer();
            
            // Remove dealer cards that are no longer on server
            localDealer.m_currentCards.removeIf(card -> {
                boolean found = update.dealerCards.stream().anyMatch(ci -> ci.id == card.m_id);
                return !found;
            });
            
            // Remove player cards that are no longer on server
            for (PlayerInfo serverPlayer : update.players) {
                Player localPlayer = null;
                for (Player p : logic.getPlayersList()) {
                    if (p.getName().equals(serverPlayer.name)) {
                        localPlayer = p;
                        break;
                    }
                }
                
                if (localPlayer != null) {
                    localPlayer.m_currentCards.removeIf(card -> {
                        boolean found = serverPlayer.cards.stream().anyMatch(ci -> ci.id == card.m_id);
                        return !found;
                    });
                }
            }
        }
    }

    /**
     * Processes pending animations one at a time when the sequencer is not busy.
     * This ensures animations happen sequentially across all clients in multiplayer.
     */
    private void processPendingAnimations() {
        // Only process if sequencer is not busy and there are pending animations
        if (sequencer.isBusy() || pendingAnimations.isEmpty()) {
            return;
        }

        // Process the first pending animation
        PendingAnimation pending = pendingAnimations.remove(0);

        if (pending.type == PendingAnimation.Type.DEALER) {
            Dealer dealer = logic.getDealer();
            sequencer.createDealCardToDealerAction(dealer);
            ui.updateDealerScore(dealer);
        } else if (pending.type == PendingAnimation.Type.PLAYER) {
            // Find the player by name
            Player targetPlayer = null;
            for (Player p : logic.getPlayersList()) {
                if (p.getName().equals(pending.playerName)) {
                    targetPlayer = p;
                    break;
                }
            }

            if (targetPlayer != null) {
                sequencer.createDealCardAction(targetPlayer, pending.playerIndex);
                ui.updatePlayerScore(targetPlayer);
            } else {
                Gdx.app.error("GameScreen", "Could not find player: " + pending.playerName);
            }
        }
    }
}
