package com.badlogic.blackjack.lobby;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.badlogic.blackjack.Main;
import com.badlogic.blackjack.GameScreen;
import com.badlogic.blackjack.network.GameServer;
import com.badlogic.blackjack.network.GameClient;
import com.badlogic.blackjack.network.NetworkPacket;
import com.badlogic.blackjack.network.GameClient.LobbyUpdateListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HostLobbyScreen implements Screen, LobbyUpdateListener {
    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private GameClient client;
    private final String roomName;
    private final int maxPlayers;
    private final String hostPlayerName;

    private List<String> connectedPlayerNames = new ArrayList<>();

    private Label roomNameLabel;
    private Label playerCounterLabel;
    private Label playersListLabel;
    private TextButton startGameButton;
    private Label hostIPLabel;

    public HostLobbyScreen(Main game, String hostPlayerName, String roomName, int maxPlayers) {
        this.game = game;
        this.roomName = roomName;
        this.maxPlayers = maxPlayers;
        this.skin = game.assets.skin; // Assuming a skin is available via Main/Assets
        this.hostPlayerName = hostPlayerName; // INITIALIZE NEW FIELD

        this.stage = new Stage(new ScreenViewport(), game.spriteBatch);

        Gdx.input.setInputProcessor(stage);

        // --- UI Setup ---
        Table root = new Table(skin);
        root.setFillParent(true);
        stage.addActor(root);

        roomNameLabel = new Label("Room: " + roomName, skin, "default");
        playerCounterLabel = new Label("Players: 1/" + maxPlayers, skin, "default");
        playersListLabel = new Label("Host (You)", skin);
        playersListLabel.setAlignment(Align.center);
        startGameButton = new TextButton("Start Game", skin);
        hostIPLabel = new Label("Host IP: [Starting Server...]", skin);

        root.add(roomNameLabel).pad(10).row();
        root.add(hostIPLabel).pad(10).row();
        root.add(playerCounterLabel).pad(10).row();
        root.add(new ScrollPane(playersListLabel, skin)).expand().fill().pad(20).row();
        root.add(startGameButton).width(200).height(50).pad(20).row();

        // Host can't start if they are the only one (optional rule)
        startGameButton.setDisabled(true);

        // --- Networking Setup ---
        try {
            // 1. Start the Server and STORE IT IN MAIN
            game.gameServer = new GameServer(roomName, maxPlayers); // CREATE AND STORE
            game.gameServer.start();

            // Display Host IP
            hostIPLabel.setText("Host IP: " + getLocalIP());

            // 2. Start the Client (Host is also a client)
            client = new GameClient(hostPlayerName);
            client.addLobbyUpdateListener(this);
            // Host connects to their own local server
            client.connect("127.0.0.1");
            game.gameClient = client; // Store client for cleanup

        } catch (IOException e) {
            Gdx.app.error("HostLobbyScreen", "Failed to start server/client: " + e.getMessage());
            // Transition back to StartScreen on failure
            game.setScreen(new com.badlogic.blackjack.StartScreen(game));
        }

        startGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Broadcast the START_GAME packet to all clients
                game.gameServer.sendStartGame();
                // Host immediately transitions to the GameScreen
                transitionToGame(maxPlayers, connectedPlayerNames); // Modified call
            }
        });
    }

    // Quick and dirty way to get a local IP. For real deployment, this is harder.
    private String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException e) {
            return "127.0.0.1 (Localhost)";
        }
    }

    private void transitionToGame(int finalMaxPlayers, List<String> playerNames) {
        // The server (game.gameServer) now remains active.

        // Pass the player names and isHost=true flag
        game.setScreen(new GameScreen(game, true, playerNames));
        dispose();
    }

    @Override
    public void onLobbyUpdate(NetworkPacket.LobbyUpdate update) {
        // --- MODIFIED: Store the player names ---
        this.connectedPlayerNames = update.playerNames;
        // --- END MODIFIED ---

        // Update player counter
        playerCounterLabel.setText("Players: " + update.currentPlayers + "/" + update.maxPlayers);

        // Update players list
        StringBuilder sb = new StringBuilder();
        for (String name : update.playerNames) {
            sb.append(name).append("\n");
        }
        playersListLabel.setText(sb.toString());

        // Enable start button if enough players (e.g., more than 1)
        startGameButton.setDisabled(update.currentPlayers < 2);
    }

    // --- NEW LISTENER METHOD ---
    @Override
    public void onGameStateUpdate(NetworkPacket.GameStateUpdate update) {
        // Not used in the lobby screen, but required by the interface
    }
    // --- END NEW LISTENER METHOD ---

    @Override
    public void onGameStart(NetworkPacket.StartGame start) {
        // Host client received its own start packet.
        transitionToGame(start.maxPlayers, start.playerNames);
    }
    
    @Override
    public void onExitMatch(NetworkPacket.ExitMatchResponse response) {
        // If match ended, return to start screen
        Gdx.app.log("HostLobbyScreen", "Received exit match response - returning to start screen");
        game.setScreen(new com.badlogic.blackjack.StartScreen(game));
        dispose();
    }
    
    @Override
    public void onRestartMatch(NetworkPacket.RestartMatchResponse response) {
        // Restart match - transition to game screen with the same players
        Gdx.app.log("HostLobbyScreen", "Received restart match response - starting game");
        game.setScreen(new com.badlogic.blackjack.GameScreen(game, true, response.playerNames));
        dispose();
    }

    @Override
    public void show() { }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        stage.dispose();
        // The server is NOT disposed here; it stays running until Main.dispose()
    }
}
