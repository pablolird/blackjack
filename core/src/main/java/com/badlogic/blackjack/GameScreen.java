package com.badlogic.blackjack;

import com.badlogic.blackjack.ecs.ECS;
import com.badlogic.blackjack.animation.Sequencer;
import com.badlogic.blackjack.assets.Assets;
import com.badlogic.blackjack.game.*;
import com.badlogic.blackjack.network.GameClient;
import com.badlogic.blackjack.network.GameServer;
import com.badlogic.blackjack.ui.UI;
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

// Implement the Screen interface from libgdx
public class GameScreen implements Screen, LobbyUpdateListener {
    private final Main game;
    private final FitViewport gameViewport;
    private final Viewport uiViewport;
    private final SpriteBatch spriteBatch;
    private final Assets assets;

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
    private boolean exitMatchInitiated = false;

    // Timer tracking for multiplayer clients
    private float clientTimer = 0f;
    private int lastClientPlayerIndex = -1;
    private GameState lastClientGameState = null;
    private GameState currentNetworkState = null;
    private int currentNetworkPlayerIndex = -1;

    public GameScreen(Main game, boolean isHost, List<String> playerNames) {
        this.game = game;
        this.assets = game.assets;
        this.spriteBatch = game.spriteBatch;

        this.isHost = isHost;
        this.playerNames = playerNames;
        this.gameServer = isHost ? game.gameServer : null;

        // All clients (host and non-host) need to be listeners for state updates
        if (game.gameClient != null) {
            game.gameClient.addLobbyUpdateListener(this);
            game.gameClient.setSessionMode(GameClient.SessionMode.MATCH);
        }

        // Use the constants from Main
        gameViewport = new FitViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        uiViewport = new FitViewport(960, 540);

        // Use layers
        audioManager = new AudioManager(assets);
        ecs = new ECS(assets);
        sequencer = new Sequencer(ecs, audioManager);

        // Determine if this is a local game
        boolean isLocalGame = (game.gameClient == null);
        logic = new BlackjackLogic(sequencer, playerNames, isLocalGame);
        ui = new UI(uiViewport, spriteBatch, logic, audioManager, game);

        logic.setGameUI(ui);
        ecs.createBoardEntity(Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        ecs.createDeckEntity();
        audioManager.playMusic(assets.bgMusic1, 0.25f);

        // Set up exit match callback
        game.exitMatchCallback = this::handleExitMatch;

        // Set up restart match callback
        game.restartMatchCallback = this::handleRestartMatch;
    }

    private void handleRestartMatch() {
        Gdx.app.log("GameScreen", "handleRestartMatch called - isLocal: " + (game.gameClient == null) + ", isHost: " + isHost);
        if (game.gameClient == null) {
            // Local game - restart with same number of players
            int numPlayers = logic.getPlayersList().size();
            Gdx.app.log("GameScreen", "Restarting local game with " + numPlayers + " players");
            game.setScreen(new GameScreen(game, numPlayers));
            dispose();
        } else if (isHost) {
            // Multiplayer host - send restart request to server
            Gdx.app.log("GameScreen", "Sending restart request to server");
            game.gameClient.sendRestartMatchRequest();
        }
        // Non-host multiplayer players wait for server response
    }

    private void handleExitMatch() {
        if (game.gameClient != null) {
            // Multiplayer game, notify all players
            exitMatchInitiated = true;
            game.gameClient.sendExitMatchRequest();
        } else {
            // Local game, just return to start screen
            game.setScreen(new StartScreen(game));
            dispose();
        }
    }

    public GameScreen(Main game, int numPlayers) {
        this(game, false, createLocalPlayerNames(numPlayers));
    }

    private static List<String> createLocalPlayerNames(int n) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            names.add("P" + (i+1));
        }
        return names;
    }

    @Override
    public void render(float delta) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            ui.togglePauseMenu();
        }

        if (game.gameClient == null) {
            // Local game - update logic directly
            logic.update(delta);

            if (logic.getGameState() == GameState.GAME_OVER && !ui.isGameOverMenuVisible()) {
                // For local games, always show host menu (restart/exit options)
                ui.showGameOverMenu(true);
                ui.showBettingPanel(false);
                ui.showPlayerActionPanel(false);
            }
        } else if (isHost && gameServer != null) {
            // Multiplayer host - server updates logic
            gameServer.update(delta);
        }

        sequencer.update(delta); // Sequencer runs on all clients for animations

        // Handle RESOLVING_BETS delay and animation on clients (multiplayer only)
        if (game.gameClient != null && resolvingBetsDelay > 0) {
            resolvingBetsDelay -= delta;
            if (resolvingBetsDelay <= 0 && !cardReturnAnimationStarted) {
                // Delay complete, start card return animation
                sequencer.moveCardsToDeck(logic.getPlayersList(), logic.getDealer());
                cardReturnAnimationStarted = true;
                Gdx.app.log("GameScreen", "Started card return animation");
            }
        }

        // Update timer for multiplayer clients (server handles its own timer in BlackjackLogic)
        if (game.gameClient != null && currentNetworkState != null) {
            // Use network state instead of logic state for multiplayer clients
            GameState currentState = currentNetworkState;
            int currentPlayerIndex = currentNetworkPlayerIndex;

            // Reset timer when player changes or state changes
            if (lastClientPlayerIndex != currentPlayerIndex || lastClientGameState != currentState) {
                clientTimer = 0f;
                lastClientPlayerIndex = currentPlayerIndex;
                lastClientGameState = currentState;

                // Show/hide timer based on state
                if (currentState == GameState.PLAYER_TURN) {
                    ui.resetTimer();
                    ui.showTimer();
                } else if (currentState == GameState.BETTING) {
                    ui.resetTimer();
                    ui.showTimer();
                } else {
                    ui.hideTimer();
                }
            }

            // Update timer in PLAYER_TURN and BETTING states
            if (currentState == GameState.PLAYER_TURN || currentState == GameState.BETTING) {
                clientTimer += delta;
                ui.updateTimer(delta);
            }
        }

        // Process pending animations one at a time when sequencer is not busy (multiplayer only)
        // For local games, animations are handled directly by the logic
        if (game.gameClient != null) {
            processPendingAnimations();
        }

        ecs.update(delta);
        ui.update(delta);

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
        // Stop music when exiting game screen unless we're restarting and handing off to a new screen
        if (!isRestarting) {
            audioManager.stopMusic();
        }

        // Remove listener to prevent duplicate handling of network events
        if (game.gameClient != null) {
            if (!isRestarting && game.gameClient.getSessionMode() == GameClient.SessionMode.MATCH && !exitMatchInitiated) {
                exitMatchInitiated = true;
                game.gameClient.sendExitMatchRequest();
            }
            game.gameClient.removeLobbyUpdateListener(this);
        }

        ui.dispose();
    }


    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void onLobbyUpdate(NetworkPacket.LobbyUpdate update) {
    }

    @Override
    public void onGameStart(NetworkPacket.StartGame start) {
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
        exitMatchInitiated = true;

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
    public void onExitLobby(NetworkPacket.ExitLobbyResponse response) {
    }

    @Override
    public void onLobbyFull(NetworkPacket.LobbyFullResponse response) {
    }

    @Override
    public void onGameStateUpdate(NetworkPacket.GameStateUpdate update) {
        // Only process network updates in multiplayer games
        if (game.gameClient == null) {
            return; // Local game - logic handles everything directly
        }

        Gdx.app.log("GameScreen", "Received state update: " + update.currentGameState + " | Current Player: " + update.currentPlayerName);

        // Update network state tracking for timer
        GameState newState = GameState.valueOf(update.currentGameState);
        int newPlayerIndex = update.currentPlayerIndex;

        // Reset timer if state or player changed
        if (currentNetworkState != newState || currentNetworkPlayerIndex != newPlayerIndex) {
            clientTimer = 0f;
            lastClientPlayerIndex = newPlayerIndex;
            lastClientGameState = newState;

            // Show/hide timer based on new state
            if (newState == GameState.PLAYER_TURN) {
                ui.resetTimer();
                ui.showTimer();
            } else if (newState == GameState.BETTING) {
                ui.resetTimer();
                ui.showTimer();
            } else {
                ui.hideTimer();
            }
        }

        currentNetworkState = newState;
        currentNetworkPlayerIndex = newPlayerIndex;

        // CLIENT SIDE IMPLEMENTATION

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

            // Rebuild UI layout to reflect player removal
            ui.rebuildLayout(logic.getPlayersList(), logic.dealer);

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
                // For local games, always show host menu (restart/exit options)
                boolean showHostMenu = isHost || (game.gameClient == null);
                ui.showGameOverMenu(showHostMenu);
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

        // 3. Highlight dealer or player depending on state
        boolean dealerFocusState =
            state == GameState.DEALING_DEALER ||
            state == GameState.DEALING_PLAYERS ||
            state == GameState.DEALER_TURN ||
            state == GameState.RESOLVING_BETS ||
            state == GameState.FINISHING_ROUND;

        if (dealerFocusState) {
            ui.focusDealerOnly();
        } else {
            ui.resetDealerFocus();
            if (update.currentPlayerIndex >= 0 && update.currentPlayerIndex < logic.getPlayersList().size()) {
                Player currentPlayer = logic.getPlayersList().get(update.currentPlayerIndex);
                ui.updateCurrentPlayerColor(currentPlayer);
            } else {
//            ui.updateCurrentPlayerColor(null); // No player is active, unhighlight all
            }
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
                    break;
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
                            Card newCard = new Card(serverCardInfo.id, serverCardInfo.rank, serverCardInfo.suit);
                            localPlayer.addCard(newCard);
                            pendingAnimations.add(new PendingAnimation(PendingAnimation.Type.PLAYER, serverCardInfo, playerIndex, serverPlayer.name));
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
                sequencer.createDealCardAction(targetPlayer, pending.playerIndex, pending.cardInfo.id, ui);
                ui.updatePlayerScore(targetPlayer);
            } else {
                Gdx.app.error("GameScreen", "Could not find player: " + pending.playerName);
            }
        }
    }
}
