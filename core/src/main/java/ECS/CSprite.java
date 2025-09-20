package ECS;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class CSprite {
    Sprite m_sprite;

    public void setOrigin() {
        float spriteHeight = m_sprite.getHeight();
        float spriteWidth = m_sprite.getWidth();
        m_sprite.setOrigin(spriteWidth/2, spriteHeight/2);
    }

    // This constructor is what we'll use for the board
    public CSprite(Texture t) {
        this.m_sprite = new Sprite(t);
        setOrigin();
    }

    // This one can be used for cards later
    public CSprite(TextureRegion t) {
        this.m_sprite = new Sprite(t);
        setOrigin();
    }

    public Sprite getSprite() {
        return this.m_sprite;
    }
}
