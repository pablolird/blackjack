package com.badlogic.blackjack.GameUI;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class PlayerWindow extends WindowMenu {
    Vector2 g_pos;
    Vector2 g_shift;

    public PlayerWindow(String title, Skin skin, Stage s, Vector2 g_pos, Vector2 g_shift) {
        super(title, skin, s);
        this.g_pos = g_pos;
        this.g_shift = g_shift;

        this.isVisible = true;
        this.window.getTitleLabel().setFontScale(0.4f);
        this.window.getCell(t).pad(0).padTop(20f);
        this.window.setVisible(true);
        this.window.setZIndex(0);
    }

    @Override
    public void pack() {
        window.pack();
        Vector2 pos = recalculateCenter(); // Calls PlayerWindow's logic
        window.setPosition(pos.x, pos.y);
    }

    public Vector2 recalculateCenter() {
        return new Vector2(g_pos.x - (window.getWidth() / 2f) + g_shift.x, g_pos.y - (window.getHeight() / 2f) + g_shift.y);
    }

    public void add(Image i) {
        t.add(i).padRight(10f).padBottom(0).width(30f).height(30f);
    }

    public void add(Label b) {
        t.add(b).padRight(10f).padBottom(0f);
        pack();
    }
}
