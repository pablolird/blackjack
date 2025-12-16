package com.badlogic.blackjack.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.badlogic.gdx.Gdx;
import com.badlogic.blackjack.game.BlackjackLogic;
import com.badlogic.blackjack.game.GameStateListener;
import com.badlogic.blackjack.game.GameState;
import com.badlogic.blackjack.game.Player;
import com.badlogic.blackjack.game.Card;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static com.badlogic.blackjack.Main.TCP_PORT;
import static com.badlogic.blackjack.Main.UDP_PORT;

// Game server, handles server-side network communication
public class GameServer implements GameStateListener {
    // Server instance
    private final Server server;
    private final String roomName;
    private final int maxPlayers;
    // Map connection ID to Player Name
    private final Map<Integer, String> connectedPlayers = new HashMap<>();
    private final Map<String, Integer> playerNameToIndex = new HashMap<>();
    private String hostPlayerName; // Track the host player
    private BlackjackLogic gameLogic;
    private float animationTimer = 0f;
    private static final float ANIMATION_DELAY = 0.4f; // Time to wait for animations
    private final List<String> queuedPlayerRemovals = new ArrayList<>(); // Players to remove at safe phase

    public GameServer(String roomName, int maxPlayers) {
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        server = new Server();
        registerPackets();
        addListeners();
    }

    // Register packets
    private void registerPackets() {

        server.getKryo().register(ArrayList.class);

        // Register nested packet classes before the ones that use them
        server.getKryo().register(NetworkPacket.CardInfo.class);
        server.getKryo().register(NetworkPacket.PlayerInfo.class);

        // Register packet classes
        server.getKryo().register(NetworkPacket.RegisterPlayer.class);
        server.getKryo().register(NetworkPacket.LobbyUpdate.class);
        server.getKryo().register(NetworkPacket.StartGame.class);
        server.getKryo().register(NetworkPacket.PlayerActionType.class);
        server.getKryo().register(NetworkPacket.PlayerAction.class);
        server.getKryo().register(NetworkPacket.ExitMatchRequest.class);
        server.getKryo().register(NetworkPacket.ExitMatchResponse.class);
        server.getKryo().register(NetworkPacket.RestartMatchRequest.class);
        server.getKryo().register(NetworkPacket.RestartMatchResponse.class);
        server.getKryo().register(NetworkPacket.ExitLobbyRequest.class);
        server.getKryo().register(NetworkPacket.ExitLobbyResponse.class);
        server.getKryo().register(NetworkPacket.LobbyFullResponse.class);
        server.getKryo().register(NetworkPacket.GameStateUpdate.class);
    }

    // Add listeners
    private void addListeners() {
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Gdx.app.log("GameServer", "Client connected: " + connection.getID());
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof NetworkPacket.RegisterPlayer) {
                    NetworkPacket.RegisterPlayer packet = (NetworkPacket.RegisterPlayer) object;
                    // Check if lobby is full
                    if (connectedPlayers.size() < maxPlayers) {
                        // Ensure names are unique on the server to avoid collisions
                        String finalName = ensureUniqueName(packet.name);

                        connectedPlayers.put(connection.getID(), finalName);
                        // First player to connect is the host
                        if (hostPlayerName == null) {
                            hostPlayerName = finalName;
                            Gdx.app.log("GameServer", "Host player set: " + hostPlayerName);
                        }
                        Gdx.app.log("GameServer", "Registered player: " + finalName + " (Total: " + connectedPlayers.size() + ")");
                        sendLobbyUpdate();
                    } else {
                        // Lobby is full, notify the client before closing
                        Gdx.app.log("GameServer", "Lobby full. Notifying and rejecting connection: " + packet.name);
                        NetworkPacket.LobbyFullResponse response = new NetworkPacket.LobbyFullResponse();
                        response.message = "Lobby is full";
                        connection.sendTCP(response);
                        connection.close();
                    }
                } else if (object instanceof NetworkPacket.ExitMatchRequest) {
                    NetworkPacket.ExitMatchRequest request = (NetworkPacket.ExitMatchRequest) object;
                    String playerName = connectedPlayers.get(connection.getID());
                    if (playerName != null && playerName.equals(request.playerName)) {
                        handleExitMatchRequest(playerName, connection);
                    }
                } else if (object instanceof NetworkPacket.RestartMatchRequest) {
                    NetworkPacket.RestartMatchRequest request = (NetworkPacket.RestartMatchRequest) object;
                    String playerName = connectedPlayers.get(connection.getID());
                    // Only host can restart
                    if (playerName != null && playerName.equals(hostPlayerName)) {
                        handleRestartMatchRequest();
                    }
                } else if (object instanceof NetworkPacket.ExitLobbyRequest) {
                    NetworkPacket.ExitLobbyRequest request = (NetworkPacket.ExitLobbyRequest) object;
                    String playerName = connectedPlayers.get(connection.getID());
                    if (playerName != null && playerName.equals(request.playerName)) {
                        handleExitLobbyRequest(playerName, connection);
                    }
                } else if (object instanceof NetworkPacket.PlayerAction) {
                    if (gameLogic == null) return;

                    rebuildPlayerIndexMap();

                    NetworkPacket.PlayerAction action = (NetworkPacket.PlayerAction) object;
                    String playerName = connectedPlayers.get(connection.getID());
                    Integer playerIndex = playerNameToIndex.get(playerName);

                    // Check if player exists and is still in the game
                    if (playerName != null && playerIndex != null) {

                        // --- SERVER-SIDE VALIDATION ---
                        // Check if it's the correct player's turn for BETTING
                        if (gameLogic.getGameState() == GameState.BETTING) {
                            if (playerIndex != gameLogic.getCurrentPlayerIndex()) {
                                Gdx.app.log("GameServer", "IGNORING bet from " + playerName + ": Not their turn.");
                                return; // Not this player's turn to bet
                            }
                            // Process bet action
                            if (action.action == NetworkPacket.PlayerActionType.ADD_TO_BET) {
                                gameLogic.playerAddToBet(action.amount);
                            } else if (action.action == NetworkPacket.PlayerActionType.LOCK_IN_BET) {
                                gameLogic.playerLockInBet(); // This method handles advancing the turn
                            }

                            // Check if it's the correct player's turn for PLAYER_TURN
                        } else if (gameLogic.getGameState() == GameState.PLAYER_TURN) {
                            if (playerIndex != gameLogic.getCurrentPlayerIndex()    ) {
                                Gdx.app.log("GameServer", "IGNORING action from " + playerName + ": Not their turn.");
                                return; // Not this player's turn
                            }
                            // Process game action
                            if (action.action == NetworkPacket.PlayerActionType.HIT) {
                                gameLogic.hit();
                            } else if (action.action == NetworkPacket.PlayerActionType.STAND) {
                                gameLogic.stand();
                            }
                        }

                        sendGameStateUpdate();

                    } else {
                        if (playerName != null && playerIndex == null) {
                            Gdx.app.log("GameServer", "Player " + playerName + " was removed from game (zero balance) - ignoring action");
                        } else {
                            Gdx.app.log("GameServer", "Unregistered player sent action: " + connection.getID());
                        }
                    }
                }
            }

            @Override
            public void disconnected(Connection connection) {
                String playerName = connectedPlayers.remove(connection.getID());
                if (playerName != null) {
                    Gdx.app.log("GameServer", "Player disconnected: " + playerName);
                    // Handle disconnection same as exit match
                    handleExitMatchRequest(playerName, connection);
                }
            }
        });
    }

    public void start() throws IOException {
        server.bind(TCP_PORT, UDP_PORT);
        server.start();
        Gdx.app.log("GameServer", "Server started on port: " + TCP_PORT);
    }

    // Send a lobby update to all connected players
    public void sendLobbyUpdate() {
        NetworkPacket.LobbyUpdate update = new NetworkPacket.LobbyUpdate();
        update.roomName = this.roomName;
        update.maxPlayers = this.maxPlayers;
        update.currentPlayers = connectedPlayers.size();
        update.playerNames = new ArrayList<>(connectedPlayers.values());

        // Send the update to all connected players
        server.sendToAllTCP(update);
    }

    // Send a start game packet to all connected players
    public void sendStartGame() {
        NetworkPacket.StartGame start = new NetworkPacket.StartGame();
        start.maxPlayers = this.maxPlayers;
        start.playerNames = new ArrayList<>(connectedPlayers.values());
        server.sendToAllTCP(start);

        ArrayList<String> playerNames = start.playerNames;
        for (int i = 0; i < playerNames.size(); i++) {
            playerNameToIndex.put(playerNames.get(i), i);
        }
        Gdx.app.log("GameServer", "Starting game with players: " + playerNames);

        // We pass NULL for sequencer and UI, as the server doesn't render.
        this.gameLogic = new BlackjackLogic(null, playerNames);
        this.gameLogic.setGameUI(null);
        this.gameLogic.setGameStateListener(this);

        this.gameLogic.update(0);
    }

    @Override
    public void onGameStateChanged() {

        Gdx.app.log("GameServer", "Logic state changed, broadcasting update...");
        sendGameStateUpdate();

        GameState state = gameLogic.getGameState();
        if (state == GameState.ANIMATIONS_IN_PROGRESS ||
            state == GameState.DEALING_DEALER ||
            state == GameState.DEALING_PLAYERS ||
            state == GameState.FINISHING_ROUND) {
            animationTimer = ANIMATION_DELAY;
        }
    }

    public void update(float delta) {
        if (gameLogic == null) return;

        GameState state = gameLogic.getGameState();

        // Handle animation delay
        if (animationTimer > 0) {
            animationTimer -= delta;
            if (animationTimer <= 0) {
                // Animation delay complete, continue game logic
                gameLogic.update(delta);
            }
            return;
        }

        // Auto-advance certain states
        switch (state) {
            case DEALING_DEALER:
            case DEALING_PLAYERS:
            case ANIMATIONS_IN_PROGRESS:
            case DEALER_TURN:
                gameLogic.update(delta);
                break;
            case RESOLVING_BETS:
            case FINISHING_ROUND:
                // Remove players that disconnected or lost
                processQueuedPlayerRemovals();
                gameLogic.update(delta);
                break;
            case STARTING: // STARTING auto-transitions to BETTING
                gameLogic.update(delta);
                break;
            case BETTING:
            case PLAYER_TURN:
                // Sync player turn
                gameLogic.update(delta);
                break;
            case GAME_OVER:
                break;
        }
    }

    // Rebuilds player order/list after removal
    private void rebuildPlayerIndexMap() {
        playerNameToIndex.clear();
        List<Player> players = gameLogic.getPlayersList();
        for (int i = 0; i < players.size(); i++) {
            playerNameToIndex.put(players.get(i).getName(), i);
        }
        Gdx.app.log("GameServer", "Rebuilt player index map: " + playerNameToIndex);
    }

    // Send a game state update to all connected players
    public void sendGameStateUpdate() {
        if (gameLogic == null) return;

        NetworkPacket.GameStateUpdate update = new NetworkPacket.GameStateUpdate();
        update.currentGameState = gameLogic.getGameState().name();

        rebuildPlayerIndexMap();

        GameState currentPhase = gameLogic.getGameState();

        if (currentPhase == GameState.PLAYER_TURN || currentPhase == GameState.BETTING) {
            if (gameLogic.getCurrentPlayerIndex() < gameLogic.getPlayersList().size()) {
                Player currentPlayer = gameLogic.getPlayersList().get(gameLogic.getCurrentPlayerIndex());
                update.currentPlayerName = currentPlayer.getName();
                update.currentPlayerIndex = gameLogic.getCurrentPlayerIndex();
            }
        } else {
            update.currentPlayerName = "";
            update.currentPlayerIndex = -1;
        }

        // 1. Dealer Cards
        update.dealerCards = new ArrayList<>();
        boolean revealDealerHole = (currentPhase == GameState.DEALER_TURN
            || currentPhase == GameState.RESOLVING_BETS
            || currentPhase == GameState.FINISHING_ROUND);

        for (int i = 0; i < gameLogic.getDealer().m_currentCards.size(); i++) {
            Card c = gameLogic.getDealer().m_currentCards.get(i);
            NetworkPacket.CardInfo info = new NetworkPacket.CardInfo();
            info.id = c.m_id;
            info.rank = c.getRank();
            info.suit = c.getSuit();
            info.isFaceUp = (i > 0) || revealDealerHole;
            update.dealerCards.add(info);
        }

        // 2. Player States - only send active players
        update.players = new ArrayList<>();
        for (Player p : gameLogic.getPlayersList()) {
            if (!p.isActive()) {
                continue;
            }

            NetworkPacket.PlayerInfo pInfo = new NetworkPacket.PlayerInfo();
            pInfo.name = p.getName();
            pInfo.balance = p.getBalance();
            pInfo.currentBet = p.getCurrentBet();
            pInfo.isActive = p.isActive();

            pInfo.cards = new ArrayList<>();
            for (Card c : p.m_currentCards) {
                NetworkPacket.CardInfo info = new NetworkPacket.CardInfo();
                info.id = c.m_id;
                info.rank = c.getRank();
                info.suit = c.getSuit();
                info.isFaceUp = true;
                pInfo.cards.add(info);
            }
            update.players.add(pInfo);
        }

        server.sendToAllTCP(update);
        Gdx.app.log("GameServer", "Broadcasted state: " + update.currentGameState + " (Current Player: " + update.currentPlayerName + ")");
    }

    private void handleExitMatchRequest(String playerName, Connection connection) {
        if (playerName == null) return;

        boolean isHost = playerName.equals(hostPlayerName);

        if (isHost) {
            // Host exited - send exit response to all clients and stop server
            Gdx.app.log("GameServer", "Host " + playerName + " exited - ending match for all players and stopping server");
            NetworkPacket.ExitMatchResponse response = new NetworkPacket.ExitMatchResponse();
            response.hostExited = true;
            response.exitedPlayerName = playerName;
            server.sendToAllTCP(response);

            // Close all connections
            for (Connection conn : server.getConnections()) {
                conn.close();
            }

            // Stop the server
            server.stop();
            Gdx.app.log("GameServer", "Server stopped after host exit");
        } else {
            // Non-host exited - disconnect them immediately
            Gdx.app.log("GameServer", "Non-host " + playerName + " exited - disconnecting");

            // Send exit response before closing connection
            NetworkPacket.ExitMatchResponse response = new NetworkPacket.ExitMatchResponse();
            response.hostExited = false;
            response.exitedPlayerName = playerName;
            connection.sendTCP(response);

            // Remove from connection tracking
            connectedPlayers.remove(connection.getID());
            playerNameToIndex.remove(playerName);

            // Close the connection
            connection.close();

            // If game hasn't started, update lobby
            if (gameLogic == null) {
                sendLobbyUpdate();
            } else {
                // If game is in progress, queue for removal at safe phase
                if (!queuedPlayerRemovals.contains(playerName)) {
                    queuedPlayerRemovals.add(playerName);
                }
            }
        }
    }

    // Adds a hyphen to duplicate player names when joining lobby
    // Necessary due to the fact that the server uses player name as the ID to identify each player
    // (Need to implement a better workaround later, not an emergency though)
    private String ensureUniqueName(String requestedName) {
        String finalName = requestedName;

        // Check if name already exists in connected players
        while (connectedPlayers.containsValue(finalName)) {
            // Append a hyphen to make it unique
            finalName = finalName + "-";
        }

        if (!finalName.equals(requestedName)) {
            Gdx.app.log("GameServer", "Name conflict: " + requestedName + " renamed to " + finalName);
        }

        return finalName;
    }

    private void processQueuedPlayerRemovals() {
        if (queuedPlayerRemovals.isEmpty() || gameLogic == null) return;

        GameState currentState = gameLogic.getGameState();
        // Safe to remove players after resolving bets
        if (currentState == GameState.RESOLVING_BETS || currentState == GameState.FINISHING_ROUND) {
            for (String playerName : new ArrayList<>(queuedPlayerRemovals)) {
                Gdx.app.log("GameServer", "Removing queued player: " + playerName);
                gameLogic.removePlayerByName(playerName);
                queuedPlayerRemovals.remove(playerName);
            }
            rebuildPlayerIndexMap();
            sendGameStateUpdate();
        }
    }

    private void handleExitLobbyRequest(String playerName, Connection connection) {
        if (playerName == null) return;

        boolean isHost = playerName.equals(hostPlayerName);

        if (isHost) {
            // Host exited lobby - close all connections and stop server
            Gdx.app.log("GameServer", "Host " + playerName + " exited lobby - closing all connections");
            NetworkPacket.ExitLobbyResponse response = new NetworkPacket.ExitLobbyResponse();
            response.hostExited = true;
            response.exitedPlayerName = playerName;

            // Send response to all clients (including host's own client)
            try {
                server.sendToAllTCP(response);
            } catch (Exception e) {
                Gdx.app.error("GameServer", "Error sending exit lobby response: " + e.getMessage());
            }

            // Close all connections and stop server
            Gdx.app.postRunnable(() -> {
                try {
                    // Close all connections first
                    List<Connection> connections = new ArrayList<>(server.getConnections());
                    for (Connection conn : connections) {
                        try {
                            conn.close();
                        } catch (Exception e) {
                            // Ignore errors when closing connections
                        }
                    }

                    // Stop the server
                    server.stop();
                    Gdx.app.log("GameServer", "Server stopped after host exit lobby");
                } catch (Exception e) {
                    Gdx.app.error("GameServer", "Error stopping server: " + e.getMessage());
                }
            });
        } else {
            // Non-host exited lobby - remove them
            Gdx.app.log("GameServer", "Non-host " + playerName + " exited lobby");

            // Send exit response before closing connection
            NetworkPacket.ExitLobbyResponse response = new NetworkPacket.ExitLobbyResponse();
            response.hostExited = false;
            response.exitedPlayerName = playerName;
            connection.sendTCP(response);

            // Remove from connection tracking
            connectedPlayers.remove(connection.getID());
            playerNameToIndex.remove(playerName);

            // Close the connection
            connection.close();

            // Update lobby
            sendLobbyUpdate();
        }
    }

    private void handleRestartMatchRequest() {
        if (gameLogic == null) {
            Gdx.app.log("GameServer", "Cannot restart: game not started");
            return;
        }

        // Get list of currently connected players
        ArrayList<String> playerNames = new ArrayList<>(connectedPlayers.values());
        Gdx.app.log("GameServer", "Restarting match with players: " + playerNames);

        // Send restart response to all clients
        NetworkPacket.RestartMatchResponse response = new NetworkPacket.RestartMatchResponse();
        response.playerNames = playerNames;
        server.sendToAllTCP(response);

        // Restart the game logic with the same players
        this.gameLogic = new BlackjackLogic(null, playerNames);
        this.gameLogic.setGameUI(null);
        this.gameLogic.setGameStateListener(this);

        // Rebuild player index map
        rebuildPlayerIndexMap();

        // Start the new game
        this.gameLogic.update(0);
    }

    public void dispose() {
        server.close();
        server.stop();
        Gdx.app.log("GameServer", "Server stopped.");
    }

    public String getRoomName() {
        return roomName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }
}
