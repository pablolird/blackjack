package ECS;
import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;

public class Entity {
    int m_id;
    String m_tag;
    HashMap<Class<?>, Object> m_components;

    Entity(int id, String tag) {
        this.m_id = id;
        this.m_tag = tag;
        this.m_components = new HashMap<>();
        if (tag.equals("card")) {
            this.addComponent(new CTransform(new Vector2(160, 90), new Vector2(24, 32)));
            CTransform card_transform = (CTransform) this.getComponent(CTransform.class);
            card_transform.m_angularVelocity = 4f;
            this.addComponent(new CState("MOVING"));
            CState entity_state = (CState) this.getComponent(CState.class);
            entity_state.m_initialPosition = card_transform.m_position;
        }
    }

    public void addComponent(Object component) {
        if (!m_components.containsKey(component.getClass())) {
            m_components.put(component.getClass(), component);
        }
    }

    public boolean hasComponent(Class<?> c) {
        return m_components.containsKey(c);
    }

    public Object getComponent(Class<?> c) {
        return m_components.get(c);
    }
}
