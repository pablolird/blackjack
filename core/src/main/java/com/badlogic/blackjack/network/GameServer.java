package com.badlogic.blackjack.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.badlogic.gdx.Gdx;
import com.badlogic.blackjack.BlackjackLogic;
import com.badlogic.blackjack.GameStateListener;
import com.badlogic.blackjack.GameState;
import com.badlogic.blackjack.Player;
import com.badlogic.blackjack.Card;
import com.badlogic.blackjack.Dealer;
import com.badlogic.blackjack.Sequencer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static com.badlogic.blackjack.Main.TCP_PORT;
import static com.badlogic.blackjack.Main.UDP_PORT;

public class GameServer implements GameStateListener {
    private final Server server;
    private final String roomName;
    private final int maxPlayers;
    // Map connection ID to Player Name
    private final Map<Integer, String> connectedPlayers = new HashMap<>();
    private final Map<String, Integer> playerNameToIndex = new HashMap<>();
    private BlackjackLogic gameLogic;

    public GameServer(String roomName, int maxPlayers) {
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        server = new Server();
        registerPackets();
        addListeners();
    }

    private void registerPackets() {
        // Must register all classes that will be sent over the network
        server.getKryo().register(NetworkPacket.RegisterPlayer.class);
        server.getKryo().register(NetworkPacket.LobbyUpdate.class);
        server.getKryo().register(NetworkPacket.StartGame.class);
        server.getKryo().register(NetworkPacket.PlayerActionType.class); // Register the enum
        server.getKryo().register(NetworkPacket.PlayerAction.class);
        server.getKryo().register(NetworkPacket.GameStateUpdate.class);

        server.getKryo().register(NetworkPacket.CardInfo.class);
        server.getKryo().register(NetworkPacket.PlayerInfo.class);
        server.getKryo().register(ArrayList.class);
    }

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
                        connectedPlayers.put(connection.getID(), packet.name);
                        Gdx.app.log("GameServer", "Registered player: " + packet.name + " (Total: " + connectedPlayers.size() + ")");
                        sendLobbyUpdate();
                    } else {
                        Gdx.app.log("GameServer", "Lobby full. Rejecting connection: " + packet.name);
                        connection.close();
                    }
                } else if (object instanceof NetworkPacket.PlayerAction) {
                    // --- MODIFIED: Handle Game Actions ---
                    if (gameLogic == null) return; // Ignore if game hasn't started

                    NetworkPacket.PlayerAction action = (NetworkPacket.PlayerAction) object;
                    String playerName = connectedPlayers.get(connection.getID());
                    Integer playerIndex = playerNameToIndex.get(playerName);

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
                        // --- END VALIDATION ---

                        // IMPORTANT: Broadcast the new state to everyone after a valid action
                        // Note: playerLockInBet and stand() will ALSO trigger onGameStateChanged(),
                        // so this broadcast might be redundant, but it's safer to have it
                        // to show immediate feedback (like adding to a bet).
                        sendGameStateUpdate();

                    } else {
                        Gdx.app.log("GameServer", "Unregistered player sent action: " + connection.getID());
                    }
                    // --- END MODIFIED ---
                }
            }

            @Override
            public void disconnected(Connection connection) {
                String playerName = connectedPlayers.remove(connection.getID());
                if (playerName != null) {
                    Gdx.app.log("GameServer", "Player disconnected: " + playerName);
                    playerNameToIndex.remove(playerName); // Keep the map clean
                    sendLobbyUpdate();
                }
            }
        });
    }

    public void start() throws IOException {
        server.bind(TCP_PORT, UDP_PORT);
        server.start();
        Gdx.app.log("GameServer", "Server started on port: " + TCP_PORT);
    }

    public void sendLobbyUpdate() {
        NetworkPacket.LobbyUpdate update = new NetworkPacket.LobbyUpdate();
        update.roomName = this.roomName;
        update.maxPlayers = this.maxPlayers;
        update.currentPlayers = connectedPlayers.size();
        update.playerNames = new ArrayList<>(connectedPlayers.values());

        // Send the update to all connected players
        server.sendToAllTCP(update);
    }

    public void sendStartGame() {
        NetworkPacket.StartGame start = new NetworkPacket.StartGame();
        start.maxPlayers = this.maxPlayers;
        start.playerNames = new ArrayList<>(connectedPlayers.values()); // Include player names
        server.sendToAllTCP(start);

        // --- NEW: Server-Side Player Index Mapping ---
        ArrayList<String> playerNames = start.playerNames;
        for (int i = 0; i < playerNames.size(); i++) {
            playerNameToIndex.put(playerNames.get(i), i);
        }
        Gdx.app.log("GameServer", "Starting game with players: " + playerNames);

        // --- NEW: Server-side logic initialization ---
        // We pass NULL for sequencer and UI, as the server doesn't render.
        this.gameLogic = new BlackjackLogic(null, playerNames);
        this.gameLogic.setGameUI(null); // Explicitly set UI to null
        this.gameLogic.setGameStateListener(this); // Set the callback

        // Manually call update(0) once to kick-start the logic
        // from STARTING to BETTING, which triggers the first broadcast.
        this.gameLogic.update(0);
        // --- END NEW ---
    }

    @Override
    public void onGameStateChanged() {
        // This is called by gameLogic.notifyStateChanged()
        Gdx.app.log("GameServer", "Logic state changed, broadcasting update...");
        sendGameStateUpdate();
    }

    public void sendGameStateUpdate() {
        if (gameLogic == null) return;

        NetworkPacket.GameStateUpdate update = new NetworkPacket.GameStateUpdate();
        // --- MODIFIED ---
        update.currentGameState = gameLogic.getGameState().name();

        // Determine current player for turn-based phases
        // --- MODIFIED ---
        GameState currentPhase = gameLogic.getGameState();

        if (currentPhase == GameState.PLAYER_TURN || currentPhase == GameState.BETTING) {
            // --- MODIFIED ---
            if (gameLogic.getCurrentPlayerIndex() < gameLogic.getPlayersList().size()) {
                // --- MODIFIED ---
                Player currentPlayer = gameLogic.getPlayersList().get(gameLogic.getCurrentPlayerIndex());
                update.currentPlayerName = currentPlayer.getName(); // This will work now
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

        // --- MODIFIED ---
        // (Accessing m_currentCards is OK because it's public in Dealer.java)
        for (int i = 0; i < gameLogic.getDealer().m_currentCards.size(); i++) {
            Card c = gameLogic.getDealer().m_currentCards.get(i);
            // ... (CardInfo logic remains the same)
            NetworkPacket.CardInfo info = new NetworkPacket.CardInfo();
            info.id = c.m_id;
            info.rank = c.getRank();
            info.suit = c.getSuit();
            info.isFaceUp = (i > 0) || revealDealerHole;
            update.dealerCards.add(info);
        }

        // 2. Player States
        update.players = new ArrayList<>();
        // --- MODIFIED ---
        for (Player p : gameLogic.getPlayersList()) {
            NetworkPacket.PlayerInfo pInfo = new NetworkPacket.PlayerInfo();
            pInfo.name = p.getName();
            pInfo.balance = p.getBalance();
            pInfo.currentBet = p.getCurrentBet();
            pInfo.isActive = p.isActive();

            pInfo.cards = new ArrayList<>();
            // (Accessing m_currentCards is OK because it's public in Dealer.java, which Player extends)
            for (Card c : p.m_currentCards) {
                // ... (CardInfo logic remains the same)
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
