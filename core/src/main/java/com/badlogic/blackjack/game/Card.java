package com.badlogic.blackjack.game;

// Card class
public class Card {
    // ID tracking is necessary to reference specific cards for animations or logic, as well as for
    // synchronization in multiplayer.
    private static int nextId = 0;
    public final int m_id;
    private final String m_rank;
    private final String m_suit;
    private int m_value;

    public Card(String rank, String suit) {
        this.m_id = nextId++;
        this.m_rank = rank;
        this.m_suit = suit;

        // Q, J, K all worth 10 in blackjack
        if (rank.equals("Q") || rank.equals("J") || rank.equals("K")) {
            this.m_value = 10;
        }
        // Aces are first assigned 11, but can change to a 1 later in calculations
        else if (rank.equals("A")) {
            this.m_value = 11;
        }
        else {
            this.m_value = Integer.parseInt(rank);
        }
    }

    // Constructor for network synchronization - allows setting a specific ID
    public Card(int id, String rank, String suit) {
        this.m_id = id;
        // Update nextId to avoid conflicts
        if (id >= nextId) {
            nextId = id + 1;
        }
        this.m_rank = rank;
        this.m_suit = suit;

        if (rank.equals("Q") || rank.equals("J") || rank.equals("K")) {
            this.m_value = 10;
        }
        else if (rank.equals("A")) {
            this.m_value = 11;
        }
        else {
            this.m_value = Integer.parseInt(rank);
        }
    }

    public String getSuit() {
        return m_suit;
    }


    public int getValue() {
        return this.m_value;
    }

    public String getRank() {
        return this.m_rank;
    }

    public void setValue(int value) {
        this.m_value = value;
    }

}
