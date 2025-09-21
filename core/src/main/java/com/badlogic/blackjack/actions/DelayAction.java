package com.badlogic.blackjack.actions;

public class DelayAction implements Action {
    private float duration;

    public DelayAction(float durationInSeconds) {
        this.duration = durationInSeconds;
    }

    @Override
    public boolean update(float delta) {
        duration -= delta;
        return duration <= 0;
    }
}
