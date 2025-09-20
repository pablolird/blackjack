package ECS;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;

public class Assets {
    public TextureRegion[][] cardRegions; // grid of cards
    public Map<String, Sprite> cardMap;
    public Texture board;

    public Assets() {}


    public void loadFromFile() {
        board = new Texture(Gdx.files.internal("board.png"));
        Texture cardSheet = new Texture(Gdx.files.internal("cardSheet.png"));
        int cardWidth = 48;
        int cardHeight = 64;

        cardRegions = Sprite.split(cardSheet, cardWidth, cardHeight);

        // Optional: build a flat map if you want easy name lookup
        cardMap = new HashMap<>();
        String[] suits = {"hearts", "diamonds", "clubs", "spades"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

        for (int suit = 0; suit < 4; suit++) {
            for (int rank = 0; rank < 13; rank++) {
                cardMap.put(ranks[rank] + "_of_" + suits[suit], new Sprite(cardRegions[suit][rank]));
            }
        }
    }
}
