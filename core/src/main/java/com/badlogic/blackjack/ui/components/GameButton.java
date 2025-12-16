package com.badlogic.blackjack.ui.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

// Game button, extends from TextButton
public class GameButton {
    public TextButton button;
    Color color;
    Color disabledColor = Color.GRAY;
    boolean isDisabled;

    public boolean isDisabled() {
        return isDisabled;
    }

    // Constructor
    public GameButton(Skin skin, String text, Color c) {
        isDisabled = true;
        button = new TextButton(text, skin);
        button.setDisabled(isDisabled);
        button.getLabel().setFontScale(0.5f);
        color = c;
        button.setColor(disabledColor);
    }

    public void toggleDisable() {
        isDisabled = !isDisabled;
        if (isDisabled) {
//            button.getLabel().setColor(Color.GRAY);
            button.setColor(Color.GRAY);
            button.setDisabled(true);
        }
        else {
//            button.getLabel().setColor(Color.GRAY);
            button.setColor(disabledColor);
            button.setDisabled(false);
        }
    }

    public void toggleDisable(boolean b) {
        if (b) {
            button.setColor(disabledColor);
            button.setDisabled(true);
        }
        else {
            button.setColor(color);
            button.setDisabled(false);
        }
    }
}
