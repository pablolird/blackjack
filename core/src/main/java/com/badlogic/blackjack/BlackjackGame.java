package com.badlogic.blackjack;

public class BlackjackGame {
    private ECS ecs;
    private Sequencer sequencer;
    private BlackjackLogic logic;

    public BlackjackGame() {
        ecs = new ECS();
        sequencer = new Sequencer(ecs);
        logic = new BlackjackLogic(sequencer);
    }

    public void update(float delta) {
        // 1. Advance game logic
        logic.update(delta);

        // 2. Advance actions/sequences
        sequencer.update(delta);

        // 3. Advance ECS systems
        ecs.update(delta);
    }

    public void render() {
        ecs.render();
    }

    public void dispose() {
        ecs.dispose();
    }
}
