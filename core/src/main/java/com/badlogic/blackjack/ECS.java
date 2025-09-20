package com.badlogic.blackjack;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import ECS.EntityManager;
import ECS.MotionSystem;
import ECS.RenderSystem;

public class ECS {
    private EntityManager entityManager;
    private MotionSystem motionSystem;
    private RenderSystem renderSystem;

    public ECS() {
        entityManager = new EntityManager();
        motionSystem = new MotionSystem();
        renderSystem = new RenderSystem();
    }

    public void update(float delta) {
        motionSystem.move(entityManager.getEntities());
    }

    public void render(SpriteBatch spriteBatch) {
        renderSystem.render(entityManager.getEntities(), spriteBatch);
    }
}
