package com.badlogic.blackjack.actions;

import ECS.CTransform;
import ECS.Entity;
import com.badlogic.gdx.math.Vector2;

public class ShiftToAction implements Action {
    private Entity entity;
    private Vector2 shiftAmount;
    private Vector2 targetPosition;
    private float speed;
    private CTransform transform;
    private boolean initialized = false;

    public ShiftToAction(Entity entity, Vector2 shiftAmount, float speed) {
        this.entity = entity;
        this.speed = speed;
        this.shiftAmount = shiftAmount;
        this.transform = (CTransform) entity.getComponent(CTransform.class);
    }

    @Override
    public boolean update(float delta) {
        if (!initialized) {
            // Calculate the target position on the first update
            targetPosition = transform.m_position.cpy().add(shiftAmount);
            initialized = true;
        }

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
