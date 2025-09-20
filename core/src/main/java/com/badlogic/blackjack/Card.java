package com.badlogic.blackjack;

public class Card {
    private final String m_rank;
    private final String m_suit;
    private int m_value;

    public Card(String rank, String suit) {
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
