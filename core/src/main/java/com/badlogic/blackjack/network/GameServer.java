package com.badlogic.blackjack.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.badlogic.gdx.Gdx;
import com.badlogic.blackjack.BlackjackLogic; // New import
import com.badlogic.blackjack.Sequencer; // New import - will need a non-rendering one for the server

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.badlogic.blackjack.Main.TCP_PORT;
import static com.badlogic.blackjack.Main.UDP_PORT;

public class GameServer {
    private final Server server;
    private final String roomName;
    private final int maxPlayers;
    // Map connection ID to Player Name
    private final Map<Integer, String> connectedPlayers = new HashMap<>();

    // --- NEW FIELDS FOR GAME STATE ---
    // Maps player name to their index in the BlackjackLogic playersList
    private final Map<String, Integer> playerNameToIndex = new HashMap<>();
    private BlackjackLogic gameLogic;
    // --- END NEW FIELDS ---

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
        // --- NEW REGISTRATIONS ---
        server.getKryo().register(NetworkPacket.PlayerActionType.class); // Register the enum
        server.getKryo().register(NetworkPacket.PlayerAction.class);
        server.getKryo().register(NetworkPacket.GameStateUpdate.class);
        // --- END NEW REGISTRATIONS ---
        // Also need to register any custom types used in the DTOs
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
                    // --- NEW: Handle Game Actions ---
                    if (gameLogic == null) return; // Ignore if game hasn't started

                    NetworkPacket.PlayerAction action = (NetworkPacket.PlayerAction) object;
                    String playerName = connectedPlayers.get(connection.getID());
                    Integer playerIndex = playerNameToIndex.get(playerName);

                    if (playerName != null && playerIndex != null) {
                        Gdx.app.log("GameServer", "Action from " + playerName + " (Index: " + playerIndex + "): " + action.action + " / " + action.amount);
                        // In the next step, we will delegate this to gameLogic
                    } else {
                        Gdx.app.log("GameServer", "Unregistered player sent action: " + connection.getID());
                    }
                    // --- END NEW ---
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

    // Modified to include player names in the StartGame packet
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
        // Server-side logic initialization will be added here in the next step.
        // --- END NEW ---
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
