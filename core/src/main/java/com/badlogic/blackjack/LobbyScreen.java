// LobbyScreen.java (Complete file with modification to the listener)
package com.badlogic.blackjack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import java.util.HashMap;
import java.util.Map;

// Removed unused import: import sun.nio.cs.ext.ISO2022_CN_CNS;

public class LobbyScreen implements Screen
{
    public enum Mode { HOST, JOIN }

    private final Main game;
    private final Stage stage;
    private final Skin skin;

    // Fields for local font management
    private final FreeTypeFontGenerator fontGenerator;
    private final BitmapFont minecraftFont;

    // NEW: Fields for client discovery
    private Client discoveryClient;
    private final HashMap<String, Network.HostAdvertise> discoveredHosts = new HashMap<>();
    private Table roomListTable;

    TextButton createButton(String text, Skin skin) {
        TextButton button = new TextButton(text, skin);
        button.padRight(20f);
        button.padLeft(20f);
        button.padTop(10f);
        button.padBottom(10f);
        return button;
    }

    public LobbyScreen(final Main game, Mode initialMode)
    {
        Skin skin1;
        this.game = game;

        // 1. FONT AND SKIN INITIALIZATION (Moved from StartScreen)
        fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Minecraft.ttf"));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = 72;
        minecraftFont = fontGenerator.generateFont(parameter);

        try {
            skin1 = new Skin(Gdx.files.internal("ui/uiskin.json"));
        } catch (Exception e) {
            Gdx.app.error("LobbyScreen", "Could not load skin", e);
            skin1 = new Skin(); // Fallback
        }

        // Register the custom font. This is essential for the title label.
        this.skin = skin1;
        this.skin.add("minecraft-font", minecraftFont, BitmapFont.class);


        // NEW: Initialize and start the client for discovery
        discoveryClient = new Client();
        Network.register(discoveryClient.getKryo());
        discoveryClient.start();

// NEW: Initialize and start the client for discovery
        discoveryClient = new Client();
        Network.register(discoveryClient.getKryo());
        discoveryClient.start();

        discoveryClient.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.HostAdvertise) {
                    final Network.HostAdvertise ad = (Network.HostAdvertise) object;
                    String ipAddress = null;

                    // 1. Try to get IP from UDP Remote Address (Most likely correct for broadcasts)
                    java.net.InetSocketAddress udpAddress = connection.getRemoteAddressUDP();
                    if (udpAddress != null && udpAddress.getAddress() != null) {
                        ipAddress = udpAddress.getAddress().getHostAddress();
                    }

                    // 2. Fallback/Correction for unreliable broadcast addresses (e.g., 255.255.255.255, null, or local IP issues)
                    // If the IP is null, the broadcast address, or a vague address, we assume 127.0.0.1 for local testing.
                    if (ipAddress == null || ipAddress.equals("255.255.255.255") || ipAddress.startsWith("0.")) {
                        // CRITICAL FIX: Use loopback address 127.0.0.1 for self-hosted games
                        ipAddress = "127.0.0.1";
                        Gdx.app.log("LobbyScreen", "Forced IP to 127.0.0.1 for local host discovery.");
                    }

                    // Note: If running on a real LAN, you might prefer the actual LAN IP here.
                    // Since the host broadcast is being seen, 127.0.0.1 is the correct connection target for self-hosting.

                    final String finalIpAddress = ipAddress;

                    Gdx.app.postRunnable(() -> {
                        // Store/update the discovered host using the guaranteed valid IP
                        discoveredHosts.put(finalIpAddress, ad);
                        Gdx.app.log("LobbyScreen", "Host discovered and added to list: " + finalIpAddress + " | Room: " + ad.lobbyName);
                        // Trigger a UI refresh
                        populateRoomList();
                    });
                }
            }
        });


        this.stage = new Stage(new ScreenViewport(), game.spriteBatch);

        Table currentTable;
        if (initialMode == Mode.HOST) {
            currentTable = hostGameLayout(game);
        } else { // Mode.JOIN
            currentTable = joinGameLayout(game);
            startDiscovery(); // Start discovery immediately for join mode
        }

        currentTable.setVisible(true);
        stage.addActor(currentTable);

        // Input processing for ESC key
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    // ESC key always goes back to the main menu (StartScreen)
                    game.setScreen(new StartScreen(game));
                    dispose();
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setInputProcessor(multiplexer);
    }

    // NEW: Non-blocking method to run the discovery in a background thread
    private void runDiscovery() {
        new Thread("DiscoveryClient") {
            @Override
            public void run() {
                // This call BLOCKS for 5000ms. Running it here prevents the main thread from freezing.
                discoveryClient.discoverHosts(Network.PORT_UDP, 5000);

                // After the 5000ms timeout, log the result
                Gdx.app.postRunnable(() -> {
                    Gdx.app.log("LobbyScreen", "Host discovery completed after timeout.");
                    // After discovery times out, refresh the list to show "No games found" if empty
                    populateRoomList();
                });
            }
        }.start();
    }

    // MODIFIED: Method to send a discovery request
    private void startDiscovery() {
        Gdx.app.log("LobbyScreen", "Sent host discovery request, running in background thread.");
        runDiscovery(); // Call the non-blocking method
    }


    Table hostGameLayout(Main game) {
// ... This method remains unchanged ...
        Table hostTable = new Table(skin);
        hostTable.setFillParent(true);

        hostTable.row().padBottom(10f);
        Label roomName = new Label("Room name:", skin);
        hostTable.add(roomName).align(Align.left).fill();
        TextField roomNameField = new TextField("My Game",skin);
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
        maxPlayers.setSelected(4);
        hostTable.add(maxPlayers).fill();
        hostTable.row();

        // Button container for Back and Create Game
        Table buttonTable = new Table();

        TextButton backButton = createButton("Back", skin);
        buttonTable.add(backButton).padRight(10f);

        TextButton createGameButton = createButton("Create Game", skin);
        buttonTable.add(createGameButton).padLeft(10f);

        hostTable.add(buttonTable).padTop(20f).colspan(2);

        createGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Transition to MultiplayerScreen as HOST
                game.setScreen(new MultiplayerScreen(game, true, maxPlayers.getSelected(), null));
                dispose();
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Go back to StartScreen
                game.setScreen(new StartScreen(game));
                dispose();
            }
        });
        return hostTable;
    }

    Table joinGameLayout(Main game) {
        Table mainTable = new Table(skin);
        mainTable.setFillParent(true);

        // Title: USE THE CORRECT FONT NAME "minecraft-font"
        Label titleLabel = new Label("Available Rooms", skin, "minecraft-font", Color.WHITE);
        titleLabel.setFontScale(1f);
        mainTable.add(titleLabel).padBottom(20f);
        mainTable.row();

        // Create header table
        Table headerTable = new Table();

        Label nameHeader = new Label("Room Name", skin);
        nameHeader.setColor(Color.LIGHT_GRAY);
        headerTable.add(nameHeader).width(250f).padLeft(10f).align(Align.left);

        Label playersHeader = new Label("Players", skin);
        playersHeader.setColor(Color.LIGHT_GRAY);
        headerTable.add(playersHeader).width(80f).padLeft(-40f);

        Label lockHeader = new Label("Password", skin);
        lockHeader.setColor(Color.LIGHT_GRAY);
        headerTable.add(lockHeader).width(150f).padLeft(10f);

        Label actionHeader = new Label("", skin);
        headerTable.add(actionHeader).padLeft(10f).padRight(10f);

        mainTable.add(headerTable).padBottom(5f);
        mainTable.row();

        // Create a container for the scrollable list (Store reference here)
        roomListTable = new Table();
        roomListTable.top();

        // Initial population of the list (will show "Searching...")
        populateRoomList();

        // Create scroll pane
        ScrollPane scrollPane = new ScrollPane(roomListTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        // Add scroll pane to main table with fixed size
        mainTable.add(scrollPane).width(550f).height(400f).pad(10f);
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
                Gdx.app.log("LobbyScreen", "Refreshing lobby list (network discovery)");
                // Clear old rooms and restart discovery
                discoveredHosts.clear();
                populateRoomList();
                startDiscovery();
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Go back to StartScreen
                game.setScreen(new StartScreen(game));
                dispose();
            }
        });

        return mainTable;
    }

    // NEW: Populates the roomListTable with discovered hosts
    private void populateRoomList() {
        roomListTable.clearChildren(); // Clear existing room components

        if (discoveredHosts.isEmpty()) {
            Label noRoomsLabel = new Label("Searching for games...", skin);
            noRoomsLabel.setColor(Color.WHITE);
            roomListTable.add(noRoomsLabel).padTop(100f);
        } else {
            // Iterate over discovered hosts and add components
            for (Map.Entry<String, Network.HostAdvertise> entry : discoveredHosts.entrySet()) {
                String ipAddress = entry.getKey();
                Network.HostAdvertise ad = entry.getValue();

                // HasPassword is set to false as HostAdvertise does not contain that field yet.
                addRoomComponent(roomListTable, ad.lobbyName, ad.currentPlayers,
                    ad.maxPlayers, false, ipAddress);
            }
        }
        // Ensure the table updates its layout
        roomListTable.invalidateHierarchy();
    }


    private void addRoomComponent(Table table, String roomName, int currentPlayers,
                                  int maxPlayers, boolean hasPassword, String ipAddress) {
        Table roomComponent = new Table();
// ... rest of addRoomComponent remains unchanged ...
        // Room name
        Label nameLabel = new Label(roomName, skin);
        nameLabel.setColor(Color.WHITE);
        roomComponent.add(nameLabel).width(250f).padLeft(50f).align(Align.left);

        // Player count
        Label playersLabel = new Label(currentPlayers + "/" + maxPlayers, skin);
        playersLabel.setColor(Color.LIGHT_GRAY);
        roomComponent.add(playersLabel).width(80f).padLeft(10f);

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
                Gdx.app.log("LobbyScreen", "Attempting to join room: " + roomName + " at IP: " + ipAddress);

                // Transition to MultiplayerScreen as a client
                game.setScreen(new MultiplayerScreen(game, false, 0, ipAddress));
                dispose();
            }
        });

        table.add(roomComponent).fillX().padBottom(20f).padLeft(10f).padRight(10f);
        table.row();
    }


    @Override
    public void render(float delta)
    {
// ... unchanged render ...
        Gdx.gl.glClearColor(0.5f, 0,0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (discoveryClient != null) {
            discoveryClient.stop(); // NEW: Stop the client
        }
        stage.dispose();
        skin.dispose();
        fontGenerator.dispose();
    }
}
