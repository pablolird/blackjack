package com.badlogic.blackjack.animation.actions;

// For introducing delays between actions
public class DelayAction implements Action {
    private float duration;

    public DelayAction(float durationInSeconds) {
        this.duration = durationInSeconds;
    }

    @Override
    public boolean update(float delta) {
        // Tick down the remaining time; returns true when elapsed
        duration -= delta;
        return duration <= 0;
    }
}
