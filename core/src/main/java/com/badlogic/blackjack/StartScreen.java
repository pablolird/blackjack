package com.badlogic.blackjack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class StartScreen implements Screen {

    private Stage stage;
    private Skin skin; // Assumes you have a skin in assets

    public StartScreen(final Main game) {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        try {
            skin = new Skin(Gdx.files.internal("skin/x1/uiskin.json"));
        } catch (Exception e) {
            Gdx.app.error("StartScreen", "Could not load skin", e);
            // Create a programmatic skin as a fallback
            skin = new Skin();
        }


        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        TextButton startButton = new TextButton("Start Game", skin);

        SelectBox players = new SelectBox(skin);
        Label txt = new Label("Number of players:", skin);

        players.setItems(1,2,3,4,5,6,7);

        table.add(txt).padRight(10);
        table.add(players);
        table.row();
        table.add(startButton).padTop(30).fill().colspan(2);

        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // This is the screen transition!
                game.setScreen(new GameScreen(game, (int)players.getSelected()));
                dispose(); // Dispose this screen
            }
        });
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.4f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        stage.dispose();
        // Dispose the skin if we created it here
        // If it's from Assets, do NOT dispose it.
        skin.dispose();
    }
}
