package com.badlogic.blackjack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashMap;
import java.util.Map;

public class UI {
    private Stage stage;
    private Skin skin;
    private SpriteBatch spriteBatch;
    private Table playerActionTable;
    private Window pauseMenu;
    private Map<Player, Label> playerScoreLabels;
    private boolean paused;

    public UI(Viewport vp, SpriteBatch sb) {
        this.paused = false;
        playerScoreLabels = new HashMap<>();

        stage = new Stage(vp, sb);
        skin = new Skin(Gdx.files.internal("skin/sgx-ui.json"));

        Gdx.input.setInputProcessor(stage);

        // 2. Build the Player Action Panel (initially hidden)
        playerActionTable = new Table();
        playerActionTable.setFillParent(true);
        playerActionTable.bottom(); // Anchor to the bottom
        stage.addActor(playerActionTable);

        TextButton hitButton = new TextButton("Hit", skin);
        TextButton standButton = new TextButton("Stand", skin);
        playerActionTable.add(hitButton).pad(10);
        playerActionTable.add(standButton).pad(10);
        playerActionTable.setVisible(false); // Hide it by default

        // 3. Build the Pause Menu (initially hidden)
        pauseMenu = new Window("Pause", skin);
        TextButton quitButton = new TextButton("Leave Room", skin);
        pauseMenu.add(quitButton);
        pauseMenu.pack(); // Size the window to its contents
        pauseMenu.setPosition(stage.getWidth() / 2 - pauseMenu.getWidth() / 2, stage.getHeight() / 2 - pauseMenu.getHeight() / 2);
        stage.addActor(pauseMenu);
        pauseMenu.setVisible(false); // Hide it by default
    }

    public void update(float delta) {
        stage.act(delta);
    }

    public void render() {
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }

    public void showPlayerActionPanel(boolean visible) {
        playerActionTable.setVisible(visible);
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void togglePauseMenu() {
        paused = !paused;
        pauseMenu.setVisible(paused);
    }
}
