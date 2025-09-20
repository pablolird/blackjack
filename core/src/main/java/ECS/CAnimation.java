package ECS;

import com.badlogic.gdx.math.Vector2;

public class CAnimation {
    public Vector2 m_targetPosition;
    public float m_speed;

    public CAnimation(Vector2 v, float speed) {
        this.m_targetPosition = v;
        this.m_speed = speed;
    }
}
