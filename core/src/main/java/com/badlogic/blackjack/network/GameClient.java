package com.badlogic.blackjack.network;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.badlogic.blackjack.Main.TCP_PORT;
import static com.badlogic.blackjack.Main.UDP_PORT;

// Game client, handles client-side network communication
public class GameClient {
    // Session modes for client-side state management
    public enum SessionMode {
        NONE,
        LOBBY,
        MATCH
    }

    private final Client client;
    private final Array<LobbyUpdateListener> listeners = new Array<>();
    private final String playerName;
    private Future<?> connectionFuture;
    private SessionMode sessionMode = SessionMode.NONE;
    private boolean exitRequested = false;

    // Interface for handling lobby updates
    public interface LobbyUpdateListener {
        void onLobbyUpdate(NetworkPacket.LobbyUpdate update);
        void onGameStart(NetworkPacket.StartGame start);
        void onGameStateUpdate(NetworkPacket.GameStateUpdate update);
        void onExitMatch(NetworkPacket.ExitMatchResponse response);
        void onRestartMatch(NetworkPacket.RestartMatchResponse response);
        void onExitLobby(NetworkPacket.ExitLobbyResponse response);
        void onLobbyFull(NetworkPacket.LobbyFullResponse response);
    }

    public GameClient(String playerName) {
        this.playerName = playerName;
        client = new Client();
        registerPackets();
        addListeners();
    }

    private void registerPackets() {
        // Register ArrayList
        client.getKryo().register(ArrayList.class);

        // Register nested packet classes before the ones that use them
        client.getKryo().register(NetworkPacket.CardInfo.class);
        client.getKryo().register(NetworkPacket.PlayerInfo.class);

        // Register packet classes
        client.getKryo().register(NetworkPacket.RegisterPlayer.class);
        client.getKryo().register(NetworkPacket.LobbyUpdate.class);
        client.getKryo().register(NetworkPacket.StartGame.class);
        client.getKryo().register(NetworkPacket.PlayerActionType.class);
        client.getKryo().register(NetworkPacket.PlayerAction.class);
        client.getKryo().register(NetworkPacket.ExitMatchRequest.class);
        client.getKryo().register(NetworkPacket.ExitMatchResponse.class);
        client.getKryo().register(NetworkPacket.RestartMatchRequest.class);
        client.getKryo().register(NetworkPacket.RestartMatchResponse.class);
        client.getKryo().register(NetworkPacket.ExitLobbyRequest.class);
        client.getKryo().register(NetworkPacket.ExitLobbyResponse.class);
        client.getKryo().register(NetworkPacket.LobbyFullResponse.class);
        client.getKryo().register(NetworkPacket.GameStateUpdate.class);
    }

    private void addListeners() {
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof NetworkPacket.LobbyUpdate) {
                    final NetworkPacket.LobbyUpdate update = (NetworkPacket.LobbyUpdate) object;
                    Gdx.app.postRunnable(() -> {
                        for (LobbyUpdateListener listener : listeners) {
                            listener.onLobbyUpdate(update);
                        }
                    });
                } else if (object instanceof NetworkPacket.StartGame) {
                    final NetworkPacket.StartGame start = (NetworkPacket.StartGame) object;
                    Gdx.app.postRunnable(() -> {
                        for (LobbyUpdateListener listener : listeners) {
                            listener.onGameStart(start);
                        }
                    });
                } else if (object instanceof NetworkPacket.GameStateUpdate) {
                    final NetworkPacket.GameStateUpdate update = (NetworkPacket.GameStateUpdate) object;
                    Gdx.app.postRunnable(() -> {
                        for (LobbyUpdateListener listener : listeners) {
                            listener.onGameStateUpdate(update);
                        }
                    });
                } else if (object instanceof NetworkPacket.ExitMatchResponse) {
                    final NetworkPacket.ExitMatchResponse response = (NetworkPacket.ExitMatchResponse) object;
                    Gdx.app.postRunnable(() -> {
                        for (LobbyUpdateListener listener : listeners) {
                            listener.onExitMatch(response);
                        }
                    });
                } else if (object instanceof NetworkPacket.RestartMatchResponse) {
                    final NetworkPacket.RestartMatchResponse response = (NetworkPacket.RestartMatchResponse) object;
                    Gdx.app.postRunnable(() -> {
                        for (LobbyUpdateListener listener : listeners) {
                            listener.onRestartMatch(response);
                        }
                    });
                } else if (object instanceof NetworkPacket.ExitLobbyResponse) {
                    final NetworkPacket.ExitLobbyResponse response = (NetworkPacket.ExitLobbyResponse) object;
                    Gdx.app.postRunnable(() -> {
                        for (LobbyUpdateListener listener : listeners) {
                            listener.onExitLobby(response);
                        }
                    });
                } else if (object instanceof NetworkPacket.LobbyFullResponse) {
                    final NetworkPacket.LobbyFullResponse response = (NetworkPacket.LobbyFullResponse) object;
                    Gdx.app.postRunnable(() -> {
                        for (LobbyUpdateListener listener : listeners) {
                            listener.onLobbyFull(response);
                        }
                    });
                }
            }

            @Override
            public void disconnected(Connection connection) {
                Gdx.app.log("GameClient", "Disconnected from server.");
                // Treat disconnection as exit match - notify listeners
            }
        });
    }

    // Add a listener for lobby updates
    public void addLobbyUpdateListener(LobbyUpdateListener listener) {
        listeners.add(listener);
    }

    // Remove a listener for lobby updates
    public void removeLobbyUpdateListener(LobbyUpdateListener listener) {
        listeners.removeValue(listener, true);
    }

    // Send an action to the server
    public void sendAction(NetworkPacket.PlayerActionType type, int amount) {
        NetworkPacket.PlayerAction action = new NetworkPacket.PlayerAction();
        action.action = type;
        action.amount = amount;
        client.sendTCP(action);
        Gdx.app.log("GameClient", "Sent action: " + type + " / " + amount);
    }
    public void sendAction(NetworkPacket.PlayerActionType type) {
        sendAction(type, 0);
    }

    // Send an exit match request to the server
    public void sendExitMatchRequest() {
        exitRequested = true;
        if (!client.isConnected()) {
            return;
        }
        NetworkPacket.ExitMatchRequest request = new NetworkPacket.ExitMatchRequest();
        request.playerName = this.playerName;
        client.sendTCP(request);
        Gdx.app.log("GameClient", "Sent exit match request");
    }

    // Send a restart match request to the server
    public void sendRestartMatchRequest() {
        NetworkPacket.RestartMatchRequest request = new NetworkPacket.RestartMatchRequest();
        client.sendTCP(request);
        Gdx.app.log("GameClient", "Sent restart match request");
    }

    // Send an exit lobby request to the server
    public void sendExitLobbyRequest(String playerName) {
        exitRequested = true;
        if (!client.isConnected()) {
            return;
        }
        NetworkPacket.ExitLobbyRequest request = new NetworkPacket.ExitLobbyRequest();
        request.playerName = playerName;
        client.sendTCP(request);
        Gdx.app.log("GameClient", "Sent exit lobby request");
    }

    // Send an exit lobby request to the server
    public void sendExitLobbyRequest() {
        sendExitLobbyRequest(this.playerName);
    }

    public void connect(final String ipAddress) {
        client.start();

        // Use an executor to manage the connection attempt with a timeout
        connectionFuture = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                // 5000ms timeout for connection
                client.connect(5000, ipAddress, TCP_PORT, UDP_PORT);
                Gdx.app.log("GameClient", "Successfully connected to server at " + ipAddress);

                // Send the initial registration packet
                NetworkPacket.RegisterPlayer register = new NetworkPacket.RegisterPlayer();
                register.name = this.playerName;
                client.sendTCP(register);

            } catch (IOException e) {
                Gdx.app.error("GameClient", "Failed to connect to " + ipAddress + ": " + e.getMessage());
            }
        });
    }

    public void dispose() {
        if (client.isConnected() && !exitRequested) {
            if (sessionMode == SessionMode.MATCH) {
                sendExitMatchRequest();
            } else if (sessionMode == SessionMode.LOBBY) {
                sendExitLobbyRequest();
            }
        }

        // Ensure the connect attempt future is cancelled so threads do not leak
        if (connectionFuture != null && !connectionFuture.isDone()) {
            connectionFuture.cancel(true);
        }
        client.close();
        client.stop();
        Gdx.app.log("GameClient", "Client stopped.");
        sessionMode = SessionMode.NONE;
    }

    public void setSessionMode(SessionMode sessionMode) {
        this.sessionMode = sessionMode;
    }

    public SessionMode getSessionMode() {
        return sessionMode;
    }

    public boolean isConnected() {
        return client.isConnected();
    }
}
