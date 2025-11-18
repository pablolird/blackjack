package com.badlogic.blackjack;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Timer {
    float paddingTop = 8f;
    Vector2 stage;
    Label timerLabel;
    float currentTime;
    int currentTimeInt;
    int timerConstant;
    boolean isLocal;


    public Timer(Stage s, Skin skin, float constant, boolean localGame) {
        isLocal = localGame;
        stage = new Vector2(s.getWidth(), s.getHeight());
        timerLabel = new Label("00:00", skin);
        currentTimeInt = 0;
        currentTime = 0f;
        timerLabel.setFontScale(0.8f);
        this.hide();
        timerConstant = (int) constant;

        updateTextPosition();
        s.addActor(timerLabel);
    }

    private void updateTextPosition() {
        float pos_x = (stage.x-(timerLabel.getWidth()*timerLabel.getFontScaleX()))/2f;
        float pos_y = (stage.y-(timerLabel.getHeight()*timerLabel.getFontScaleY()))-paddingTop;
        timerLabel.setPosition(pos_x,pos_y);
    }

    public void updateText() {
        int remaining = (timerConstant-(currentTimeInt));
        // Clamp at 0 to prevent negative numbers
        remaining = Math.max(0, remaining);
        String newText = "00:"+((remaining>9)?(remaining):("0"+remaining));
        timerLabel.setText(newText);
    }

    public void hide() {
        timerLabel.setVisible(false);
    }

    public void show() {
        timerLabel.setVisible(true);
    }

    public void reset() {
        currentTime = 0;
        currentTimeInt = 0;
        updateText();
        timerLabel.setVisible(true);
    }

    public void update(float delta) {
        currentTime+=delta;
        if (((int) currentTime)> currentTimeInt) {
            currentTimeInt = (int) currentTime;
            updateText();
        }
    }
}
