package com.badlogic.blackjack;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class BlackjackGame {
    private FitViewport gameViewport;
    private Viewport uiViewport;
    private SpriteBatch spriteBatch;
    private ECS ecs;
    private Sequencer sequencer;
    private BlackjackLogic logic;
    private UI ui;
    private Assets assets; // Add an instance of Assets


    public BlackjackGame(int width, int height) {
        gameViewport = new FitViewport(width, height);
        uiViewport = new ScreenViewport(); // ScreenViewport uses screen pixels
        spriteBatch = new SpriteBatch();
        ui = new UI(uiViewport, spriteBatch);

        assets = new Assets(); // Create and load assets
        assets.loadFromFile();

        ecs = new ECS(assets);
        sequencer = new Sequencer(ecs);
        logic = new BlackjackLogic(sequencer, ui);

        ecs.createBoardEntity(width,height);
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

        uiViewport.apply();
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
