package com.badlogic.blackjack.ui;
import com.badlogic.blackjack.GameScreen;
import com.badlogic.blackjack.Main;
import com.badlogic.blackjack.StartScreen;
import com.badlogic.blackjack.audio.AudioManager;
import com.badlogic.blackjack.audio.SoundType;
import com.badlogic.blackjack.game.BlackjackLogic;
import com.badlogic.blackjack.game.Dealer;
import com.badlogic.blackjack.game.Get;
import com.badlogic.blackjack.game.Player;
import com.badlogic.blackjack.ui.components.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.blackjack.network.GameClient;
import com.badlogic.blackjack.network.NetworkPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class UI {
    private Stage stage;
    private Skin skin;
    private Skin startGameSkin;
    private Timer timer;
    private Table actionTable;
    private WindowMenu pauseMenu;
    private WindowMenu gameOverMenu;
    private WindowMenu gameOverWaitingMenu;
    private WindowMenu gameOverLocalMenu;
    private BlackjackLogic blackjackLogic;
    private boolean paused;
    private AudioManager audioManager;
    private Get g;
    private Main game;
    private GameClient gameClient;
    Player currentPlayer;
    private int initialPlayerCount;

    private ArrayList<GameButton> bettingButtons;
    private ArrayList<GameButton> mainButtons;

    private HashMap<Integer, PlayerUI> playerUI;
    private HashMap<Integer, Integer> playerIdToVisualPosition; // Maps Player ID -> visual position index (1-based)
    private Label dealerScoreLabel;
    private Window dealerWindow;

    private Drawable coinDrawable;
    private Drawable clubDrawable;

    Color HEX(String value) {
        // Remove '#' if present
        if (value.startsWith("#")) {
            value = value.substring(1);
        }

        // Parse hex and create Color
        long colorLong = Long.parseLong(value, 16);

        // Handle shorthand (RGB) like "FFF"
        if (value.length() == 3) {
            int r = Integer.parseInt(value.charAt(0) + value.substring(0, 1), 16);
            int g = Integer.parseInt(value.charAt(1) + value.substring(1, 2), 16);
            int b = Integer.parseInt(value.charAt(2) + value.substring(2, 3), 16);
            return new Color(r / 255f, g / 255f, b / 255f, 1f);
        }

        // Normal 6-digit RGB or 8-digit RGBA hex
        if (value.length() == 6) {
            return new Color(
                ((colorLong >> 16) & 0xFF) / 255f,
                ((colorLong >> 8) & 0xFF) / 255f,
                (colorLong & 0xFF) / 255f,
                1f
            );
        } else if (value.length() == 8) {
            return new Color(
                ((colorLong >> 24) & 0xFF) / 255f,
                ((colorLong >> 16) & 0xFF) / 255f,
                ((colorLong >> 8) & 0xFF) / 255f,
                (colorLong & 0xFF) / 255f
            );
        } else {
            throw new IllegalArgumentException("Invalid hex color format: " + value);
        }
    }

    public void hideTimer() {
        this.timer.hide();
    }

    public void showTimer() {
        this.timer.show();
    }

    public void resetTimer() {
        this.timer.reset();
    }

    public void updateTimer(float delta) {
        this.timer.update(delta);
    }

    public void initSkin(String path) {
        // Needed to add a freetype font included in a json skin for some reason
        this.skin = new Skin(Gdx.files.internal(path)) {
            //Override json loader to process FreeType fonts from skin JSON
            @Override
            protected Json getJsonLoader(final FileHandle skinFile) {
                Json json = super.getJsonLoader(skinFile);
                final Skin skin = this;

                json.setSerializer(FreeTypeFontGenerator.class, new Json.ReadOnlySerializer<FreeTypeFontGenerator>() {
                    @Override
                    public FreeTypeFontGenerator read(Json json,
                                                      JsonValue jsonData, Class type) {
                        String path = json.readValue("font", String.class, jsonData);
                        jsonData.remove("font");

                        FreeTypeFontGenerator.Hinting hinting = FreeTypeFontGenerator.Hinting.valueOf(json.readValue("hinting",
                            String.class, "AutoMedium", jsonData));
                        jsonData.remove("hinting");

                        Texture.TextureFilter minFilter = Texture.TextureFilter.valueOf(
                            json.readValue("minFilter", String.class, "Nearest", jsonData));
                        jsonData.remove("minFilter");

                        Texture.TextureFilter magFilter = Texture.TextureFilter.valueOf(
                            json.readValue("magFilter", String.class, "Nearest", jsonData));
                        jsonData.remove("magFilter");

                        FreeTypeFontParameter parameter = json.readValue(FreeTypeFontParameter.class, jsonData);
                        parameter.hinting = hinting;
                        parameter.minFilter = minFilter;
                        parameter.magFilter = magFilter;
                        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(skinFile.parent().child(path));
                        BitmapFont font = generator.generateFont(parameter);
                        skin.add(jsonData.name, font);
                        if (parameter.incremental) {
                            generator.dispose();
                            return null;
                        } else {
                            return generator;
                        }
                    }
                });

                return json;
            }
        };
    }

    public void init(BlackjackLogic bl, AudioManager audioManager, Main game, Viewport vp, SpriteBatch sb, String skin_path) {
        this.blackjackLogic = bl;
        this.startGameSkin = game.assets.skin;
        this.playerUI = new HashMap<>();
        this.playerIdToVisualPosition = new HashMap<>();
        this.game = game;
        this.g = new Get();
        this.paused = false;
        this.audioManager = audioManager;
        this.stage = new Stage(vp, sb);
        this.gameClient = game.gameClient;
        this.initialPlayerCount = blackjackLogic.getPlayersList().size();

        this.bettingButtons = new ArrayList<>();
        this.mainButtons = new ArrayList<>();

        Gdx.input.setInputProcessor(stage);
        initSkin(skin_path);
        this.timer = new Timer(stage, skin, BlackjackLogic.PLAYER_ACTION_TIMEOUT, bl.isLocal());
    }

    public void buildActionTable() {
        actionTable = new Table();
        actionTable.setFillParent(true);
        actionTable.bottom(); // Anchor to the bottom
        stage.addActor(actionTable);

        // Create 3 main action buttons (hit, stand, lock bet)
        GameButton hitButton = new GameButton(skin," HIT ", HEX("#D91C1C"));
        GameButton standButton = new GameButton(skin," STAND ", HEX("#2924AB"));

        // Add main buttons to array to disable them in group when needed
        mainButtons.add(hitButton);
        mainButtons.add(standButton);

        // Add betting buttons to array to disable them in group too
        GameButton confirmBetButton = new GameButton(skin," Confirm Bet ", HEX("#e37622"));
        bettingButtons.add(confirmBetButton);

        // Event listeners
        hitButton.button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null) {
                    gameClient.sendAction(NetworkPacket.PlayerActionType.HIT);
                } else {
                    // Local game - call logic directly
                    blackjackLogic.hit();
                }
            }
        });

        standButton.button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null) {
                    gameClient.sendAction(NetworkPacket.PlayerActionType.STAND);
                } else {
                    // Local game - call logic directly
                    blackjackLogic.stand();
                }
                audioManager.playSound(SoundType.STAND, 1.0f);
            }
        });

        confirmBetButton.button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null) {
                    gameClient.sendAction(NetworkPacket.PlayerActionType.LOCK_IN_BET);
                } else {
                    // Local game - call logic directly
                    blackjackLogic.playerLockInBet();
                }
                audioManager.playSound(SoundType.LOCKBET, 0.65f);
            }
        });

        // Create betting buttons (10,50,100)
        int[] amounts = {10,50,100};
        String[] hexs = {"#43b32d","#96b32d","#c2ba21"};

        for (int i = 0; i < amounts.length; i++) {
            GameButton t = new GameButton(skin," Bet "+amounts[i]+" ", HEX(hexs[i]));
            addBetButton(t,amounts[i]);
            bettingButtons.add(t);
            actionTable.add(t.button).pad(10);
        }

        // Then add main action buttons
        actionTable.add(confirmBetButton.button).pad(10);
        actionTable.add(hitButton.button).pad(10);
        actionTable.add(standButton.button).pad(10);
    }

    private void addBetButton(GameButton b, int amount) {
        b.button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null) {
                    gameClient.sendAction(NetworkPacket.PlayerActionType.ADD_TO_BET, amount);
                } else {
                    blackjackLogic.playerAddToBet(amount);
                }
                audioManager.playSound(SoundType.BET, 1.0f);
            }
        });
    }

    private void buildPauseMenu() {
        TextButton exitMatchButton = new TextButton("Exit Match", startGameSkin);
        // Exit Match button listener - will be set by GameScreen
        exitMatchButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null && game.exitMatchCallback != null) {
                    game.exitMatchCallback.onExitMatch();
                } else {
                    // Local game, just return to start screen
                    game.setScreen(new StartScreen(game));
                    dispose(); // Dispose this screen
                }
            }
        });

        pauseMenu = new WindowMenu("Pause",skin,stage);
        pauseMenu.add(exitMatchButton);
        stage.addActor(pauseMenu.window);
    }

    private void buildGameOverMenu() {
        // 4. Build the Game Over Menu for Host (with restart and exit options)
        gameOverMenu = new WindowMenu("Game Over", skin, stage);
        TextButton restartMatchButton = new TextButton("Restart Match", startGameSkin);
        gameOverMenu.add(restartMatchButton);
        TextButton exitButton = new TextButton("Exit", startGameSkin);
        gameOverMenu.add(exitButton);
        stage.addActor(gameOverMenu.window);

        // 5. Build the Game Over Waiting Menu for Non-Host players
        gameOverWaitingMenu = new WindowMenu("Game Over",skin,stage);
        Label waitingLabel = new Label("Waiting for host...", startGameSkin);
        gameOverWaitingMenu.add(waitingLabel);
        stage.addActor(gameOverWaitingMenu.window);

        // 6. Build the Game Over Menu for Local Games (exit only, no restart)
        gameOverLocalMenu = new WindowMenu("Game Over",skin,stage);
        TextButton restartLocalButton = new TextButton("Restart", startGameSkin);
        TextButton exitLocalButton = new TextButton("Exit", startGameSkin);
        gameOverLocalMenu.add(restartLocalButton);
        gameOverLocalMenu.add(exitLocalButton);
        stage.addActor(gameOverLocalMenu.window);

        exitLocalButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Local game - just return to start screen
                game.setScreen(new StartScreen(game));
                dispose();
            }
        });

        restartLocalButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game, initialPlayerCount));
                dispose();
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null && game.exitMatchCallback != null) {
                    game.exitMatchCallback.onExitMatch();
                } else {
                    game.setScreen(new StartScreen(game));
                    dispose();
                }
            }
        });

        restartMatchButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // For local games, handle restart directly without callbacks
                if (gameClient == null) {
                    // Local game - restart directly
                    Gdx.app.log("UI", "Restarting local game");
                    game.setScreen(new GameScreen(game, initialPlayerCount));
                    dispose(); // Dispose this screen
                } else if (game.restartMatchCallback != null) {
                    // Multiplayer - use callback
                    Gdx.app.log("UI", "Restart match button clicked - using callback");
                    game.restartMatchCallback.onRestartMatch();
                } else {
                    Gdx.app.log("UI", "No callback available for restart");
                }
            }
        });
    }

    public UI(Viewport vp, SpriteBatch sb, BlackjackLogic bl, AudioManager audioManager, Main game) {

        init(bl, audioManager, game, vp, sb, "SKIN_JSON2/skin.json");
        buildActionTable();
        // This should have a conditional rendering and logic depending on whether user is host or client
        buildPauseMenu();
        buildGameOverMenu();

        // Check later where to put this

        // =========================
        Texture coinTex = new Texture(Gdx.files.internal("coin1.png"));
        Texture clubTex = new Texture(Gdx.files.internal("spade.png"));

        // Wrap them in Drawables (needed for UI widgets)
        coinDrawable = new TextureRegionDrawable(new TextureRegion(coinTex));
        clubDrawable = new TextureRegionDrawable(new TextureRegion(clubTex));
        // =========================
        buildLayout(bl.playersList);

    }

    // --- BUILD LAYOUT ---
    public void buildLayout(List<Player> players) {
        // DEALER
        PlayerWindow w = new PlayerWindow("DEALER",
            skin,
            stage,
            g.position.get("DEALER_CARD"),
            g.scoreShift.get("DEALER"));

        dealerWindow = w.window;
//        w.setSize(90,90);
        dealerScoreLabel = new Label("0", skin);
        dealerScoreLabel.setFontScale(0.4f);
        dealerScoreLabel.setColor(255/255f, 230/255f, 156/255f, 1);
        Vector2 dealerShift = g.scoreShift.get("DEALER");
        Image m = new Image(clubDrawable);
        m.setScaling(Scaling.fit);
        w.add(m);
        w.add(dealerScoreLabel);
        stage.addActor(w.window);

        // PLAYERS
        for (int i = 0; i < players.size(); i++) {
            String playerKey = "PLAYER" + (i + 1) + "_CARD";
            Player p = players.get(i);

            PlayerWindow w2 = new PlayerWindow("PLAYER" + (i + 1),
                skin,
                stage,
                g.position.get(playerKey),
                g.scoreShift.get("PLAYER" + (i + 1)));

            PlayerUI pUI = new PlayerUI(w2.window, w2.window.getTitleLabel(), p.totalValue(), p.getBalance(), skin);

            Image m3 = new Image(coinDrawable);
            m3.setScaling(Scaling.fit);
            w2.add(m3);
            w2.add(pUI.playerBalance);
            Image m2 = new Image(clubDrawable);
            m2.setScaling(Scaling.fit);
            w2.add(m2);
            w2.add(pUI.playerScore);
            stage.addActor(w2.window);

            // Store UI in map for later access
            playerUI.put(p.getID(), pUI);
            // Store visual position index (1-based) for card dealing
            playerIdToVisualPosition.put(p.getID(), i + 1);
        }
    }

    // --- REBUILD LAYOUT ---
    public void rebuildLayout(List<Player> players, Dealer d) {
        // Note: This method doesn't rebuild windows, just updates scores/balances.
        // Visual positions remain the same as originally assigned in buildLayout.
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            // Get corresponding player UI
            PlayerUI pUI = playerUI.get(p.getID());

            if (pUI != null) {
                pUI.updateBalance(p.getBalance());
                pUI.updateScore(p.totalValue());
                // Visual position mapping is preserved from buildLayout - don't reassign
            }
        }

        updateDealerScore(d);

        // Reset current player highlight
        currentPlayer = null;
    }

    public void updatePlayerScore(Player p) {
        playerUI.get(p.getID()).updateScore(p.totalValue());
    }

    public void updateDealerScore(Dealer dealer) {
        if (dealerScoreLabel != null) {
            dealerScoreLabel.setText(dealer.totalValue());
        }
    }

    public void updatePlayerBalance(Player p) {
        playerUI.get(p.getID()).updateBalance(p.getBalance());
    }

    /**
     * Gets the visual position index (1-based) for a player.
     * This is used to determine which PLAYER position slot the player occupies on screen.
     * @param p The player
     * @return Visual position index (1-7), or -1 if player not found
     */
    public int getVisualPositionIndex(Player p) {
        Integer visualPos = playerIdToVisualPosition.get(p.getID());
        return visualPos != null ? visualPos : -1;
    }

    public void update(float delta) {
        stage.act(delta);
    }
    public void render() {
        stage.draw();
    }

    private void resetPlayerWindows(Window.WindowStyle defaultStyle) {
        for (PlayerUI pUI : playerUI.values()) {
            pUI.playerWindow.setStyle(defaultStyle);
            pUI.playerName.setColor(Color.WHITE);
        }
    }

    private void setDealerStyle(Window.WindowStyle style) {
        if (dealerWindow != null) {
            dealerWindow.setStyle(style);
        }
    }

    public void focusDealerOnly() {
        Window.WindowStyle defaultStyle = skin.get("default", Window.WindowStyle.class);
        Window.WindowStyle focusedStyle = skin.get("focused", Window.WindowStyle.class);

        resetPlayerWindows(defaultStyle);
        setDealerStyle(focusedStyle);
        currentPlayer = null;
    }

    public void resetDealerFocus() {
        Window.WindowStyle defaultStyle = skin.get("default", Window.WindowStyle.class);
        setDealerStyle(defaultStyle);
    }

    public void updateCurrentPlayerColor(Player p) {
        if (currentPlayer != null) {
            PlayerUI prev = playerUI.get(p.getID());
            if (prev != null) {
                prev.playerName.setColor(HEX("#808080"));
            }
        }
        setCurrentPlayer(p);
    }

    public void setCurrentPlayer(Player p) {
        Window.WindowStyle defaultStyle = skin.get("default", Window.WindowStyle.class);
        Window.WindowStyle focusedStyle = skin.get("focused", Window.WindowStyle.class);

        resetPlayerWindows(defaultStyle);
        setDealerStyle(defaultStyle);

        currentPlayer = p;
        if (currentPlayer != null) {
            PlayerUI current = playerUI.get(p.getID());
            if (current != null) {
                current.playerWindow.setStyle(focusedStyle);
            }
        }
    }


    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }

    public void showPlayerActionPanel(boolean visible) {
        for (int i = 0; i < mainButtons.toArray().length; i++) {
            mainButtons.get(i).toggleDisable(!visible);
        }
    }

    public void showBettingPanel(boolean visible) {
        for (int i = 0; i < bettingButtons.toArray().length; i++) {
            bettingButtons.get(i).toggleDisable(!visible);
        }
    }

    public boolean ActionPanelIsVisible() {
        // Not the most rock solid condition but it gets the job done for now I guess
        return !mainButtons.get(0).isDisabled();
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void togglePauseMenu() {
        paused = !paused;
        pauseMenu.setVisible(paused);
    }

    public void showGameOverMenu(boolean isHost) {
        // Check if this is a local game
        boolean isLocalGame = (gameClient == null);

        if (isLocalGame) {
            // Local game - show menu with exit only
            gameOverLocalMenu.setVisible(true);
            gameOverMenu.setVisible(false);
            gameOverWaitingMenu.setVisible(false);
        } else if (isHost) {
            // Multiplayer host - show menu with restart and exit
            gameOverMenu.setVisible(true);
            gameOverLocalMenu.setVisible(false);
            gameOverWaitingMenu.setVisible(false);
        } else {
            // Multiplayer non-host - show waiting menu
            gameOverMenu.setVisible(false);
            gameOverLocalMenu.setVisible(false);
            gameOverWaitingMenu.setVisible(true);
        }
    }

    public boolean isGameOverMenuVisible() {
        return gameOverMenu.isVisible() || gameOverWaitingMenu.isVisible() || gameOverLocalMenu.isVisible();
    }
}
