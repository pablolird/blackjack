package com.badlogic.blackjack;
// Player class
public class Player extends Dealer {
    private final String m_name;
    private final int m_id;
    private int m_balance;
    private int m_currentBet;
    private boolean m_active = true;

    public Player(String name, int id, int balance) {
        this.m_id = id;
        this.m_name = name;
        this.m_balance = balance;
        this.m_currentBet = 0;
    }

    public String getName() {
        return this.m_name;
    }

    public int getBalance() {
        return m_balance;
    }

    public void lockInBet()
    {
        // Deprecated :(
    }

    public void addToBet(int amount)
    {
        if (this.m_balance >= amount)
        {
            this.m_balance -= amount;
            this.m_currentBet += amount;
        }
    }

    public int getCurrentBet()
    {
        return m_currentBet;
    }
    public void addBalance(int amount)
    {
        this.m_balance += amount;
    }

    public void setBalance(int newBalance) {
        this.m_balance = newBalance;
    }

    public void setCurrentBet(int newBet) {
        this.m_currentBet = newBet;
    }

    public boolean isActive() {
        return this.m_active;
    }

    public void toggleActive() {
        this.m_active = !this.m_active;
    }

    public void reset()
    {
        this.m_currentCards.clear();
        this.m_currentBet = 0;
        this.m_active = true;
    }

    public int getID() {
        return m_id;
    }
}
