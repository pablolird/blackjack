package com.badlogic.blackjack.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.badlogic.gdx.Gdx;

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
                }
            }

            @Override
            public void disconnected(Connection connection) {
                String playerName = connectedPlayers.remove(connection.getID());
                if (playerName != null) {
                    Gdx.app.log("GameServer", "Player disconnected: " + playerName);
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
        server.sendToAllTCP(start);
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
