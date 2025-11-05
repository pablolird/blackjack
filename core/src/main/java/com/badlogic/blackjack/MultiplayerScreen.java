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

public class MultiplayerScreen implements Screen
{
    private Stage stage;
    private Skin skin;

    private final SpriteBatch spriteBatch;
    private final Assets assets;

    private final boolean SetTiledBG = true;

    public MultiplayerScreen(final Main game)
    {
        this.spriteBatch = game.spriteBatch;
        this.assets = game.assets;

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        try
        {
            skin = new Skin(Gdx.files.internal("skin/x1/uiskin.json"));
        } catch (Exception e)
        {
            Gdx.app.error("MultiplayerScreen", "Could not load skin", e);
            skin = new Skin();
        }

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // JOIN
        TextButton joinGameButton = new TextButton("Join Game", skin);
        table.add(joinGameButton).pad(10).fillX();
        table.row();

        // HOST
        TextButton HostGameButton = new TextButton("Host Game", skin);
        table.add(HostGameButton).pad(10).fillX();
        table.row();

        joinGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {

            }
        });

        HostGameButton.addListener(new ChangeListener()
        {
           @Override
           public void changed(ChangeEvent event, Actor actor)
           {

           }
        });
    }
    @Override
    public void show()
    {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta)
    {
        Gdx.gl.glClearColor(0.4f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (SetTiledBG) {
            // --- DRAW REPEATING BACKGROUND ---

            // Set the batch to use the stage's camera
            spriteBatch.setProjectionMatrix(stage.getViewport().getCamera().combined);

            spriteBatch.begin();

            Texture backgroundTexture = assets.menuBackground;

            // Create a TextureRegion and set its u2/v2 coordinates to tile
            TextureRegion backgroundRegion = new TextureRegion(backgroundTexture);

            float screenWidth = stage.getWidth();
            float screenHeight = stage.getHeight();

            // Calculate texture coordinates for tiling (assuming 64x64 tile size)
            final float TILE_SIZE = 64f;
            float u2 = screenWidth / TILE_SIZE;
            float v2 = screenHeight / TILE_SIZE;

            backgroundRegion.setU2(u2);
            backgroundRegion.setV2(v2);

            // Draw the tiled region to cover the entire viewport
            spriteBatch.draw(backgroundRegion, 0, 0, screenWidth, screenHeight);

            spriteBatch.end();
        }

        // --- DRAW UI STAGE ---
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
        // Dispose the skin since this screen created it.
        skin.dispose();
    }

}


