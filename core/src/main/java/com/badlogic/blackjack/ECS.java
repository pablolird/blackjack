package com.badlogic.blackjack;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import ECS.*;


public class ECS {
    private final Get g;
    private final EntityManager entityManager;
    private final RenderSystem renderSystem;
    private final Assets assets; // Store the assets manager
    private static final float CARD_SCALE = 2.0f;

    // Constructor now accepts Assets
    public ECS(Assets assets) {
        this.assets = assets;
        entityManager = new EntityManager();
        renderSystem = new RenderSystem();
        g = new Get();
    }

    // --- FACADE METHOD ---
    // This translates the game-specific request into generic ECS actions
    public void createBoardEntity(int width, int height) {
        Entity board = entityManager.createEntity("board");
        board.addComponent(new CTransform(new Vector2(g.world_dimensions.x/2, g.world_dimensions.y/2), new Vector2(width, height)));
        board.addComponent(new CSprite(assets.board)); // Use the loaded board texture
    }

    public Entity createCardEntity(Card c) {
        Entity card = entityManager.createEntity("card");
        Vector2 cardSize = new Vector2(assets.cardWidth * CARD_SCALE, assets.cardHeight * CARD_SCALE);
        card.addComponent(new CTransform(new Vector2(g.position.get("DECK")), cardSize));
        card.addComponent(new CSprite(assets.getCardSprite(c.getSuit(),c.getRank()))); // Use the loaded board texture
        card.addComponent(new CCard(c.m_id, c.getSuit(), c.getRank()));

        return card;
    }

    // You will also need a way to find an entity by its card ID
    public Entity findCardEntity(int cardId) {
        for (Entity e : entityManager.getEntities()) {
            if (e.hasComponent(CCard.class)) {
                CCard cCard = (CCard) e.getComponent(CCard.class);
                if (cCard.m_id == cardId) {
                    return e;
                }
            }
        }
        return null; // Or throw an exception if not found
    }

    public void update(float delta) {

    }

    public void render(SpriteBatch spriteBatch) {
        // We clear the screen in Main.java, so we just need to render here.
        // spriteBatch.begin() and end() are now handled inside RenderSystem for better encapsulation.
        renderSystem.render(entityManager.getEntities(), spriteBatch);
    }
}
