package com.badlogic.blackjack.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.util.HashMap;
import java.util.Map;

public class Assets {
    public TextureRegion[][] cardRegions;
    public Map<String, Sprite> cardMap;
    public Texture board;
    public Texture deck;
    public int cardWidth;
    public int cardHeight;

    public Sound dealCardSFX;
    public Sound betSFX;
    public Sound lockbetSFX;
    public Sound standSFX;
    public Sound buttonSFX;
    public Music bgMusic1;

    public Skin skin;

    public Assets() {}

    public Sprite getCardSprite(String suit, String rank) {
        return cardMap.get(rank + "_of_" + suit);
    }

    // ASSET LOADING
    public void loadFromFile() {
        // Textures and skin are loaded once at startup and shared across screens
        board = new Texture(Gdx.files.internal("board.png"));
        deck = new Texture(Gdx.files.internal("deck.png"));

        Texture cardSheet = new Texture(Gdx.files.internal("TRUEminicards.png"));
        cardWidth = cardSheet.getWidth()/13;
        cardHeight = cardSheet.getHeight()/4;

        // Set the filter for the card sheet texture
        cardSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        cardRegions = TextureRegion.split(cardSheet, cardWidth, cardHeight);

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
        buttonSFX = Gdx.audio.newSound(Gdx.files.internal("button.wav"));
        bgMusic1 = Gdx.audio.newMusic(Gdx.files.internal("SongOption3.ogg"));

        try {
            this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        } catch (Exception e) {
            Gdx.app.error("Assets", "Failed to load uiskin.json from 'ui/uiskin.json'", e);
            try {
                this.skin = new Skin(Gdx.files.internal("skin/x1/uiskin.json"));
            } catch (Exception e2) {
                Gdx.app.error("Assets", "Failed to load uiskin.json from 'skin/x1/uiskin.json'. UI may fail.", e2);
                this.skin = new Skin();
            }
        }
    }
}
