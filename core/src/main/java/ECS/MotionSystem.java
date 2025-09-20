package ECS;

import com.badlogic.gdx.Gdx;

import java.util.List;

public class MotionSystem {
    public void move(List<Entity> entities) {
        float delta = Gdx.graphics.getDeltaTime();
        for (Entity e : entities) {
            if (e.hasComponent(CTransform.class)) {
                CTransform entityTransform = (CTransform) e.getComponent(CTransform.class);

                entityTransform.m_prevPosition.set(entityTransform.m_position);
                entityTransform.m_position.add(entityTransform.m_velocity.cpy().scl(delta));
                entityTransform.m_rotation+= entityTransform.m_angularVelocity;
            }
        }
    }
}
