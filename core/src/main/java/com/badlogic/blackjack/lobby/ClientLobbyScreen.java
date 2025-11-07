package com.badlogic.blackjack.lobby;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.badlogic.blackjack.Main;
import com.badlogic.blackjack.GameScreen;
import com.badlogic.blackjack.network.GameClient;
import com.badlogic.blackjack.network.NetworkPacket;
import com.badlogic.blackjack.network.GameClient.LobbyUpdateListener;

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

    public ClientLobbyScreen(Main game, String ipAddress) {
        this.game = game;
        this.ipAddress = ipAddress;
        this.skin = game.assets.skin;
        this.stage = new Stage(new ScreenViewport());

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
        root.add(new Label("Waiting for host to start match...", skin)).pad(20).row();

        // --- Networking Setup ---
        // For simplicity, hardcode a player name for the joining client
        client = new GameClient("Guest_" + (int)(Math.random() * 100));
        client.addLobbyUpdateListener(this);
        client.connect(ipAddress);
        game.gameClient = client; // Store client for cleanup
    }

    private void transitionToGame(int finalMaxPlayers) {
        // Client transitions to GameScreen
        game.setScreen(new GameScreen(game, finalMaxPlayers));
        dispose();
    }

    @Override
    public void onLobbyUpdate(NetworkPacket.LobbyUpdate update) {
        // Status updated once connected
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
        // Host has started the game, transition!
        transitionToGame(start.maxPlayers);
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
        // Client disposal is handled in Main.dispose() or when transitioning
    }
}
