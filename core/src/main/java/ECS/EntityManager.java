package ECS;

import java.util.ArrayList;
import java.util.List;

public class EntityManager {
    private List<Entity> m_entities = new ArrayList<>();
    public List<List<Entity>> m_playerCardsList = new ArrayList<>();
    private int nextId = 0;

    public EntityManager() {}


    public Entity createEntity(String tag) {
        Entity e = new Entity(nextId++, tag); // uses factory
        m_entities.add(e);
        return e;
    }

    public void destroyEntity(Entity e) {
        m_entities.remove(e);
    }

    public List<Entity> getEntities() {
        return m_entities;
    }
}
