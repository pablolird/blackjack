package com.badlogic.blackjack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UI {
    private final Stage stage;
    private final Skin skin;
    private final Table playerActionTable;
    private final Window pauseMenu;
    private final Map<Player, Label> playerScoreLabels;
    private final Map<Player, Label> playerBalanceLabels;
    private final BlackjackLogic blackjackLogic;
    private boolean paused;
    private final Get g;
    private SpriteBatch spriteBatch;
    Player currentPlayer;


    public UI(Viewport vp, SpriteBatch sb, BlackjackLogic bl) {
        this.blackjackLogic = bl;
        this.g = new Get();
        this.paused = false;
        playerScoreLabels = new HashMap<>();
        playerBalanceLabels = new HashMap<>();

        stage = new Stage(vp, sb);
        skin = new Skin(Gdx.files.internal("skin/x1/uiskin.json"));

        Gdx.input.setInputProcessor(stage);

        // 2. Build the Player Action Panel (initially hidden)
        playerActionTable = new Table();
        playerActionTable.setFillParent(true);
        playerActionTable.bottom(); // Anchor to the bottom
        stage.addActor(playerActionTable);

        TextButton hitButton = new TextButton("Hit", skin);
        TextButton standButton = new TextButton("Stand", skin);

        hitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                blackjackLogic.hit();
            }
        });

        standButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                blackjackLogic.stand();
            }
        });

        playerActionTable.add(hitButton).pad(10);
        playerActionTable.add(standButton).pad(10);
        playerActionTable.setVisible(false); // Hide it by default

        // 3. Build the Pause Menu (initially hidden)
        pauseMenu = new Window("Pause", skin);
        TextButton quitButton = new TextButton("Leave Room", skin);
        pauseMenu.add(quitButton);
        pauseMenu.pack(); // Size the window to its contents
        pauseMenu.setPosition(stage.getWidth() / 2 - pauseMenu.getWidth() / 2, stage.getHeight() / 2 - pauseMenu.getHeight() / 2);
        stage.addActor(pauseMenu);
        pauseMenu.setVisible(false); // Hide it by default

        buildLayout(bl.playersList);
    }

    // This method now places labels at specific coordinates
    public void buildLayout(List<Player> players) {
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Label scoreLabel = new Label(p.getName() + ": 0", skin);
            scoreLabel.scaleBy(-0.5f);
            scoreLabel.setAlignment(Align.left);

            // Get the specific coordinates from your Get class
            Label balanceLabel = new Label(p.getBalance() + "c", skin);
            balanceLabel.scaleBy(-0.5f);
            balanceLabel.setAlignment(Align.left);

            String playerKey = "PLAYER" + (i + 1) + "_CARD";
            Vector2 position = g.position.get(playerKey);

            if (position != null) {
                scoreLabel.setPosition(position.x - (scoreLabel.getWidth() / 2f) + g.scoreShift.get("PLAYER"+(i+1)).x,
                    position.y - (scoreLabel.getHeight() / 2f) + g.scoreShift.get("PLAYER"+(i+1)).y);

                float yOffset;
                if (i == 0 || i == 6)
                {
                    yOffset = 25; // Player 1 and 7 offset (on top of name)
                } else
                {
                    yOffset = -25; // Other player offset (below name)
                }

                balanceLabel.setPosition(position.x - (balanceLabel.getWidth() / 2f) + g.scoreShift.get("PLAYER"+(i+1)).x,
                    position.y - (balanceLabel.getHeight() / 2f) + g.scoreShift.get("PLAYER"+(i+1)).y + yOffset);

            }

            playerScoreLabels.put(p, scoreLabel);
            stage.addActor(scoreLabel); // Add the label directly to the stage
            playerBalanceLabels.put(p, balanceLabel);
            stage.addActor(balanceLabel);
        }
    }

    public void updatePlayerScore(Player p) {
        Label scoreLabel = playerScoreLabels.get(p);
        if (scoreLabel != null) {
            scoreLabel.setText(p.getName() + ": " + p.totalValue());
        }
    }

    public void updatePlayerBalance(Player p) {
        Label balanceLabel = playerBalanceLabels.get(p);
        if (balanceLabel != null) {
            balanceLabel.setText(p.getBalance() + "c");
        }
    }

    public void update(float delta) {
        stage.act(delta);
    }

    public void render() {
        stage.draw();
    }

    public void updateCurrentPlayerColor(Player p) {
        playerScoreLabels.get(currentPlayer).setColor(Color.WHITE);
        setCurrentPlayer(p);
    }

    public void setCurrentPlayer(Player p) {
        currentPlayer = p;
        playerScoreLabels.get(currentPlayer).setColor(Color.BLUE);
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
}
