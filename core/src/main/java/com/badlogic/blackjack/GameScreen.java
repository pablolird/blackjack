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

    // Queue system for sequential animations in multiplayer
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

        if (!ui.isPaused()) {
            logic.update(delta);
            sequencer.update(delta); // Sequencer runs on all clients
            
            // Process pending animations one at a time when sequencer is not busy
            processPendingAnimations();
        }

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
    public void onGameStateUpdate(NetworkPacket.GameStateUpdate update) {
        Gdx.app.log("GameScreen", "Received state update: " + update.currentGameState + " | Current Player: " + update.currentPlayerName);

        // Flag to prevent queuing more than ONE animation per network packet.
        boolean animationQueuedThisPacket = false;

        // --- 1. CRITICAL: Check and Queue Dealer Card Animation (Priority 1) ---
        Dealer localDealer = logic.getDealer();
        for (NetworkPacket.CardInfo serverCardInfo : update.dealerCards) {
            if (!localDealer.hasCard(serverCardInfo.id)) {
                // This is the new card! Add it to local data and QUEUE animation
                Card newCard = new Card(serverCardInfo.id, serverCardInfo.rank, serverCardInfo.suit);
                localDealer.addCard(newCard);
                pendingAnimations.add(new PendingAnimation(PendingAnimation.Type.DEALER, serverCardInfo, -1, null));
                animationQueuedThisPacket = true;
                ui.updateDealerScore(localDealer);
                break; // *** STOP HERE! Only one animation per packet. ***
            }
        }

        // --- 2. Synchronize Player Data and Queue Animations (Priority 2) ---
        int playerIndex = 0;
        for (NetworkPacket.PlayerInfo serverPlayer : update.players) {
            Player localPlayer = null;
            for (Player p : logic.getPlayersList()) {
                if (p.getName().equals(serverPlayer.name)) {
                    localPlayer = p;
                    break;
                }
            }

            if (localPlayer != null) {
                // Force the local player's state (Always sync money/balance)
                localPlayer.setBalance(serverPlayer.balance);
                localPlayer.setCurrentBet(serverPlayer.currentBet);
                ui.updatePlayerBalance(localPlayer);

                if (!animationQueuedThisPacket) { // Only check if no Dealer or previous Player animation was queued
                    for (NetworkPacket.CardInfo serverCardInfo : serverPlayer.cards) {
                        if (!localPlayer.hasCard(serverCardInfo.id)) {
                            // This is the new card! Add it to local data AND QUEUE animation
                            Card newCard = new Card(serverCardInfo.id, serverCardInfo.rank, serverCardInfo.suit);
                            localPlayer.addCard(newCard);
                            pendingAnimations.add(new PendingAnimation(PendingAnimation.Type.PLAYER, serverCardInfo, playerIndex, serverPlayer.name));
                            animationQueuedThisPacket = true;
                            ui.updatePlayerScore(localPlayer);
                            break; // *** STOP HERE! Only one animation per packet. ***
                        }
                    }
                }

                // Always update the UI score
                ui.updatePlayerScore(localPlayer);
            }
            playerIndex++;
        }

        // If no animation was queued, ensure the dealer score is updated
        // (This happens automatically in the loops if a new card was found, but is safe here)
        if (!animationQueuedThisPacket) {
            ui.updateDealerScore(localDealer);
        }


        // 3. Update the UI panels based on the game state
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
            default:
                // Hide both panels during animations, dealing, etc.
                ui.showBettingPanel(false);
                ui.showPlayerActionPanel(false);
                break;
        }

        // 4. Highlight the current player
        if (update.currentPlayerIndex >= 0 && update.currentPlayerIndex < logic.getPlayersList().size()) {
            Player currentPlayer = logic.getPlayersList().get(update.currentPlayerIndex);
            ui.updateCurrentPlayerColor(currentPlayer);
        } else {
            ui.updateCurrentPlayerColor(null); // No player is active, unhighlight all
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
