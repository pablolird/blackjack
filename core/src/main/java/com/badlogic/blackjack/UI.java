package com.badlogic.blackjack;

import com.badlogic.blackjack.audio.AudioManager;
import com.badlogic.blackjack.audio.SoundType;
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
    private final Table bettingActionTable;
    private final Window pauseMenu;
    private final Map<Player, Label> playerScoreLabels;
    private final Map<Player, Label> playerBalanceLabels;
    private final BlackjackLogic blackjackLogic;
    private boolean paused;
    private AudioManager audioManager;
    private final Get g;
    private Label dealerScoreLabel;
    private SpriteBatch spriteBatch;
    Player currentPlayer;


    public UI(Viewport vp, SpriteBatch sb, BlackjackLogic bl, AudioManager audioManager) {
        this.blackjackLogic = bl;
        this.g = new Get();
        this.paused = false;
        playerScoreLabels = new HashMap<>();
        playerBalanceLabels = new HashMap<>();
        this.audioManager = audioManager;

        stage = new Stage(vp, sb);
        skin = new Skin(Gdx.files.internal("skin/x1/uiskin.json"));

        Gdx.input.setInputProcessor(stage);

        // 1. Build the Player Action Panel (initially hidden)
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

        // 2. Build the Betting Action Panel
        bettingActionTable = new Table();
        bettingActionTable.setFillParent(true);
        bettingActionTable.bottom();
        stage.addActor(bettingActionTable);

        TextButton bet10Button = new TextButton("Bet 10", skin);
        TextButton bet50Button = new TextButton("Bet 50", skin);
        TextButton bet100Button = new TextButton("Bet 100", skin);
        TextButton confirmBetButton = new TextButton("Confirm Bet", skin);

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
            }
        });

        bettingActionTable.add(bet10Button).pad(10);
        bettingActionTable.add(bet50Button).pad(10);
        bettingActionTable.add(bet100Button).pad(10);
        bettingActionTable.add(confirmBetButton).pad(10);
        bettingActionTable.setVisible(false);

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
        // DEALER
        dealerScoreLabel = new Label("Dealer: 0", skin);
        dealerScoreLabel.scaleBy(-0.5f);
        dealerScoreLabel.setAlignment(Align.left);
        dealerScoreLabel.setColor(255/255f, 230/255f, 156/255f, 1);
        Vector2 dealerPosition = g.position.get("DEALER_CARD");
        Vector2 dealerShift = g.scoreShift.get("DEALER");
        dealerScoreLabel.setPosition(dealerPosition.x - (dealerScoreLabel.getWidth() / 2f) + dealerShift.x,
            dealerPosition.y - (dealerScoreLabel.getHeight() / 2f) + dealerShift.y);
        stage.addActor(dealerScoreLabel);

        // PLAYERS
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Label scoreLabel = new Label(p.getName() + ": 0", skin);
            scoreLabel.scaleBy(-0.5f);
            scoreLabel.setAlignment(Align.left);
            scoreLabel.setColor(128/255f, 128/255f, 128/255f, 0.75f);

            // Get the specific coordinates from your Get class
            Label balanceLabel = new Label(p.getBalance() + "c", skin);
            balanceLabel.scaleBy(-0.5f);
            balanceLabel.setAlignment(Align.left);
            balanceLabel.setColor(Color.YELLOW);

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

    public void updateDealerScore(Dealer dealer) {
        if (dealerScoreLabel != null) {
            dealerScoreLabel.setText("Dealer: " + dealer.totalValue());
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

    public void updateCurrentPlayerColor(Player p)
    {
        if (!(currentPlayer == null)) {
            playerScoreLabels.get(currentPlayer).setColor(128 / 255f, 128 / 255f, 128 / 255f, 0.75f);
        }
        setCurrentPlayer(p);
    }

    public void setCurrentPlayer(Player p) {
        currentPlayer = p;
        if(!(currentPlayer == null)) {
            playerScoreLabels.get(currentPlayer).setColor(Color.WHITE);
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
}
