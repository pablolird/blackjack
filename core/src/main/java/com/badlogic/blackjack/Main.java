package com.badlogic.blackjack;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Main game class that manages screens.
 * Changed from ApplicationListener to extend Game.
 */
public class Main extends Game {
    public static final int WORLD_WIDTH = 960;
    public static final int WORLD_HEIGHT = 540;

    // These will be shared by all screens
    public SpriteBatch spriteBatch;
    public Assets assets;

    // The old 'private BlackjackGame game;' field has been removed.

    @Override
    public void create() {
        // Create shared objects
        spriteBatch = new SpriteBatch();
        assets = new Assets();
        assets.loadFromFile();

        // Set the very first screen to be shown
        // The Game superclass will store this in its 'screen' field
        this.setScreen(new StartScreen(this));
    }

    @Override
    public void render() {
        // This will automatically call the render() method
        // of the currently active screen
        super.render();
    }

    @Override
    public void dispose() {
        // Dispose of shared assets and batch
        if (spriteBatch != null) spriteBatch.dispose();

        super.dispose();
    }

    //
    // The old resize(), pause(), and resume() methods have been REMOVED.
    // The 'Game' superclass now handles delegating these calls to the active screen.
    //
}
