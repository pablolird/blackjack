package com.badlogic.blackjack;

import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;

public class Get {
//    int[][] boardPositions = {{20,100},{25,105},{20,50},{25,55},{90,20},{95,25},{150,20},
//        {155,25},{210,20},{215,25},{270,100},{275,105},{270,50},{275,55}};
//    int[] boardRotations = {90,90,0,0,0,270,270};

    HashMap<String, Vector2> position;
    HashMap<String, Float> rotation;
    HashMap<String, Vector2> shift;
    Vector2 world_dimensions;

    public Get() {
        world_dimensions = new Vector2(320,180);

        position = new HashMap<>();
        position.put("DECK", new Vector2(172,106));
        position.put("PLAYER1_CARD", new Vector2(53,142));
        position.put("PLAYER2_CARD", new Vector2(86,125));
        position.put("PLAYER3_CARD", new Vector2(120,110));
        position.put("PLAYER4_CARD", new Vector2(160,103));
        position.put("PLAYER5_CARD", new Vector2(200,109));
        position.put("PLAYER6_CARD", new Vector2(233,126));
        position.put("PLAYER7_CARD", new Vector2(267,142));

        rotation = new HashMap<>();
        rotation.put("PLAYER1", 330f);
        rotation.put("PLAYER2", 330f);
        rotation.put("PLAYER3", 330f);
        rotation.put("PLAYER4", 180f);
        rotation.put("PLAYER5", 210f);
        rotation.put("PLAYER6", 210f);
        rotation.put("PLAYER7", 210f);

        shift = new HashMap<>();
        shift.put("PLAYER1", new Vector2(0,-5));
        shift.put("PLAYER2", new Vector2(0,-5));
        shift.put("PLAYER3", new Vector2(-5,0));
        shift.put("PLAYER4", new Vector2(-5,0));
        shift.put("PLAYER5", new Vector2(-5,0));
        shift.put("PLAYER6", new Vector2(0,-5));
        shift.put("PLAYER7", new Vector2(0,-5));
    }

}
