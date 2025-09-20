package com.badlogic.blackjack.actions;

import ECS.CTransform;
import ECS.Entity;
import com.badlogic.gdx.math.Vector2;

public class MoveToAction implements Action {
    private Entity entity;
    private Vector2 targetPosition;
    private float speed;
    private CTransform transform;

    public MoveToAction(Entity entity, Vector2 targetPosition, float speed) {
        this.entity = entity;
        this.targetPosition = targetPosition;
        this.speed = speed;
        this.transform = (CTransform) entity.getComponent(CTransform.class);
    }

    @Override
    public boolean update(float delta) {
        if (transform.m_position.equals(targetPosition)) {
            return true; // Action is complete
        }

        Vector2 direction = targetPosition.cpy().sub(transform.m_position).nor();
        float distance = speed * delta;

        if (transform.m_position.dst(targetPosition) < distance) {
            transform.m_position.set(targetPosition);
            return true; // Arrived, action is complete
        } else {
            transform.m_position.mulAdd(direction, distance);
            return false; // Still moving
        }
    }
}
