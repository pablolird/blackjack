package com.badlogic.blackjack;

import com.badlogic.blackjack.assets.Assets;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.blackjack.network.GameClient;
import com.badlogic.blackjack.network.GameServer; // NEW IMPORT

/**
 * Main game class that manages screens.
 * Changed from ApplicationListener to extend Game.
 */
public class Main extends Game {
    public static final int WORLD_WIDTH = 960;
    public static final int WORLD_HEIGHT = 540;

    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54777;

    public SpriteBatch spriteBatch;
    public Assets assets;

    public GameClient gameClient;
    public GameServer gameServer;

    public interface ExitMatchCallback
    {
        void onExitMatch();
    }
    public ExitMatchCallback exitMatchCallback;

    public interface RestartMatchCallback
    {
        void onRestartMatch();
    }
    public RestartMatchCallback restartMatchCallback;

    @Override
    public void create()
    {
        // Create shared objects
        spriteBatch = new SpriteBatch();
        assets = new Assets();
        assets.loadFromFile();

        // Set the very first screen to be shown
        this.setScreen(new StartScreen(this));
    }

    @Override
    public void render() {
        super.render();
    }


    @Override
    public void dispose() {
        // Dispose of shared assets and batch
        if (spriteBatch != null) spriteBatch.dispose();

        // Dispose network components
        if (gameClient != null) {
            gameClient.dispose();
        }
        if (gameServer != null) {
            gameServer.dispose();
        }

        super.dispose();
    }
}
