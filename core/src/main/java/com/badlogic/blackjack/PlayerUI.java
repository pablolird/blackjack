package com.badlogic.blackjack;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

public class PlayerUI {
    Label playerName;
    Label playerScore;
    Label playerBalance;

    PlayerUI(String name, int score, int balance, Skin skin) {
        playerName = new Label(name, skin);
        playerScore = new Label(Integer.toString(score), skin);
        playerBalance = new Label(Integer.toString(balance), skin);

        playerName.setFontScale(0.4f);

        playerScore.setFontScale(0.4f);
        playerScore.setColor(255/255f, 230/255f, 156/255f, 1);

        playerBalance.setFontScale(0.4f);
        playerBalance.setColor(Color.YELLOW);
    }

    public void updateColorName(Color c) {
        playerName.setColor(c);
    }

    public void updateScore(int score) {
        playerScore.setText(Integer.toString(score));
    }

    public void updateBalance(int balance) {
        playerBalance.setText(Integer.toString(balance));
    }
}
