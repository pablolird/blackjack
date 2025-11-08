package com.badlogic.blackjack;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
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

    // --- NEW NETWORK CONSTANTS ---
    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54777;
    // --- END NEW NETWORK CONSTANTS ---

    // These will be shared by all screens
    public SpriteBatch spriteBatch;
    public Assets assets;

    // --- MODIFIED: Network Fields ---
    public GameClient gameClient;
    public GameServer gameServer; // NEW FIELD: Store the server instance
    // --- END MODIFIED ---
    
    // Exit match callback interface
    public interface ExitMatchCallback {
        void onExitMatch();
    }
    public ExitMatchCallback exitMatchCallback;

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

    // REMOVED: public void setScreen(GameScreen s, Object c) { ... }
    // The Game class already has a setScreen(Screen screen) method.

    @Override
    public void dispose() {
        // Dispose of shared assets and batch
        if (spriteBatch != null) spriteBatch.dispose();

        // --- DISPOSE NETWORK COMPONENTS ---
        if (gameClient != null) {
            gameClient.dispose();
        }
        if (gameServer != null) { // NEW: Dispose the server
            gameServer.dispose();
        }
        // --- END DISPOSE NETWORK COMPONENTS ---

        super.dispose();
    }
}
