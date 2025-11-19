package com.badlogic.blackjack;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import com.badlogic.blackjack.lobby.HostLobbyScreen;
import com.badlogic.blackjack.lobby.ClientLobbyScreen;

public class StartScreen implements Screen {
    private Stage stage;
    private Skin skin;
    private TiledDrawable startBackground;
    private TiledDrawable hostBackground;
    private TiledDrawable joinBackground;
    private TiledDrawable localBackground;
    private Table hostTable;
    private Table startTable;
    private Table localTable;
    private Table joinTable;
    private TextButtonStyle t1;
    private int depth = 0;

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
        table.setBackground(localBackground);
        table.row().padBottom(10f);
        Label maxPlayersLabel = new Label("Max number of players: ", skin);
        maxPlayersLabel.setWidth(100);
        table.add(maxPlayersLabel).align(Align.left).fill();

        SelectBox<Integer> maxPlayers = new SelectBox<>(skin);
        maxPlayers.setItems(2,3,4,5,6,7);
        table.add(maxPlayers).fill().padLeft(50f);

        table.row();


        Table confirm = new Table();

        TextButton backButton = createButton("Back", skin);
        confirm.add(backButton).padRight(10f).padTop(50f);

        TextButton createGameButton = createButton("Create Game", skin);
        confirm.add(createGameButton).padLeft(10f).padTop(50f);

        table.add(confirm).colspan(2);

        createGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // SCREEN TRANSITION - LOCAL GAME STARTS IMMEDIATELY
                game.setScreen(new GameScreen(game, maxPlayers.getSelected()));
                dispose();
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                localTable.setVisible(false);
                startTable.setVisible(true);
                depth = 0;
            }
        });

        return table;
    }

    Table joinGameLayout(Main game) {
        Table joinTable = new Table();
        joinTable.setFillParent(true);
        joinTable.setBackground(joinBackground);

        joinTable.row().padBottom(10f);
        Label playerNameLabel = new Label("Your Name:", skin);
        playerNameLabel.setWidth(100);
        joinTable.add(playerNameLabel).align(Align.left).fill();

        TextField playerNameField = new TextField("Guest", skin);
        playerNameField.setMaxLength(12);
        joinTable.add(playerNameField).fill().padLeft(40f).width(300f);

        joinTable.row().padBottom(10f);
        Label ipLabel = new Label("Host IP Address:", skin);
        ipLabel.setWidth(100);
        joinTable.add(ipLabel).align(Align.left).fill();

        TextField ipAddressField = new TextField("127.0.0.1", skin);
        joinTable.add(ipAddressField).fill().padLeft(40f).width(300f);

        joinTable.row().padTop(20f);

        TextButton backButton = createButton("Back", skin);
        joinTable.add(backButton).padRight(10f);

        TextButton confirmButton = createButton("Confirm", skin);
        joinTable.add(confirmButton).padLeft(10f);

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                joinTable.setVisible(false);
                startTable.setVisible(true);
                depth = 0;
            }
        });

        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Transition to ClientLobbyScreen and start connection attempt
                String ipAddress = ipAddressField.getText();
                String playerName = playerNameField.getText();
                game.setScreen(new ClientLobbyScreen(game, playerName, ipAddress));
                dispose();
            }
        });

        return joinTable;
    }

    Table multiplayerScreenLayout(Main game) {
        Table startTable = new Table();
        startTable.setBackground(startBackground);
        startTable.setFillParent(true);

        Table titleTable = new Table();
        Label gameTitle = new Label("Blackjack", skin, "minecraft-font", Color.WHITE);
        titleTable.add(gameTitle);
        startTable.row().padBottom(80f);

        startTable.add(titleTable);
        startTable.row();

        TextButton localGame = createButton("Local Game", skin);
        localGame.getLabel().setFontScale(0.5f);
        startTable.add(localGame);
        startTable.row().pad(10f);

        TextButton hostGame = createButton("Host Game", skin);
        hostGame.getLabel().setFontScale(0.5f);
        startTable.add(hostGame);
        startTable.row().padBottom(10f);;

        TextButton joinGame = createButton("Join Game", skin);
        joinGame.getLabel().setFontScale(0.5f);
        startTable.add(joinGame);
        startTable.row();

        TextButton exitButton = createButton("Exit", skin);
        exitButton.getLabel().setFontScale(0.5f);
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
                startTable.setVisible(false);
                hostTable.setVisible(true);
                depth=1;
            }
        });

        joinGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startTable.setVisible(false);
                joinTable.setVisible(true); // SHOW NEW JOIN TABLE
                depth=1;
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

    Table hostGameLayout(Main game) {
        Table hostTable = new Table();
        // hostTable.setDebug(true);
        hostTable.setFillParent(true);
        hostTable.setBackground(hostBackground);

        // Host properties capture
        TextField roomNameField = new TextField("My Blackjack Room",skin);
        roomNameField.setMaxLength(20);
        SelectBox<Integer> maxPlayersSelectBox = new SelectBox<>(skin);
        maxPlayersSelectBox.setItems(2,3,4,5,6,7);

        TextField playerNameField = new TextField("Host", skin);
        playerNameField.setMaxLength(12);

        hostTable.row().padBottom(10f);
        Label roomName = new Label("Room name:", skin);
        hostTable.add(roomName).align(Align.left).fill();
        hostTable.add(roomNameField).width(300f).fill().padLeft(20f); // USE THE TEXT FIELD

        hostTable.row().padBottom(10f);
        Label playerNameLabel = new Label("Your Name:", skin);
        hostTable.add(playerNameLabel).align(Align.left).fill();
        hostTable.add(playerNameField).fill().padLeft(20f);

        hostTable.row().padBottom(10f);
        Label maxPlayersLabel = new Label("Max number of players: ", skin);
        maxPlayersLabel.setWidth(100);
        hostTable.add(maxPlayersLabel).align(Align.left).fill();
        hostTable.add(maxPlayersSelectBox).fill().padLeft(20f);
        hostTable.row();

        Table buttonTable = new Table();

        TextButton backButton = createButton("Back", skin);
        buttonTable.add(backButton).padRight(10f).padTop(50f);

        TextButton createGameButton = createButton("Create Game", skin); // RENAMED FROM refreshButton
        buttonTable.add(createGameButton).padLeft(10f).padTop(50f);

        hostTable.add(buttonTable).padTop(20f).colspan(2);

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hostTable.setVisible(false);
                startTable.setVisible(true);
                depth = 0;
            }
        });

        createGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = roomNameField.getText();
                int max = maxPlayersSelectBox.getSelected();
                String hostName = playerNameField.getText();

                // Transition to HostLobbyScreen and start the server/client
                game.setScreen(new HostLobbyScreen(game, hostName, name, max));
                dispose();
            }
        });

        return hostTable;
    }


    public StartScreen(final Main game) {
        // Tiles declaration
        Texture startTex = new Texture(Gdx.files.internal("pixel/Picture/color_background_91.png"));
        Texture hostTex = new Texture(Gdx.files.internal("pixel/Picture/color_background_6.png"));
        Texture joinTex = new Texture(Gdx.files.internal("pixel/Picture/color_background_4.png"));
        Texture localTex = new Texture(Gdx.files.internal("pixel/Picture/color_background_8.png"));

        startBackground = new TiledDrawable(new TextureRegion(startTex));
        hostBackground = new TiledDrawable(new TextureRegion(hostTex));
        joinBackground = new TiledDrawable(new TextureRegion(joinTex));
        localBackground = new TiledDrawable(new TextureRegion(localTex));        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Minecraft.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 72;
        parameter.borderColor = Color.DARK_GRAY;
        parameter.borderWidth = 3f;
        BitmapFont font1 = generator.generateFont(parameter);
        generator.dispose();

        stage = new Stage(new ScreenViewport());

        this.skin = game.assets.skin;

        if (skin != null) {
            skin.add("minecraft-font", font1, BitmapFont.class);
        } else {
            Gdx.app.error("StartScreen", "Skin is null, UI may not render correctly.");
        }


        startTable = multiplayerScreenLayout(game);
        hostTable = hostGameLayout(game);
        localTable = localGameLayout(game);
        joinTable = joinGameLayout(game);

        localTable.setVisible(false);
        joinTable.setVisible(false);
        hostTable.setVisible(false);

        stage.addActor(hostTable);
        stage.addActor(startTable);
        stage.addActor(localTable);
        stage.addActor(joinTable);


        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage); // Keep UI input
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    if (depth == 1) {
                        hostTable.setVisible(false);
                        localTable.setVisible(false);
                        joinTable.setVisible(false);
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
    }
}
