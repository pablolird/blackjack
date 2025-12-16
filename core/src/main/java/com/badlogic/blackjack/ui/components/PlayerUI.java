package com.badlogic.blackjack.ui.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

public class PlayerUI {
    public Window playerWindow;
    public Label playerName;
    public Label playerScore;
    public Label playerBalance;

    public PlayerUI(Window pW, Label playerName, int score, int balance, Skin skin) {
        this.playerWindow = pW;
        this.playerName = playerName;
        playerScore = new Label(Integer.toString(score), skin);
        playerBalance = new Label(Integer.toString(balance), skin);

         // Smaller font + themed colors
        playerScore.setFontScale(0.4f);
        playerScore.setColor(255/255f, 230/255f, 156/255f, 1);

        playerBalance.setFontScale(0.4f);
        playerBalance.setColor(Color.YELLOW);
    }

    public void updateScore(int score) {
        playerScore.setText(Integer.toString(score));
    }

    public void updateBalance(int balance) {
        playerBalance.setText(Integer.toString(balance));
    }
}
