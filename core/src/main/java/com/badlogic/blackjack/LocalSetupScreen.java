package com.badlogic.blackjack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class LocalSetupScreen implements Screen {

    private Stage stage;
    private Skin skin;

    private final SpriteBatch spriteBatch;
    private final Assets assets;

    // Toggle for background style
    private final boolean SetTiledBG = false; // Set this to true for tiled, false for solid color

    public LocalSetupScreen(final Main game) {
        this.spriteBatch = game.spriteBatch; // Get shared SpriteBatch
        this.assets = game.assets; // Get shared Assets

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

        SelectBox<Integer> players = new SelectBox<Integer>(skin);
        Label txt = new Label("Number of players:", skin);

        players.setItems(1,2,3,4,5,6,7);

        table.add(txt).padRight(10);
        table.add(players);
        table.row();
        table.add(startButton).padTop(30).fill().colspan(2);

        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Screen Transition
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

        if (SetTiledBG) {
            // --- DRAW REPEATING BACKGROUND ---

            spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);

            spriteBatch.begin();

            Texture backgroundTexture = assets.menuBackground;

            // Create a TextureRegion to define the tiling area
            TextureRegion backgroundRegion = new TextureRegion(backgroundTexture);

            float screenWidth = stage.getWidth();
            float screenHeight = stage.getHeight();

            final float TILE_SIZE = 64f;
            float u2 = screenWidth / TILE_SIZE;
            float v2 = screenHeight / TILE_SIZE;

            backgroundRegion.setU2(u2);
            backgroundRegion.setV2(v2);

            spriteBatch.draw(backgroundRegion, 0, 0, screenWidth, screenHeight);

            spriteBatch.end();
        }

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
