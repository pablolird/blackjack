package com.badlogic.blackjack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen; // <-- Import Screen
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.blackjack.audio.AudioManager;

// Implement the Screen interface
public class GameScreen implements Screen {
    private final Main game; // Reference to the Main game class

    private final FitViewport gameViewport;
    private final Viewport uiViewport;
    private final SpriteBatch spriteBatch; // Will be the shared batch
    private final Assets assets; // Will be the shared assets

    private final ECS ecs;
    private final Sequencer sequencer;
    private final BlackjackLogic logic;
    private final UI ui;
    private final AudioManager audioManager;

    public GameScreen(Main game) {
        this.game = game;
        this.assets = game.assets; // Get shared assets
        this.spriteBatch = game.spriteBatch; // Get shared sprite batch

        // Use the constants from Main
        gameViewport = new FitViewport(Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        uiViewport = new FitViewport(960, 540);

        // Assets are now loaded in Main.java
        audioManager = new AudioManager(assets);
        ecs = new ECS(assets);
        sequencer = new Sequencer(ecs, audioManager);
        logic = new BlackjackLogic(sequencer);
        ui = new UI(uiViewport, spriteBatch, logic, audioManager);

        logic.setGameUI(ui);
        // Use constants from Main
        ecs.createBoardEntity(Main.WORLD_WIDTH, Main.WORLD_HEIGHT);
        audioManager.playMusic(assets.bgMusic1, 0.3f);
    }

    @Override
    public void render(float delta) {
        // --- Update Logic ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            ui.togglePauseMenu();
        }

        if (!ui.isPaused()) {
            logic.update(delta);
            sequencer.update(delta);
        }

        ecs.update(delta);
        ui.update(delta);

        // --- Render Logic ---
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        gameViewport.apply();
        spriteBatch.setProjectionMatrix(gameViewport.getCamera().combined);

        spriteBatch.begin();
        ecs.render(spriteBatch);
        spriteBatch.end();
        ui.render();
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        ui.resize(width, height);
    }

    @Override
    public void dispose() {
        // IMPORTANT: Do NOT dispose of shared assets or SpriteBatch here
        // Only dispose of things created *by this screen*
        ui.dispose();
    }

    // Other required Screen methods
    @Override
    public void show() {
        // Called when this screen becomes active
    }

    @Override
    public void hide() {
        // Called when this screen is no longer active
    }

    @Override
    public void pause() {
        // For handling game pause
    }

    @Override
    public void resume() {
        // For handling game resume
    }
}
