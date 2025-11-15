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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UI {
    private Stage stage;
    private Skin skin;
    private Table actionTable;
    private Window pauseMenu;
    private Window gameOverMenu;
    private BlackjackLogic blackjackLogic;
    private boolean paused;
    private AudioManager audioManager;
    private Get g;
    private Main game;
    Player currentPlayer;

    private ArrayList<TextButton> bettingButtons;
    private ArrayList<TextButton> mainButtons;

    private HashMap<Integer, PlayerUI> playerUI;
    private Label dealerScoreLabel;

    private Drawable coinDrawable;
    private Drawable clubDrawable;
    private SpriteBatch spriteBatch;

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
        this.playerUI = new HashMap<>();
        this.game = game;
        this.g = new Get();
        this.paused = false;
        this.audioManager = audioManager;
        this.stage = new Stage(vp, sb);

        this.bettingButtons = new ArrayList<>();
        this.mainButtons = new ArrayList<>();

        Gdx.input.setInputProcessor(stage);
        initSkin(skin_path);
    }

    public void buildActionTable() {
        actionTable = new Table();
        actionTable.setFillParent(true);
        actionTable.bottom(); // Anchor to the bottom
        stage.addActor(actionTable);

        // Create 3 main action buttons (hit, stand, lock bet)
        TextButton hitButton = createButton(" HIT ", HEX("#821010"));
        TextButton standButton = createButton(" STAND ", HEX("#100d4f"));

        // Initially disable them
        hitButton.setDisabled(true);
        standButton.setDisabled(true);

        // Add main buttons to array to disable them in group when needed
        mainButtons.add(hitButton);
        mainButtons.add(standButton);

        // Add betting buttons to array to disable them in group too
        TextButton confirmBetButton = createButton(" Confirm Bet ", HEX("#e37622"));
        bettingButtons.add(confirmBetButton);

        // Event listeners
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

        confirmBetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                blackjackLogic.playerLockInBet();
                audioManager.playSound(SoundType.LOCKBET, 0.65f);
            }
        });

        // Create betting buttons (10,50,100)
        int[] amounts = {10,50,100};
        String[] hexs = {"#43b32d","#96b32d","#c2ba21"};

        for (int i = 0; i < amounts.length; i++) {
            TextButton t = createButton(" Bet "+amounts[i]+" ", HEX(hexs[i]));
            addBetButton(t,amounts[i]);
            bettingButtons.add(t);
            actionTable.add(t).pad(10);
        }

        // Then add main action buttons
        actionTable.add(confirmBetButton).pad(10);
        actionTable.add(hitButton).pad(10);
        actionTable.add(standButton).pad(10);
    }

    private void addBetButton(Button button, int amount) {
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                blackjackLogic.playerAddToBet(amount);
                audioManager.playSound(SoundType.BET, 1.0f);
            }
        });
    }

    private void buildPauseMenu() {
        // Pause menu allegedly
        pauseMenu = new Window(" Pause ", skin);
        // This should have a conditional rendering and logic depending on whether user is host or client
        // Also for host or client
        TextButton quitButton = createButton(" Quit Room ", HEX("#383645"));
        pauseMenu.add(quitButton);
        pauseMenu.pack(); // Size the window to its contents
        pauseMenu.setPosition(stage.getWidth() / 2 - pauseMenu.getWidth() / 2, stage.getHeight() / 2 - pauseMenu.getHeight() / 2);
        pauseMenu.getTitleLabel().setFontScale(0.5f);
        stage.addActor(pauseMenu);
        pauseMenu.setVisible(false); // Hide it by default

    }

    private void buildGameOverMenu(BlackjackLogic bl) {
        TextButton quitButton = createButton(" Quit Room ", HEX("#383645"));
        // 4. Build the Game over Menu (initially hidden too)
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

    public UI(Viewport vp, SpriteBatch sb, BlackjackLogic bl, AudioManager audioManager, Main game) {

        init(bl, audioManager, game, vp, sb, "SKIN_JSON2/skin.json");
        buildActionTable();
        // This should have a conditional rendering and logic depending on whether user is host or client
        buildPauseMenu();
        buildGameOverMenu(bl);

        // Check later where to put this

        // =========================-------------------------------------------------
        Texture coinTex = new Texture(Gdx.files.internal("coin1.png"));
        Texture clubTex = new Texture(Gdx.files.internal("spade.png"));

        // Wrap them in Drawables (needed for UI widgets)
        coinDrawable = new TextureRegionDrawable(new TextureRegion(coinTex));
        clubDrawable = new TextureRegionDrawable(new TextureRegion(clubTex));
        // =========================-------------------------------------------------
        buildLayout(bl.playersList);

    }

    // --- BUILD LAYOUT (unchanged by your request) ---
    public void buildLayout(List<Player> players) {
        // DEALER
        Window w = new Window("", skin);
        w.setSize(90,90);
        w.top().left(); // align contents of the window
        // CONSTANT
        Label dealerNameLabel = new Label("DEALER", skin);
        dealerNameLabel.setFontScale(0.4f);
        w.add(dealerNameLabel).colspan(2).padBottom(5f);
        w.row();
        // VARIABLE
        dealerScoreLabel = new Label("0", skin);
        dealerScoreLabel.setFontScale(0.4f);
        dealerScoreLabel.setColor(255/255f, 230/255f, 156/255f, 1);
        Vector2 dealerPosition = g.position.get("DEALER_CARD");
        Vector2 dealerShift = g.scoreShift.get("DEALER");
        w.setPosition(dealerPosition.x - (w.getWidth() / 2f) + dealerShift.x, dealerPosition.y - (w.getHeight() / 2f) + dealerShift.y);
        w.add(new Image(clubDrawable));
        w.add(dealerScoreLabel).padBottom(5);
        stage.addActor(w);

        // PLAYERS
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            PlayerUI pUI = new PlayerUI(p.getName()+i, p.getBalance(), p.totalValue(), skin);

            Window w2 = new Window("", skin);
            w2.setSize(140,90);
            w2.add(pUI.playerName).colspan(2).padBottom(5f).center();
            w2.row();

            // ================================
            // Check this shady math later.
            String playerKey = "PLAYER" + (i + 1) + "_CARD";
            Vector2 position = g.position.get(playerKey);
            if (position != null) {
                w2.setPosition(position.x - (w2.getWidth() / 2f) + g.scoreShift.get("PLAYER" + (i + 1)).x,
                    position.y - (w2.getHeight() / 2f) + g.scoreShift.get("PLAYER" + (i + 1)).y);
            }
            // ================================

            w2.add(new Image(coinDrawable));
            w2.add(pUI.playerBalance);
            w2.add(new Image(clubDrawable));
            w2.add(pUI.playerScore).padBottom(5f);
            stage.addActor(w2);

            // Store UI in map for later access
            playerUI.put(p.getID(), pUI);
        }
    }

    // --- REBUILD LAYOUT ---
    public void rebuildLayout(List<Player> players, Dealer d) {
        for (int i = 0; i < players.toArray().length; i++) {
            Player p = players.get(i);
            // Get corresponding player UI
            PlayerUI pUI =  playerUI.get(p.getID());

            pUI.updateBalance(p.getBalance());
            pUI.updateScore(p.totalValue());
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

    public void update(float delta) {
        stage.act(delta);
    }
    public void render() {
        stage.draw();
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
        for (PlayerUI pUI : playerUI.values()) {
            pUI.playerName.setColor(HEX("#FFF"));
        }

        currentPlayer = p;
        if (currentPlayer != null) {
            PlayerUI current = playerUI.get(p.getID());
            if (current != null) {
                current.playerName.setColor(Color.GREEN);
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
            mainButtons.get(i).setDisabled(!visible);
        }
    }

    public void showBettingPanel(boolean visible) {
        for (int i = 0; i < bettingButtons.toArray().length; i++) {
            bettingButtons.get(i).setDisabled(!visible);
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

    public void showGameOverMenu() {
        gameOverMenu.setVisible(true);
    }
}
