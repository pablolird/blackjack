package ECS;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class CSprite {
    Sprite m_sprite;

    public CSprite(String path) {
        this.m_sprite = new Sprite(new Texture("drop.png"));
    }

    public CSprite(TextureRegion t) {
        this.m_sprite = new Sprite(t);
    }

    public CSprite(Texture t) {
        this.m_sprite = new Sprite(t);
    }

    public Sprite getSprite() {
        return this.m_sprite;
    }

}
