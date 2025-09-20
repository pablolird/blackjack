package ECS;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class CSprite {
    Sprite m_sprite;

    // This constructor is what we'll use for the board
    public CSprite(Texture t) {
        this.m_sprite = new Sprite(t);
    }

    // This one can be used for cards later
    public CSprite(TextureRegion t) {
        this.m_sprite = new Sprite(t);
    }

    public Sprite getSprite() {
        return this.m_sprite;
    }
}
