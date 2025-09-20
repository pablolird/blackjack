package ECS;

import com.badlogic.gdx.math.Vector2;

public class CTransform {
    public Vector2 m_position;
    public Vector2 m_prevPosition;
    public Vector2 m_velocity;
    public Vector2 m_viewportSize;
    public float m_angularVelocity;
    public float m_rotation;

    public CTransform(Vector2 position, Vector2 size) {
        this.m_position = position;
        this.m_viewportSize = size;
        this.m_prevPosition = position;
        this.m_velocity = new Vector2(0f, 0f);
        this.m_rotation = 0f;
        this.m_angularVelocity = 0f;
    }

    public CTransform(Vector2 position, Vector2 size, Vector2 velocity) {
        this.m_position = position;
        this.m_viewportSize = size;
        this.m_prevPosition = position;
        this.m_velocity = velocity;
    }
}
