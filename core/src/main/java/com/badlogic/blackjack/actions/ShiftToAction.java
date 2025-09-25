package com.badlogic.blackjack.actions;

import ECS.CTransform;
import ECS.Entity;
import com.badlogic.gdx.math.Vector2;

public class ShiftToAction implements Action {
    private final Vector2 shiftAmount;
    private final float duration;
    private float elapsedTime = 0f;

    private final CTransform transform;
    private Vector2 startPosition;
    private Vector2 targetPosition;


    public ShiftToAction(Entity entity, Vector2 shiftAmount, float duration) {
        this.shiftAmount = shiftAmount;
        this.duration = duration > 0 ? duration : 0.0001f; // Avoid division by zero
        this.transform = (CTransform) entity.getComponent(CTransform.class);
    }

    @Override
    public boolean update(float delta) {
        if (startPosition == null) {
            // On the first frame, capture the start and calculate the final target position
            startPosition = transform.m_position.cpy();
            targetPosition = startPosition.cpy().add(shiftAmount);
        }

        elapsedTime += delta;

        // Calculate how far along the animation is (from 0.0 to 1.0)
        float progress = Math.min(1f, elapsedTime / duration);

        // Linearly interpolate (lerp) the position
        transform.m_position.set(startPosition).lerp(targetPosition, progress);

        // The action is done when progress is 1.0 or more
        return progress >= 1f;
    }
}
