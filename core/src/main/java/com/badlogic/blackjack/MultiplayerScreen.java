package com.badlogic.blackjack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import java.util.Arrays;

import java.io.IOException;

public class MultiplayerScreen implements Screen
{
    private final Main game;
    private final Stage stage;
    private final Skin skin;

    private final boolean isHost;
    private final int maxPlayers;
    private final String connectIp;
    private Server server;
    private Client client;

    private final Label statusLabel;
    private final Label counterLabel;

    public MultiplayerScreen(Main game, boolean isHost, int maxPlayers, String connectIp)
    {
        this.game = game;
        this.isHost = isHost;
        this.maxPlayers = maxPlayers;
        this.connectIp = connectIp;

        this.stage = new Stage(new ScreenViewport(), game.spriteBatch);
        this.skin = new Skin(Gdx.files.internal("skin/x1/uiskin.json"));

        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table(skin);
        rootTable.setFillParent(true);
        rootTable.center();

        statusLabel = new Label("", skin, "default-font", Color.WHITE);
        statusLabel.setFontScale(1.5f);
        statusLabel.setAlignment(Align.center);

        counterLabel = new Label("", skin, "default-font", Color.WHITE);
        counterLabel.setFontScale(2.0f);
        counterLabel.setAlignment(Align.center);

        rootTable.add(statusLabel).padBottom(50f).row();
        rootTable.add(counterLabel).row();

        stage.addActor(rootTable);

        if(isHost)
        {
            setupServer();
        }
        else
        {
            setupClient();
        }
    }

    private void setupServer()
    {
        statusLabel.setText("Hosting Game: Waiting for players...");
        server = new Server();
        Network.register(server.getKryo());
        server.start();

        try {
            server.bind(Network.PORT_TCP, Network.PORT_UDP);
            Gdx.app.log("MultiplayerScreen", "Server started on port " + Network.PORT_TCP);

            new Thread(() -> {
                while (server.getConnections() != null) {

                    // FIX: Use Arrays.asList().size() to safely get connection count
                    int connectionCount = Arrays.asList(server.getConnections()).size();

                    Network.HostAdvertise ad = new Network.HostAdvertise(
                        "My Blackjack Game", // Hardcoded lobby name for now
                        connectionCount + 1, // Host + connected clients
                        maxPlayers
                    );
                    server.sendToAllUDP(ad);
                    try {
                        Thread.sleep(5000); // Wait 5 seconds between broadcasts
                    } catch (InterruptedException ignored) {
                        // The thread is stopped by interrupting it (e.g., in dispose)
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "HostAdvertiser").start();

        } catch (IOException e)
        {
            Gdx.app.error("MultiplayerScreen", "Could not start server on port " + Network.PORT_TCP, e);
            statusLabel.setText("Could not start server: " + e.getMessage());
            return;
        }

        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection)
            {
                Gdx.app.log("Server", "Client connected: " + connection.getID());
                updateLobby();
            }

            @Override
            public void disconnected(Connection connection)
            {
                Gdx.app.log("Server", "Client disconnected: " + connection.getID());
                updateLobby();
            }

            @Override
            public void received(Connection connection, Object object)
            {
                if(object instanceof Network.Connect)
                {
                    Network.Connect packet = (Network.Connect) object;
                    Gdx.app.log("Server", "Received connection request from: " + packet.playerName);
                }
            }

        });

        counterLabel.setText("Players: 1/" + maxPlayers);
    }

    private void setupClient()
    {
        statusLabel.setText("Connecting to" + connectIp + "...");
        client = new Client();
        Network.register(client.getKryo());
        client.start();

        new Thread("Connect")
        {
            public void run()
            {
                try
                {
                    client.connect(5000, connectIp, Network.PORT_TCP, Network.PORT_UDP);
                    Gdx.app.postRunnable(() ->
                    {
                       statusLabel.setText("Connected! Waiting for host to start.");
                       client.sendTCP(new Network.Connect("ClientPlayer"));
                    });
                } catch(IOException e)
                {
                    Gdx.app.postRunnable(() ->
                    {
                       Gdx.app.error("MultiplayerScreen", "Client failed to connect", e);
                       statusLabel.setText("Connection failed: " + e.getMessage());
                    });

                }

            }
        }.start();

        client.addListener(new Listener()
        {
           @Override
           public void received(Connection connection, Object object)
           {
               if(object instanceof Network.LobbyUpdate)
               {
                   Network.LobbyUpdate packet = (Network.LobbyUpdate) object;
                   Gdx.app.postRunnable(() ->
                   {
                      counterLabel.setText("Players: " + packet.currentPlayers + "/" + packet.maxPlayers);
                   });
               }
           }
        });
    }

    private void updateLobby()
    {
        int currentPlayers = Arrays.asList(server.getConnections()).size() + 1;
        Network.LobbyUpdate update = new Network.LobbyUpdate(currentPlayers, maxPlayers);
        server.sendToAllTCP(update);

        counterLabel.setText("Players: " + currentPlayers + "/" + maxPlayers);
    }


    @Override
    public void render(float delta)
    {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (server != null) {
            server.stop();
        }
        if (client != null) {
            client.stop();
        }
        stage.dispose();
        skin.dispose();
    }
}
