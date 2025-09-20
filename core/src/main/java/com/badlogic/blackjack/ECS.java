package com.badlogic.blackjack;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import ECS.*;

public class ECS {
    private EntityManager entityManager;
    private MotionSystem motionSystem;
    private RenderSystem renderSystem;
    private Assets assets; // Store the assets manager

    // Constructor now accepts Assets
    public ECS(Assets assets) {
        this.assets = assets;
        entityManager = new EntityManager();
        motionSystem = new MotionSystem();
        renderSystem = new RenderSystem();
    }

    // --- FACADE METHOD ---
    // This translates the game-specific request into generic ECS actions
    public void createBoardEntity(int width, int height) {
        Entity board = entityManager.createEntity("board");
        board.addComponent(new CTransform(new Vector2(0, 0), new Vector2(width, height)));
        board.addComponent(new CSprite(assets.board)); // Use the loaded board texture
    }


    public void update(float delta) {
        motionSystem.move(entityManager.getEntities());
    }

    public void render(SpriteBatch spriteBatch) {
        // We clear the screen in Main.java, so we just need to render here.
        // spriteBatch.begin() and end() are now handled inside RenderSystem for better encapsulation.
        renderSystem.render(entityManager.getEntities(), spriteBatch);
    }
}
