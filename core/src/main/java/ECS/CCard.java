package ECS;

public class CCard {
    public String m_suit;
    public String m_rank;
    public final int m_id;

    public CCard(int id, String suit, String rank) {
        this.m_suit = suit;
        this.m_rank = rank;
        this.m_id = id;
    }
}
