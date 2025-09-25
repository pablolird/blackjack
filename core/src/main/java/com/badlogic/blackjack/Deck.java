package com.badlogic.blackjack;
import java.util.Random;

public class Deck {
    // Optional: build a flat map if you want easy name lookup
    Random r;
    String[] suits = {"hearts", "diamonds", "clubs", "spades"};
    String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

    public Deck() {
        r = new Random();
    }

    public Card drawCard() {
        int suitIndex = r.nextInt(4);
        int rankIndex = r.nextInt(13);

        return new Card(ranks[rankIndex], suits[suitIndex]);
    }
}
