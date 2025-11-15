package com.badlogic.blackjack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.ui.Skin; // <-- NEW IMPORT

import java.util.HashMap;
import java.util.Map;

public class Assets {
    public TextureRegion[][] cardRegions; // grid of cards
    public Map<String, Sprite> cardMap;
    public Texture board;
    public int cardWidth;
    public int cardHeight;

    public Sound dealCardSFX;
    public Sound betSFX;
    public Sound lockbetSFX;
    public Sound standSFX;
    public Music bgMusic1;

    public Skin skin; // <-- NEW FIELD: Skin for UI elements

    public Assets() {}

    public Sprite getCardSprite(String suit, String rank) {
        return cardMap.get(rank + "_of_" + suit);
    }

    public void loadFromFile() {
        board = new Texture(Gdx.files.internal("board.png"));
        // Set the filter for the board texture
//        board.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Texture cardSheet = new Texture(Gdx.files.internal("TRUEminicards.png"));
        cardWidth = cardSheet.getWidth()/13;
        cardHeight = cardSheet.getHeight()/4;
        // Set the filter for the card sheet texture
        cardSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        cardRegions = TextureRegion.split(cardSheet, cardWidth, cardHeight);

        // Optional: build a flat map if you want easy name lookup
        cardMap = new HashMap<>();
        String[] suits = {"hearts", "diamonds", "clubs", "spades"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

        for (int suit = 0; suit < 4; suit++) {
            for (int rank = 0; rank < 13; rank++) {
                cardMap.put(ranks[rank] + "_of_" + suits[suit], new Sprite(cardRegions[suit][rank]));
            }
        }

        dealCardSFX = Gdx.audio.newSound(Gdx.files.internal("cardSlide2AMPx.mp3"));
        betSFX = Gdx.audio.newSound(Gdx.files.internal("betSFX.mp3"));
        lockbetSFX = Gdx.audio.newSound(Gdx.files.internal("lock.wav"));
        standSFX = Gdx.audio.newSound(Gdx.files.internal("STANDSFXV1.wav"));
        bgMusic1 = Gdx.audio.newMusic(Gdx.files.internal("SongOption3.ogg"));

        try {
            // Attempt to load from the 'ui' path first, as seen in your fallback logic
            this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        } catch (Exception e) {
            Gdx.app.error("Assets", "Failed to load uiskin.json from 'ui/uiskin.json'", e);
            // Fallback to the 'skin/x1' path
            try {
                this.skin = new Skin(Gdx.files.internal("skin/x1/uiskin.json"));
            } catch (Exception e2) {
                Gdx.app.error("Assets", "Failed to load uiskin.json from 'skin/x1/uiskin.json'. UI may fail.", e2);
                this.skin = new Skin(); // Provide an empty skin to prevent crash
            }
        }
    }
}
