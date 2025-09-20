package com.badlogic.blackjack;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class BlackjackGame {
    private FitViewport viewport;
    private SpriteBatch spriteBatch;
    private ECS ecs;
    private Sequencer sequencer;
    private BlackjackLogic logic;
    private Assets assets; // Add an instance of Assets


    public BlackjackGame(int width, int height) {
        viewport = new FitViewport(width, height);
        spriteBatch = new SpriteBatch();

        assets = new Assets(); // Create and load assets
        assets.loadFromFile();

        ecs = new ECS(assets);
        sequencer = new Sequencer(ecs);
        logic = new BlackjackLogic(sequencer);

        ecs.createBoardEntity(width,height);
    }

    public void update(float delta) {
        logic.update(delta);
        sequencer.update(delta);
        ecs.update(delta);
    }

    public void render() {
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);

        spriteBatch.begin();
        ecs.render(spriteBatch);
        spriteBatch.end();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void dispose() {
        spriteBatch.dispose();
    }
}
