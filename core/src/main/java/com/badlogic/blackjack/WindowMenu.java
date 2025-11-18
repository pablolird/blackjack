package com.badlogic.blackjack;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;

public class WindowMenu {
    Window window;
    Vector2 stage;
    boolean isVisible;
    Table t;

    private Vector2 recalculateCenter() {
        return  new Vector2(stage.x / 2f - window.getWidth() / 2, stage.y / 2 - window.getHeight() / 2);
    }

    public WindowMenu(String title, Skin skin, Stage s) {
        window = new Window(title, skin);
        stage = new Vector2(s.getWidth(), s.getHeight());

        window.getTitleTable().padTop(30f);
        window.setZIndex(20);
        window.pad(20);

        isVisible = false;
        window.setVisible(isVisible); // Hide it by default
        window.setMovable(false);
        window.setResizable(false);

        t = new Table();
        window.add(t).pad(20f).padTop(40f).padBottom(0);
        window.getTitleLabel().setFontScale(0.5f);
    }

    public void setVisible(boolean b) {
        isVisible = b;
        window.setVisible(isVisible);
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void pack() {
        window.pack();
        Vector2 pos = recalculateCenter();
        window.setPosition(pos.x,pos.y);
    }

    public void add(TextButton b) {
        t.add(b).padBottom(10f);
        t.row();
        pack();
    }

    public void add(Label b) {
        t.add(b).padBottom(10f);
        t.row();
        pack();
    }
}
