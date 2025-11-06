package com.badlogic.blackjack;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class StartScreen implements Screen {
    private Stage stage;
    private Skin skin; // Assumes you have a skin in assets
    private Texture background;
    // REMOVED: private Table hostTable;
    private Table startTable;
    // REMOVED: private Table joinGameTable;
    private Table localTable;
    private TextButtonStyle t1;
    private int depth = 0;

    // Fields for local font management (Needed to add font to skin)
    private final FreeTypeFontGenerator fontGenerator;
    private final BitmapFont minecraftFont;

    TextButton createButton(String text, Skin skin) {
        TextButton button = new TextButton(text, skin);
        button.padRight(20f);
        button.padLeft(20f);
        button.padTop(10f);
        button.padBottom(10f);
        return button;
    }

    Table localGameLayout(Main game) {
        Table table = new Table();
        table.setFillParent(true);

        table.row().padBottom(10f);
        Label maxPlayersLabel = new Label("Max number of players: ", skin);
        maxPlayersLabel.setWidth(100);
        table.add(maxPlayersLabel).align(Align.left).fill();

        SelectBox<Integer> maxPlayers = new SelectBox<>(skin);
        maxPlayers.setItems(2,3,4,5,6,7);
        table.add(maxPlayers).fill();

        table.row();

        TextButton backButton = createButton("Back", skin);
        table.add(backButton).padRight(10f);

        TextButton createGameButton = createButton("Create Game", skin);
        table.add(createGameButton).padLeft(10f);


        createGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // This is the screen transition!
                game.setScreen(new GameScreen(game, maxPlayers.getSelected()));
                dispose(); // Dispose this screen
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // This is the screen transition!
                localTable.setVisible(false);
                startTable.setVisible(true);
                depth = 0;
            }
        });

        return table;
    }

    Table multiplayerScreenLayout(Main game) {
        Table startTable = new Table();
        startTable.setFillParent(true);

        Table titleTable = new Table();
        Label gameTitle = new Label("Blackjack", skin, "minecraft-font", Color.WHITE);
        titleTable.add(gameTitle);
        startTable.row().padBottom(80f);

        startTable.add(titleTable);
        startTable.row();

        TextButton localGame = createButton("Local Game", skin);
        startTable.add(localGame);
        startTable.row().pad(10f);

        TextButton hostGame = createButton("Host Game", skin);
        startTable.add(hostGame);
        startTable.row().padBottom(10f);;

        TextButton joinGame = createButton("Join Game", skin);
        startTable.add(joinGame);
        startTable.row();

        TextButton exitButton = createButton("Exit", skin);
        startTable.add(exitButton);

        localGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startTable.setVisible(false);
                localTable.setVisible(true);
                depth=1;
            }
        });


        hostGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // NEW: Transition to LobbyScreen in HOST mode
                game.setScreen(new LobbyScreen(game, LobbyScreen.Mode.HOST));
                dispose();
            }
        });

        joinGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // MODIFIED: FORCED LOCAL CONNECTION
                // Skips LobbyScreen and attempts to connect directly to the loopback address (127.0.0.1).
                game.setScreen(new MultiplayerScreen(game, false, 0, "127.0.0.1"));
                dispose();
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // This is the screen transition!
                Gdx.app.exit();
            }
        });

        return startTable;
    }

    // REMOVED: hostGameLayout(Main game)
    // REMOVED: joinGameLayout(Main game)
    // REMOVED: addRoomComponent(...)

    public StartScreen(final Main game) {
        // FONT INITIALIZATION
        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Minecraft.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 72;
        minecraftFont = fontGenerator.generateFont(parameter);

        stage = new Stage(new ScreenViewport());

        try {
            skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        } catch (Exception e) {
            Gdx.app.error("StartScreen", "Could not load skin", e);
            // Create a programmatic skin as a fallback
            skin = new Skin();
        }

        skin.add("minecraft-font", minecraftFont, BitmapFont.class); // Add font to skin with a name

        startTable = multiplayerScreenLayout(game);
        // REMOVED: hostTable = hostGameLayout(game);
        // REMOVED: joinGameTable = joinGameLayout(game);
        localTable = localGameLayout(game);
        localTable.setVisible(false);

        // REMOVED: stage.addActor(hostTable);
        stage.addActor(startTable);
        stage.addActor(localTable);
        // REMOVED: hostTable.setVisible(false);
        // REMOVED: stage.addActor(joinGameTable);
        // REMOVED: joinGameTable.setVisible(false);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage); // Keep UI input
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    if (depth == 1) {
                        // Only localTable is managed by depth=1 now
                        localTable.setVisible(false);
                        startTable.setVisible(true);
                        depth = 0;
                    } else {
                        Gdx.app.exit();
                    }
                }
                return false; // don't consume other keys
            }
        });
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.5f, 0,0, 1);
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
        skin.dispose();
        fontGenerator.dispose(); // Dispose the generator
    }
}
