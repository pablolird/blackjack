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
    private Table hostTable;
    private Table startTable;
    private Table joinGameTable;
    private Table localTable;
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
                startTable.setVisible(false);
                hostTable.setVisible(true);
                depth=1;
            }
        });

        joinGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // This is the screen transition!
                startTable.setVisible(false);
                joinGameTable.setVisible(true);
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

        hostTable.row().padBottom(10f);
        Label roomName = new Label("Room name:", skin);
        hostTable.add(roomName).align(Align.left).fill();
        TextField roomNameField = new TextField("",skin);
        hostTable.add(roomNameField);

        hostTable.row().padBottom(10f);
        Label roomPassword = new Label("Room password:", skin);
        hostTable.add(roomPassword).align(Align.left).fill();
        TextField roomPasswordField = new TextField("",skin);
        hostTable.add(roomPasswordField);

        hostTable.row().padBottom(10f);
        Label maxPlayersLabel = new Label("Max number of players: ", skin);
        maxPlayersLabel.setWidth(100);
        hostTable.add(maxPlayersLabel).align(Align.left).fill();

        SelectBox<Integer> maxPlayers = new SelectBox<>(skin);
        maxPlayers.setItems(2,3,4,5,6,7);
        hostTable.add(maxPlayers).fill();
        hostTable.row();

        // Button container for Back and Refresh
        Table buttonTable = new Table();

        TextButton backButton = createButton("Back", skin);
        buttonTable.add(backButton).padRight(10f);

        TextButton refreshButton = createButton("Create Game", skin);
        buttonTable.add(refreshButton).padLeft(10f);

        hostTable.add(buttonTable).padTop(20f).colspan(2);

        refreshButton.addListener(new ChangeListener() {
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
                hostTable.setVisible(false);
                startTable.setVisible(true);
                depth = 0;
            }
        });
        return hostTable;
    }

    Table joinGameLayout(Main game) {
        Table mainTable = new Table(skin);
        mainTable.setFillParent(true);

        // Title
        Label titleLabel = new Label("Available Rooms", skin, "minecraft-font", Color.WHITE);
        mainTable.add(titleLabel).padBottom(20f);
        mainTable.row();

        // Create header table
        Table headerTable = new Table();
//        headerTable.setDebug(true);

        Label nameHeader = new Label("Room Name", skin);
        nameHeader.setColor(Color.LIGHT_GRAY);
        headerTable.add(nameHeader).width(200f).padLeft(10f).align(Align.left);

        Label playersHeader = new Label("Players", skin);
        playersHeader.setColor(Color.LIGHT_GRAY);
        headerTable.add(playersHeader).width(70f).padLeft(10f);

        Label pingHeader = new Label("Ping", skin);
        pingHeader.setColor(Color.LIGHT_GRAY);
        headerTable.add(pingHeader).width(50f).padLeft(10f);

        Label lockHeader = new Label("", skin);
        headerTable.add(lockHeader).width(220f).padLeft(10f);

        Label actionHeader = new Label("", skin);
        headerTable.add(actionHeader).padLeft(10f).padRight(10f);

        mainTable.add(headerTable).padBottom(5f);
        mainTable.row();

        // Create a container for the scrollable list
        Table roomListTable = new Table();
        roomListTable.top();

        // Add some demo rooms
        addRoomComponent(roomListTable, "Bob's Room", 3, 7, 45, true);
        addRoomComponent(roomListTable, "Pro Players Only", 6, 7, 120, true);
        addRoomComponent(roomListTable, "Casual Game", 2, 7, 30, false);
        addRoomComponent(roomListTable, "High Stakes", 5, 7, 80, true);
        addRoomComponent(roomListTable, "Beginners Welcome", 1, 7, 25, false);
        addRoomComponent(roomListTable, "Friday Night Cards", 4, 7, 55, false);
        addRoomComponent(roomListTable, "VIP Room", 7, 7, 150, true);
        addRoomComponent(roomListTable, "Quick Match", 2, 7, 20, false);
        addRoomComponent(roomListTable, "Bob's Room", 3, 7, 45, true);
        addRoomComponent(roomListTable, "Pro Players Only", 6, 7, 120, true);
        addRoomComponent(roomListTable, "Casual Game", 2, 7, 30, false);
        addRoomComponent(roomListTable, "High Stakes", 5, 7, 80, true);
        addRoomComponent(roomListTable, "Beginners Welcome", 1, 7, 25, false);
        addRoomComponent(roomListTable, "Friday Night Cards", 4, 7, 55, false);
        addRoomComponent(roomListTable, "VIP Room", 7, 7, 150, true);
        addRoomComponent(roomListTable, "Quick Match", 2, 7, 20, false);

        // Create scroll pane
        ScrollPane scrollPane = new ScrollPane(roomListTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        // Add scroll pane to main table with fixed size
        mainTable.add(scrollPane).width(600f).height(400f).pad(10f);
        mainTable.row();

        // Button container for Back and Refresh
        Table buttonTable = new Table();

        TextButton backButton = createButton("Back", skin);
        buttonTable.add(backButton).padRight(10f);

        TextButton refreshButton = createButton("Refresh", skin);
        buttonTable.add(refreshButton).padLeft(10f);

        mainTable.add(buttonTable).padTop(20f);

        refreshButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("Refreshing room list...");
                // Here you would fetch the updated room list from the server
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                joinGameTable.setVisible(false);
                startTable.setVisible(true);
                depth = 0;
            }
        });

        return mainTable;
    }

    private void addRoomComponent(Table table, String roomName, int currentPlayers,
                                  int maxPlayers, int ping, boolean hasPassword) {
        Table roomComponent = new Table();
        // roomComponent.setBackground(skin.getDrawable("default-round"));

        // Room name
        Label nameLabel = new Label(roomName, skin);
        nameLabel.setColor(Color.WHITE);
        roomComponent.add(nameLabel).width(200f).padLeft(10f).align(Align.left);

        // Player count
        Label playersLabel = new Label(currentPlayers + "/" + maxPlayers, skin);
        playersLabel.setColor(Color.LIGHT_GRAY);
        roomComponent.add(playersLabel).width(60f).padLeft(10f);

        // Ping/Latency
        Label pingLabel = new Label(ping + "ms", skin);
        Color pingColor = ping < 50 ? Color.GREEN : ping < 100 ? Color.YELLOW : Color.RED;
        pingLabel.setColor(pingColor);
        roomComponent.add(pingLabel).width(60f).padLeft(10f);

        // Lock icon (represented as text for now)
        Label lockLabel = new Label(hasPassword ? "(Password needed)" : "", skin);
        lockLabel.setColor(Color.YELLOW);
        roomComponent.add(lockLabel).width(150f).padLeft(10f);

        // Join button
        TextButton joinButton = new TextButton("Join", skin);
        joinButton.pad(5f, 15f, 5f, 15f);
        roomComponent.add(joinButton).padLeft(10f).padRight(10f);

        // Add click listener to join button
        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("Joining room: " + roomName +
                    " (Players: " + currentPlayers + "/" + maxPlayers +
                    ", Ping: " + ping + "ms, Password: " + hasPassword + ")");
                // Here you would typically show a password dialog if hasPassword is true,
                // then join the game
            }
        });

        table.add(roomComponent).fillX().padBottom(20f).padLeft(10f).padRight(10f);
        table.row();
    }

    public StartScreen(final Main game) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Minecraft.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 72;
        BitmapFont font1 = generator.generateFont(parameter); // font size 12 pixels
        generator.dispose(); // don't forget to dispose to avoid memory leaks!

        stage = new Stage(new ScreenViewport());

        try {
            skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        } catch (Exception e) {
            Gdx.app.error("StartScreen", "Could not load skin", e);
            // Create a programmatic skin as a fallback
            skin = new Skin();
        }

        skin.add("minecraft-font", font1, BitmapFont.class); // Add font to skin with a name

        startTable = multiplayerScreenLayout(game);
        hostTable = hostGameLayout(game);
        joinGameTable = joinGameLayout(game);
        localTable = localGameLayout(game);
        localTable.setVisible(false);

        stage.addActor(hostTable);
        stage.addActor(startTable);
        stage.addActor(localTable);
        hostTable.setVisible(false);
        stage.addActor(joinGameTable);
        joinGameTable.setVisible(false);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage); // Keep UI input
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    if (depth == 1) {
                        joinGameTable.setVisible(false);
                        hostTable.setVisible(false);
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
        // Dispose the skin if we created it here
        // If it's from Assets, do NOT dispose it.
        skin.dispose();
    }
}
