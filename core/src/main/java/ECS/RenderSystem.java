package ECS;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class RenderSystem {
    public void render(List<Entity> entities, SpriteBatch spriteBatch) {
        for (Entity e : entities) {
            if (e.hasComponent(CSprite.class) && e.hasComponent(CTransform.class)) {
                CSprite spriteComponent = (CSprite) e.getComponent(CSprite.class);
                CTransform transformComponent = (CTransform) e.getComponent(CTransform.class);

                Sprite sprite = spriteComponent.getSprite();
                float rotation = transformComponent.m_rotation;
                Vector2 position = transformComponent.m_position;
                Vector2 size = transformComponent.m_viewportSize;

                sprite.setPosition(position.x, position.y);
                sprite.setRotation(rotation);
                sprite.setSize(size.x, size.y);

                sprite.draw(spriteBatch);
            }
        }
    }
}
