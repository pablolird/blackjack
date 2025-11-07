package com.badlogic.blackjack;

import com.badlogic.blackjack.audio.AudioManager;
import com.badlogic.blackjack.audio.SoundType;
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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UI {
    private final Stage stage;
    private final Skin skin;
    private final Table playerActionTable;
    private final Table bettingActionTable;
    private final Window pauseMenu;
    private final Window gameOverMenu;
    private final Map<Player, Window> playerWindows = new HashMap<>();
    private Window dealerWindow;
    private Label dealerScoreLabel;

    private final BlackjackLogic blackjackLogic;
    private boolean paused;
    private AudioManager audioManager;
    private final Get g;
    private Main game;
    private SpriteBatch spriteBatch;
    Player currentPlayer;
    private Image coinDrawable;
    private Image spadeDrawable;

    TextButton createButton(String text, Color buttonColor, Color labelColor) {
        TextButton b = new TextButton(text, skin);
        b.getLabel().setFontScale(0.8f);
        b.setColor(buttonColor);
        b.getLabel().setColor(labelColor);

        return b;
    }

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

    TextButton createButton(String text, Color buttonColor) {
        TextButton b = new TextButton(text, skin);
        b.getLabel().setFontScale(0.5f);
        b.setColor(buttonColor);
        return b;
    }

    public UI(Viewport vp, SpriteBatch sb, BlackjackLogic bl, AudioManager audioManager, Main game) {
        this.blackjackLogic = bl;
        this.game = game;
        this.g = new Get();
        this.paused = false;
        this.audioManager = audioManager;

        Texture coinTex = new Texture(Gdx.files.internal("coin1.png"));
        Texture clubTex = new Texture(Gdx.files.internal("spade.png"));

        // Wrap them in Drawables (needed for UI widgets)
        Drawable coinDrawable = new TextureRegionDrawable(new TextureRegion(coinTex));
        Drawable spadeDrawable = new TextureRegionDrawable(new TextureRegion(clubTex));

        // Create Image actors
        Image coinImage = new Image(coinDrawable);
        Image clubImage = new Image(spadeDrawable);

        // Needed to add a freetype font included in a json skin for some reason
        skin = new Skin(Gdx.files.internal("SKIN_JSON2/skin.json")) {
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

        stage = new Stage(vp, sb);

        Gdx.input.setInputProcessor(stage);

        // 1. Build the Player Action Panel (initially hidden)
        playerActionTable = new Table();
        playerActionTable.setFillParent(true);
        playerActionTable.bottom(); // Anchor to the bottom
        stage.addActor(playerActionTable);

        TextButton hitButton = createButton(" HIT ", HEX("#821010"));
        TextButton standButton = createButton(" STAND ", HEX("#100d4f"));

        hitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                blackjackLogic.hit();
            }
        });

        standButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                audioManager.playSound(SoundType.STAND, 1.0f);
                blackjackLogic.stand();
            }
        });

        playerActionTable.add(hitButton).pad(10);
        playerActionTable.add(standButton).pad(10);
        playerActionTable.setVisible(false); // Hide it by default

        // 2. Build the Betting Action Panel
        bettingActionTable = new Table();
        bettingActionTable.setFillParent(true);
        bettingActionTable.bottom();
        stage.addActor(bettingActionTable);

        TextButton bet10Button = createButton(" Bet 10 ", HEX("#43b32d"));
        TextButton bet50Button = createButton(" Bet 50 ", HEX("#96b32d"));
        TextButton bet100Button = createButton(" Bet 100 ", HEX("#c2ba21"));
        TextButton confirmBetButton = createButton(" Confirm Bet ", HEX("#e37622"));

        bet10Button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                blackjackLogic.playerAddToBet(10);
                audioManager.playSound(SoundType.BET, 1.0f);
            }
        });

        bet50Button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                blackjackLogic.playerAddToBet(50);
                audioManager.playSound(SoundType.BET, 1.0f);
            }
        });

        bet100Button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                blackjackLogic.playerAddToBet(100);
                audioManager.playSound(SoundType.BET, 1.0f);
            }
        });

        confirmBetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                blackjackLogic.playerLockInBet();
                audioManager.playSound(SoundType.LOCKBET, 0.65f);
            }
        });

        bettingActionTable.add(bet10Button).pad(10);
        bettingActionTable.add(bet50Button).pad(10);
        bettingActionTable.add(bet100Button).pad(10);
        bettingActionTable.add(confirmBetButton).pad(10);
        bettingActionTable.setVisible(false);

//         3. Build the Pause Menu (initially hidden)
        pauseMenu = new Window(" Pause ", skin);
        TextButton quitButton = createButton(" Quit Game ", HEX("#383645"));
        pauseMenu.add(quitButton);
        pauseMenu.pack(); // Size the window to its contents
        pauseMenu.setPosition(stage.getWidth() / 2 - pauseMenu.getWidth() / 2, stage.getHeight() / 2 - pauseMenu.getHeight() / 2);
        pauseMenu.getTitleLabel().setFontScale(0.5f);
        stage.addActor(pauseMenu);
        pauseMenu.setVisible(false); // Hide it by default

        buildLayout(bl.playersList);

        // 4. Build the Close Menu (initially hidden too)
        gameOverMenu = new Window(" Game Over ", skin);
        gameOverMenu.pad(20);
        gameOverMenu.padTop(50);
        gameOverMenu.add(quitButton);

        gameOverMenu.row();
        TextButton restartButton = createButton(" Restart Game ", HEX("#383645"));
        gameOverMenu.add(restartButton).expand();
        gameOverMenu.pack(); // Size the window to its contents
        gameOverMenu.setPosition(stage.getWidth() / 2 - pauseMenu.getWidth() / 2, stage.getHeight() / 2 - pauseMenu.getHeight() / 2);
        gameOverMenu.getTitleLabel().setFontScale(0.5f);
        stage.addActor(gameOverMenu);
        gameOverMenu.setVisible(false); // Hide it by default

        quitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new StartScreen(game));
                dispose(); // Dispose this screen
            }
        });

        restartButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game, bl.numPlayers));
                dispose(); // Dispose this screen
            }
        });
    }

    // --- REBUILD LAYOUT ---
    public void rebuildLayout(List<Player> players) {
        // Remove dealer window if it exists
        if (dealerWindow != null) {
            dealerWindow.remove();
            dealerWindow = null;
        }

        // Remove all player windows
        for (Window w : playerWindows.values()) {
            w.remove();
        }
        playerWindows.clear();

        // Reset current player highlight
        currentPlayer = null;

        // Rebuild everything
        buildLayout(players);
    }

    // --- BUILD LAYOUT (unchanged by your request) ---
    public void buildLayout(List<Player> players) {
        // DEALER
        Window w = new Window("DEALER", skin);
        w.getTitleLabel().setFontScale(0.3f);
        dealerScoreLabel = new Label("Dealer: 0", skin);
        dealerScoreLabel.setFontScale(0.5f);
        dealerScoreLabel.setAlignment(Align.left);
        dealerScoreLabel.setColor(255/255f, 230/255f, 156/255f, 1);
        Vector2 dealerPosition = g.position.get("DEALER_CARD");
        Vector2 dealerShift = g.scoreShift.get("DEALER");
        w.setPosition(dealerPosition.x - (w.getWidth() / 2f) + dealerShift.x,
            dealerPosition.y - (w.getHeight() / 2f) + dealerShift.y);
        w.add(dealerScoreLabel);
        stage.addActor(w);
        dealerWindow = w;

        // PLAYERS
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Label scoreLabel = new Label("" + p.totalValue(), skin);
            scoreLabel.setFontScale(0.5f);
            Window w2 = new Window(p.getName()+i, skin);
            w2.getTitleLabel().setFontScale(0.3f);
            scoreLabel.setFontScale(0.5f);
            scoreLabel.setAlignment(Align.left);
            scoreLabel.setColor(128/255f, 128/255f, 128/255f, 0.75f);

            Label balanceLabel = new Label("Balance: " + p.getBalance() + "c", skin);
            balanceLabel.setFontScale(0.5f);
            balanceLabel.setAlignment(Align.left);
            balanceLabel.setColor(Color.YELLOW);

            String playerKey = "PLAYER" + (i + 1) + "_CARD";
            Vector2 position = g.position.get(playerKey);

            if (position != null) {
                w2.setPosition(position.x - (w2.getWidth() / 2f) + g.scoreShift.get("PLAYER" + (i + 1)).x,
                    position.y - (w2.getHeight() / 2f) + g.scoreShift.get("PLAYER" + (i + 1)).y);
            }

            w2.add(scoreLabel);
            w2.row();
            w2.add(balanceLabel);
            stage.addActor(w2);

            // Store window in map for later access
            playerWindows.put(p, w2);
        }
    }

    public void updatePlayerScore(Player p) {
        Window w = playerWindows.get(p);
        if (w != null) {
            for (Actor a : w.getChildren()) {
                if (a instanceof Label) {
                    Label label = (Label) a;
                    if (label.getText().toString().startsWith("Score:")) {
                        label.setText("Score: " + p.totalValue());
                        break;
                    }
                }
            }
        }
    }

    public void updateDealerScore(Dealer dealer) {
        if (dealerScoreLabel != null) {
            dealerScoreLabel.setText("Dealer: " + dealer.totalValue());
        }
    }

    public void updatePlayerBalance(Player p) {
        Window w = playerWindows.get(p);
        if (w != null) {
            for (Actor a : w.getChildren()) {
                if (a instanceof Label) {
                    Label label = (Label) a;
                    if (label.getText().toString().startsWith("Balance:")) {
                        label.setText("Balance: " + p.getBalance() + "c");
                        break;
                    }
                }
            }
        }
    }

    public void update(float delta) {
        stage.act(delta);
    }
    public void render() {
        stage.draw();
    }

    public void updateCurrentPlayerColor(Player p) {
        if (currentPlayer != null) {
            Window prev = playerWindows.get(currentPlayer);
            if (prev != null) {
                for (Actor a : prev.getChildren()) {
                    if (a instanceof Label) {
                        Label label = (Label) a;
                        label.setColor(128 / 255f, 128 / 255f, 128 / 255f, 0.75f);
                    }
                }
            }
        }
        setCurrentPlayer(p);
    }

    public void setCurrentPlayer(Player p) {
        currentPlayer = p;
        if (currentPlayer != null) {
            Window w = playerWindows.get(currentPlayer);
            if (w != null) {
                for (Actor a : w.getChildren()) {
                    if (a instanceof Label) {
                        Label label = (Label) a;
                        label.setColor(Color.WHITE);
                    }
                }
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
        playerActionTable.setVisible(visible);
    }

    public void showBettingPanel(boolean visible) {
        bettingActionTable.setVisible(visible);
    }

    public boolean ActionPanelIsVisible() {
        return playerActionTable.isVisible();
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void togglePauseMenu() {
        paused = !paused;
        pauseMenu.setVisible(paused);
    }

    public void showGameOverMenu() {
        gameOverMenu.setVisible(true);
    }
}
