package com.badlogic.blackjack.ecs;

import com.badlogic.blackjack.ecs.components.CCard;
import com.badlogic.blackjack.ecs.components.CSprite;
import com.badlogic.blackjack.ecs.components.CTransform;
import com.badlogic.blackjack.ecs.systems.RenderSystem;
import com.badlogic.blackjack.assets.Assets;
import com.badlogic.blackjack.game.Card;
import com.badlogic.blackjack.assets.Get;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import java.util.List;
import java.util.stream.Collectors;


public class ECS {
    private final Get g;
    private final EntityManager entityManager;
    private final RenderSystem renderSystem;
    private final Assets assets; // Store the assets manager
    private static final float CARD_SCALE = 2.0f;

    public ECS(Assets assets) {
        this.assets = assets;
        entityManager = new EntityManager();
        renderSystem = new RenderSystem();
        g = new Get();
    }

    // Translate game-specific request into generic ECS actions
    public void createBoardEntity(int width, int height) {
        Entity board = entityManager.createEntity("board");
        board.addComponent(new CTransform(new Vector2(g.world_dimensions.x/2, g.world_dimensions.y/2), new Vector2(width, height)));
        board.addComponent(new CSprite(assets.board)); // Use the loaded board texture
    }

    public Entity createCardEntity(Card c) {
        Entity card = entityManager.createEntity("card");
        Vector2 cardSize = new Vector2(assets.cardWidth * CARD_SCALE, assets.cardHeight * CARD_SCALE);
        card.addComponent(new CTransform(new Vector2(g.position.get("DECK")), cardSize));
        card.addComponent(new CSprite(assets.getCardSprite(c.getSuit(),c.getRank())));
        card.addComponent(new CCard(c.m_id, c.getSuit(), c.getRank()));

        return card;
    }

    public void createDeckEntity() {
        Entity deck = entityManager.createEntity("deck");
        Vector2 cardSize = new Vector2(assets.deck.getWidth()*0.65f, assets.deck.getHeight()*0.65f);

        deck.addComponent(new CTransform(new Vector2(g.position.get("DECK")), cardSize));
        deck.addComponent(new CSprite(assets.deck));
    }

    public void clearCardEntities() {
        List<Entity> toRemove = entityManager.getEntities().stream()
            .filter(e -> e.hasComponent(CCard.class))
            .collect(Collectors.toList());

        toRemove.forEach(entityManager::destroyEntity);
    }

    public Entity findCardEntity(int cardId) {
        for (Entity e : entityManager.getEntities()) {
            if (e.hasComponent(CCard.class)) {
                CCard cCard = (CCard) e.getComponent(CCard.class);
                if (cCard.m_id == cardId) {
                    return e;
                }
            }
        }
        return null;
    }

    public void update(float delta) {
        entityManager.update();
    }

    public void render(SpriteBatch spriteBatch) {
        renderSystem.render(entityManager.getEntities(), spriteBatch);
    }
}
