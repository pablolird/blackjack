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

public class GameClient {
    private final Client client;
    private final Array<LobbyUpdateListener> listeners = new Array<>();
    private final String playerName;
    private Future<?> connectionFuture;

    public interface LobbyUpdateListener {
        void onLobbyUpdate(NetworkPacket.LobbyUpdate update);
        void onGameStart(NetworkPacket.StartGame start);
    }

    public GameClient(String playerName) {
        this.playerName = playerName;
        client = new Client();
        registerPackets();
        addListeners();
    }

    private void registerPackets() {
        client.getKryo().register(NetworkPacket.RegisterPlayer.class);
        client.getKryo().register(NetworkPacket.LobbyUpdate.class);
        client.getKryo().register(NetworkPacket.StartGame.class);
        client.getKryo().register(ArrayList.class);
    }

    private void addListeners() {
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof NetworkPacket.LobbyUpdate) {
                    final NetworkPacket.LobbyUpdate update = (NetworkPacket.LobbyUpdate) object;
                    // Run listener callback on the LibGDX render thread
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
                }
            }

            @Override
            public void disconnected(Connection connection) {
                Gdx.app.log("GameClient", "Disconnected from server.");
            }
        });
    }

    public void addLobbyUpdateListener(LobbyUpdateListener listener) {
        listeners.add(listener);
    }

    /**
     * Attempts to connect to the server and automatically starts the client thread.
     * @param ipAddress The IP address of the host.
     */
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
                // Handle connection failure, e.g., transition back to start screen
            }
        });
    }

    public void dispose() {
        if (connectionFuture != null && !connectionFuture.isDone()) {
            connectionFuture.cancel(true);
        }
        client.close();
        client.stop();
        Gdx.app.log("GameClient", "Client stopped.");
    }
}
