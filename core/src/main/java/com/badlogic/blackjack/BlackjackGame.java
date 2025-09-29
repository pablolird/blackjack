package com.badlogic.blackjack;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.blackjack.audio.AudioManager;

public class BlackjackGame {
    private final FitViewport gameViewport;
    private final Viewport uiViewport;
    private final SpriteBatch spriteBatch;
    private final ECS ecs;
    private final Sequencer sequencer;
    private final BlackjackLogic logic;
    private final UI ui;
    private final Assets assets;
    private final AudioManager audioManager;

    public BlackjackGame(int width, int height) {
        gameViewport = new FitViewport(width, height);
        uiViewport = new FitViewport(960,540); // ScreenViewport uses screen pixels
        spriteBatch = new SpriteBatch();

        // Add an instance of Assets
        assets = new Assets(); // Create and load assets
        assets.loadFromFile();

        audioManager = new AudioManager(assets);

        ecs = new ECS(assets);
        sequencer = new Sequencer(ecs, audioManager);
        logic = new BlackjackLogic(sequencer);
        ui = new UI(uiViewport, spriteBatch, logic, audioManager);

        logic.setGameUI(ui);
        ecs.createBoardEntity(width,height);
        audioManager.playMusic(assets.bgMusic1, 0.3f);
    }

    public void update(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // This is a simple toggle, you might want more complex logic
            ui.togglePauseMenu();
        }

        // Only update the logic if the game isn't paused
        if (!ui.isPaused()) { // Assuming you add an isPaused() method to GameUI
            logic.update(delta);
            sequencer.update(delta);
        }

        ecs.update(delta);
        ui.update(delta);
    }

    public void render() {
        gameViewport.apply();
        spriteBatch.setProjectionMatrix(gameViewport.getCamera().combined);

        spriteBatch.begin();
        ecs.render(spriteBatch);
        spriteBatch.end();
        ui.render();
    }

    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        ui.resize(width, height);
    }

    public void dispose() {
        spriteBatch.dispose();
        ui.dispose();
    }
}
