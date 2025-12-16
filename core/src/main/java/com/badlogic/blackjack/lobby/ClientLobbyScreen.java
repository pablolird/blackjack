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
import com.badlogic.blackjack.network.GameClient;
import com.badlogic.blackjack.network.NetworkPacket;
import com.badlogic.blackjack.network.GameClient.LobbyUpdateListener;
import com.badlogic.blackjack.audio.AudioManager;
import com.badlogic.blackjack.audio.SoundType;

import java.util.ArrayList;
import java.util.List;

public class ClientLobbyScreen implements Screen, LobbyUpdateListener {
    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final GameClient client;
    private final String ipAddress;

    private Label statusLabel;
    private Label roomNameLabel;
    private Label playerCounterLabel;
    private Label playersListLabel;
    private final String playerName;
    private final AudioManager audioManager;

    public ClientLobbyScreen(Main game, String playerName, String ipAddress) {
        this.game = game;
        this.ipAddress = ipAddress;
        this.audioManager = new AudioManager(game.assets);
        this.skin = game.assets.skin;
        this.stage = new Stage(new ScreenViewport(), game.spriteBatch);
        this.playerName = playerName;

        Gdx.input.setInputProcessor(stage);

        // --- UI Setup ---
        Table root = new Table(skin);
        root.setFillParent(true);
        stage.addActor(root);

        // Initial values before first update
        statusLabel = new Label("Connecting to " + ipAddress + "...", skin, "default");
        roomNameLabel = new Label("Room: N/A", skin, "default");
        playerCounterLabel = new Label("Players: 0/0", skin, "default");
        playersListLabel = new Label("Waiting...", skin);
        playersListLabel.setAlignment(Align.center);

        root.add(statusLabel).pad(10).row();
        root.add(roomNameLabel).pad(10).row();
        root.add(playerCounterLabel).pad(10).row();
        root.add(new ScrollPane(playersListLabel, skin)).expand().fill().pad(20).row();
        root.add(new Label("Waiting for host...", skin)).pad(20).row();

        // Exit lobby button
        TextButton exitLobbyButton = new TextButton("Exit Lobby", skin);
        root.add(exitLobbyButton).width(200).height(50).pad(20).row();

        exitLobbyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audioManager.playSound(SoundType.BUTTON, 1.0f);
                handleExitLobby();
            }
        });

        // --- Networking Setup ---
        client = new GameClient(playerName);
        client.setSessionMode(GameClient.SessionMode.LOBBY);
        client.addLobbyUpdateListener(this);
        client.connect(ipAddress);
        game.gameClient = client;
    }

    private void transitionToGame(List<String> playerNames) {
        // Transition to game screen
        game.setScreen(new GameScreen(game, false, playerNames));
        dispose();
    }

    private void handleExitLobby() {
        // Send exit lobby request to server
        if (client != null) {
            client.sendExitLobbyRequest();
        }

        // Disconnect and return to start screen
        if (game.gameClient != null) {
            game.gameClient.dispose();
            game.gameClient = null;
        }

        // Fall back to the main menu once network is cleaned up
        game.setScreen(new com.badlogic.blackjack.StartScreen(game));
        dispose();
    }

    @Override
    public void onLobbyUpdate(NetworkPacket.LobbyUpdate update) {
        // Update status when connected
        statusLabel.setText("Connected to Host at " + ipAddress);
        roomNameLabel.setText("Room: " + update.roomName);
        playerCounterLabel.setText("Players: " + update.currentPlayers + "/" + update.maxPlayers);

        // Update players list
        StringBuilder sb = new StringBuilder();
        for (String name : update.playerNames) {
            sb.append(name).append("\n");
        }
        playersListLabel.setText(sb.toString());
    }

    @Override
    public void onGameStart(NetworkPacket.StartGame start) {
        transitionToGame(start.playerNames);
    }

    @Override
    public void onGameStateUpdate(NetworkPacket.GameStateUpdate update) {
    }

    @Override
    public void onExitMatch(NetworkPacket.ExitMatchResponse response) {
        // If host exited or match ended, return to start screen
        Gdx.app.log("ClientLobbyScreen", "Received exit match response - returning to start screen");
        game.setScreen(new com.badlogic.blackjack.StartScreen(game));
        dispose();
    }

    @Override
    public void onRestartMatch(NetworkPacket.RestartMatchResponse response) {
        // Restart match - transition to game screen with the same players
        Gdx.app.log("ClientLobbyScreen", "Received restart match response - starting game");
        game.setScreen(new com.badlogic.blackjack.GameScreen(game, false, response.playerNames));
        dispose();
    }

    @Override
    public void onExitLobby(NetworkPacket.ExitLobbyResponse response) {
        Gdx.app.log("ClientLobbyScreen", "Received exit lobby response: hostExited=" + response.hostExited);

        // Disconnect and return to start screen
        if (game.gameClient != null) {
            game.gameClient.dispose();
            game.gameClient = null;
        }

        game.setScreen(new com.badlogic.blackjack.StartScreen(game));
        dispose();
    }

    @Override
    public void onLobbyFull(NetworkPacket.LobbyFullResponse response) {
        Gdx.app.log("ClientLobbyScreen", "Lobby is full - " + response.message);

        // If lobby is full
        statusLabel.setText("Lobby is full! Returning to start screen...");

        // Disconnect after a short delay
        Gdx.app.postRunnable(() -> {
            if (game.gameClient != null) {
                game.gameClient.dispose();
                game.gameClient = null;
            }
            game.setScreen(new com.badlogic.blackjack.StartScreen(game));
            dispose();
        });
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
    }
}
