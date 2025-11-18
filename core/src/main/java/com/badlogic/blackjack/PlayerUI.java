package com.badlogic.blackjack;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;

public class PlayerUI {
    Window playerWindow;
    Label playerName;
    Label playerScore;
    Label playerBalance;

    PlayerUI(Window pW, Label playerName, int score, int balance, Skin skin) {
        this.playerWindow = pW;
        this.playerName = playerName;
        playerScore = new Label(Integer.toString(score), skin);
        playerBalance = new Label(Integer.toString(balance), skin);

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
