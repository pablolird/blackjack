package com.badlogic.blackjack;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import ECS.*;


public class ECS {
    private EntityManager entityManager;
    private RenderSystem renderSystem;
    private Assets assets; // Store the assets manager

    // HARD-CODED VALUES THAT SHOULD MAYBE BELONG SOMEWHERE ELSE:
    private float cardWidth = 24;
    private float cardHeight = 32;

    // Constructor now accepts Assets
    public ECS(Assets assets) {
        this.assets = assets;
        entityManager = new EntityManager();
        renderSystem = new RenderSystem();
    }

    // --- FACADE METHOD ---
    // This translates the game-specific request into generic ECS actions
    public void createBoardEntity(int width, int height) {
        Entity board = entityManager.createEntity("board");
        board.addComponent(new CTransform(new Vector2(0, 0), new Vector2(width, height)));
        board.addComponent(new CSprite(assets.board)); // Use the loaded board texture
    }

    public Entity createCardEntity(String suit, String rank, float width, float height) {
        Entity card = entityManager.createEntity("card");
        card.addComponent(new CTransform(new Vector2(width/2, height/2+50f),
                                         new Vector2(cardWidth, cardHeight)));
        card.addComponent(new CSprite(assets.getCardSprite(suit,rank))); // Use the loaded board texture
        card.addComponent(new CCard(suit, rank));

        return card;
    }


    public void update(float delta) {

    }

    public void render(SpriteBatch spriteBatch) {
        // We clear the screen in Main.java, so we just need to render here.
        // spriteBatch.begin() and end() are now handled inside RenderSystem for better encapsulation.
        renderSystem.render(entityManager.getEntities(), spriteBatch);
    }
}
