package com.badlogic.blackjack.ecs.components;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class CSprite {
    Sprite m_sprite;

    // Constructor for board
    public CSprite(Texture t) {
        this.m_sprite = new Sprite(t);
    }

    // Constructor for cards
    public CSprite(TextureRegion t) {
        this.m_sprite = new Sprite(t);
    }

    public Sprite getSprite() {
        return this.m_sprite;
    }
}
