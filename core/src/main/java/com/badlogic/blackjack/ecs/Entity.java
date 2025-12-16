package com.badlogic.blackjack.ecs;

import java.util.HashMap;

// Entity class which assigns entity components (such as to the cards)
public class Entity {
    int m_id;
    public String m_tag;
    HashMap<Class<?>, Object> m_components;

    Entity(int id, String tag) {
        this.m_id = id;
        this.m_tag = tag;
        this.m_components = new HashMap<>();
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
