package com.badlogic.blackjack;

import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;

public class Get {
//    int[][] boardPositions = {{20,100},{25,105},{20,50},{25,55},{90,20},{95,25},{150,20},
//        {155,25},{210,20},{215,25},{270,100},{275,105},{270,50},{275,55}};
//    int[] boardRotations = {90,90,0,0,0,270,270};

    HashMap<String, Vector2> position;
    HashMap<String, Float> rotation;
    Vector2 world_dimensions;

    public Get() {
        world_dimensions = new Vector2(320,180);

        position = new HashMap<>();
        position.put("DECK", new Vector2(160,90));
        position.put("PLAYER1_CARD", new Vector2(20,100));
        position.put("PLAYER2_CARD", new Vector2(20,50));
        position.put("PLAYER3_CARD", new Vector2(90,20));
        position.put("PLAYER4_CARD", new Vector2(150,20));
        position.put("PLAYER5_CARD", new Vector2(210,20));
        position.put("PLAYER6_CARD", new Vector2(270,50));
        position.put("PLAYER7_CARD", new Vector2(270,100));

        rotation = new HashMap<>();
        rotation.put("PLAYER1", 90f);
        rotation.put("PLAYER2", 90f);
        rotation.put("PLAYER3", 0f);
        rotation.put("PLAYER4", 0f);
        rotation.put("PLAYER5", 0f);
        rotation.put("PLAYER6", 270f);
        rotation.put("PLAYER7", 270f);
    }

}
