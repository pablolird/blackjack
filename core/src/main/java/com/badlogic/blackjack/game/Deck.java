package com.badlogic.blackjack.game;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

// Deck class
public class Deck {
    private ArrayList<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
        String[] suits = {"hearts", "diamonds", "clubs", "spades"};
        String[] signs = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

        for (String suit : suits) {
            for (String sign : signs) {
                this.cards.add(new Card(sign, suit));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(this.cards, new Random());
    }

    public Card deal() {
        if (this.cards.isEmpty()) {
            return null;
        }
        return this.cards.remove(0);
    }

    public void reset() {
        this.cards.clear();
        String[] suits = {"hearts", "diamonds", "clubs", "spades"};
        String[] signs = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};

        for (String suit : suits) {
            for (String sign : signs) {
                this.cards.add(new Card(sign, suit));
            }
        }
        shuffle();
    }
}
