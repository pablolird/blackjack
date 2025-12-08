package com.badlogic.blackjack.game;

import java.util.ArrayList;


public class Dealer {
    public ArrayList<Card> m_currentCards = new ArrayList<>();

    public Dealer() {}


    public void addCard(String sign, String suit) {
        this.m_currentCards.add(new Card(sign, suit));
    }

    public void addCard(Card c) {
        this.m_currentCards.add(c);
    }

    public void reset() {
        this.m_currentCards.clear();
    }

    public boolean hasCard(int cardId) {
        for (Card c : m_currentCards) {
            if (c.m_id == cardId) {
                return true;
            }
        }
        return false;
    }

    public int totalValue() {
        int totalValue = 0;
        int aceCount = 0;

        for (Card c : this.m_currentCards) {
            if (!c.getRank().equals("A")) {
                totalValue+=c.getValue();
            }
            else {
                aceCount++;
            }
        }

        totalValue+=(11*aceCount);

        if (totalValue>21 && aceCount>0) {
            while (aceCount>0 && totalValue>21) {
                totalValue-=10;
                aceCount--;
            }
        }

        return totalValue;
    }

    public StringBuilder displayInfo() {
        StringBuilder info = new StringBuilder("Cards: ");

        for (Card c : this.m_currentCards) {
            info.append(c.getRank()).append(" ");
        }
        info.append("\nTotal value: ").append(this.totalValue());

        return info;
    }

}
