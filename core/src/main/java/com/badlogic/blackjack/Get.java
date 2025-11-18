package com.badlogic.blackjack;

import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;

public class Get {
    HashMap<String, Vector2> position;
    HashMap<String, Float> rotation;
    HashMap<String, Vector2> shift;
    HashMap<String, Vector2> scoreShift;
    Vector2 world_dimensions;

    public Get() {
        world_dimensions = new Vector2(960, 540);

        position = new HashMap<>();
        position.put("DECK", new Vector2(480, 270));
        position.put("DEALER_CARD", new Vector2(480, 330));
        position.put("PLAYER1_CARD", new Vector2(360, 330));
        position.put("PLAYER2_CARD", new Vector2(258, 280));
        position.put("PLAYER3_CARD", new Vector2(360, 210));
        position.put("PLAYER4_CARD", new Vector2(480, 210));
        position.put("PLAYER5_CARD", new Vector2(600, 210));
        position.put("PLAYER6_CARD", new Vector2(699, 280));
        position.put("PLAYER7_CARD", new Vector2(600, 330));


        scoreShift = new HashMap<>();
        scoreShift.put("DEALER",  new Vector2(0, 104));
        scoreShift.put("PLAYER1", new Vector2(-30, 104));
        scoreShift.put("PLAYER2", new Vector2(-160, -20));
        scoreShift.put("PLAYER3", new Vector2(-70, -90));
        scoreShift.put("PLAYER4", new Vector2(0, -90));
        scoreShift.put("PLAYER5", new Vector2(70, -90));
        scoreShift.put("PLAYER6", new Vector2(160, -20));
        scoreShift.put("PLAYER7", new Vector2(30, 104));

        rotation = new HashMap<>();
        rotation.put("DEALER", 0f);
        rotation.put("PLAYER1", 180f);
        rotation.put("PLAYER2", 90f);
        rotation.put("PLAYER3", 180f);
        rotation.put("PLAYER4", 180f);
        rotation.put("PLAYER5", 180f);
        rotation.put("PLAYER6", 90f);
        rotation.put("PLAYER7", 180f);

        shift = new HashMap<>();
        shift.put("DEALER",  new Vector2(-15, 0));
        shift.put("PLAYER1", new Vector2(-15, 0));
        shift.put("PLAYER2", new Vector2(0, -15));
        shift.put("PLAYER3", new Vector2(-15, 0));
        shift.put("PLAYER4", new Vector2(-15, 0));
        shift.put("PLAYER5", new Vector2(-15, 0));
        shift.put("PLAYER6", new Vector2(0, -15));
        shift.put("PLAYER7", new Vector2(-15, 0));
    }
}
