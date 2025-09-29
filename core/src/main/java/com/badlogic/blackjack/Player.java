package com.badlogic.blackjack;

public class Player extends Dealer {
    private final String m_name;
    private int m_balance;
    private int m_currentBet;
    private boolean m_active = true;

    public Player(String name, int balance) {
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
        this.m_balance -= this.m_currentBet;
    }

    public void addToBet(int amount)
    {
        if (this.m_currentBet + amount <= this.m_balance)
        {
            this.m_currentBet += amount;
        }
    }

    public int getCurrentBet()
    {
        return m_currentBet;
    }

    public void addBalance(double multiplier) {
        this.m_balance+= (int) (multiplier*this.m_currentBet);
    }

    public boolean isActive() {
        return this.m_active;
    }

    public void toggleActive() {
        this.m_active = !this.m_active;
    }



    public void reset() {
        this.m_currentCards.clear();
        this.m_currentBet = 0;
        this.m_active = true;
    }
}
