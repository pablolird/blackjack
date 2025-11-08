package com.badlogic.blackjack;

import com.badlogic.blackjack.audio.AudioManager;
import com.badlogic.blackjack.audio.SoundType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.badlogic.blackjack.network.GameClient;
import com.badlogic.blackjack.network.NetworkPacket;

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
    private final Window gameOverWaitingMenu; // For non-host players
    private final Window gameOverLocalMenu; // For local games (exit only)
    private final Map<Player, Label> playerScoreLabels;
    private final Map<Player, Label> playerBalanceLabels;
    private final BlackjackLogic blackjackLogic;
    private final GameClient gameClient;
    private boolean paused;
    private AudioManager audioManager;
    private final Get g;
    private Main game;
    private Label dealerScoreLabel;
    private SpriteBatch spriteBatch;
    Player currentPlayer;


    public UI(Viewport vp, SpriteBatch sb, BlackjackLogic bl, AudioManager audioManager, Main game) {
        this.blackjackLogic = bl;
        this.game = game;
        this.g = new Get();
        this.paused = false;
        playerScoreLabels = new HashMap<>();
        playerBalanceLabels = new HashMap<>();
        this.audioManager = audioManager;
        this.gameClient = game.gameClient;

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
                if (gameClient != null) {
                    gameClient.sendAction(NetworkPacket.PlayerActionType.HIT);
                } else {
                    // Local game - call logic directly
                    blackjackLogic.hit();
                }
            }
        });

        standButton.addListener(new ChangeListener() {
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
                if (gameClient != null) {
                    gameClient.sendAction(NetworkPacket.PlayerActionType.ADD_TO_BET, 10);
                } else {
                    // Local game - call logic directly
                    blackjackLogic.playerAddToBet(10);
                }
                audioManager.playSound(SoundType.BET, 1.0f);
            }
        });

        bet50Button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null) {
                    gameClient.sendAction(NetworkPacket.PlayerActionType.ADD_TO_BET, 50);
                } else {
                    // Local game - call logic directly
                    blackjackLogic.playerAddToBet(50);
                }
                audioManager.playSound(SoundType.BET, 1.0f);
            }
        });

        bet100Button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null) {
                    gameClient.sendAction(NetworkPacket.PlayerActionType.ADD_TO_BET, 100);
                } else {
                    // Local game - call logic directly
                    blackjackLogic.playerAddToBet(100);
                }
                audioManager.playSound(SoundType.BET, 1.0f);
            }
        });

        confirmBetButton.addListener(new ChangeListener() {
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

        bettingActionTable.add(bet10Button).pad(10);
        bettingActionTable.add(bet50Button).pad(10);
        bettingActionTable.add(bet100Button).pad(10);
        bettingActionTable.add(confirmBetButton).pad(10);
        bettingActionTable.setVisible(false);

        // 3. Build the Pause Menu (initially hidden)
        pauseMenu = new Window("Pause", skin);
        pauseMenu.pad(20);
        
        TextButton exitMatchButton = new TextButton("Exit Match", skin);
        pauseMenu.add(exitMatchButton);
        pauseMenu.pack(); // Size the window to its contents
        pauseMenu.setPosition(stage.getWidth() / 2 - pauseMenu.getWidth() / 2, stage.getHeight() / 2 - pauseMenu.getHeight() / 2);
        stage.addActor(pauseMenu);
        pauseMenu.setVisible(false); // Hide it by default
        
        // Exit Match button listener - will be set by GameScreen
        exitMatchButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null && game.exitMatchCallback != null) {
                    game.exitMatchCallback.onExitMatch();
                } else {
                    // Local game, just return to start screen
                    game.setScreen(new StartScreen(game));
                    dispose();
                }
            }
        });

        buildLayout(bl.playersList);

        // 4. Build the Game Over Menu for Host (with restart and exit options)
        gameOverMenu = new Window("Game Over", skin);
        gameOverMenu.pad(20);
        gameOverMenu.padTop(50);
        
        TextButton restartMatchButton = new TextButton("Restart Match", skin);
        gameOverMenu.add(restartMatchButton).expand().pad(10);
        
        gameOverMenu.row();
        TextButton exitButton = new TextButton("Exit", skin);
        gameOverMenu.add(exitButton).expand().pad(10);
        
        gameOverMenu.pack(); // Size the window to its contents
        gameOverMenu.setPosition(stage.getWidth() / 2 - gameOverMenu.getWidth() / 2, stage.getHeight() / 2 - gameOverMenu.getHeight() / 2);
        stage.addActor(gameOverMenu);
        gameOverMenu.setVisible(false); // Hide it by default

        // 5. Build the Game Over Waiting Menu for Non-Host players
        gameOverWaitingMenu = new Window("Game Over", skin);
        gameOverWaitingMenu.pad(20);
        gameOverWaitingMenu.padTop(50);
        Label waitingLabel = new Label("Waiting for host...", skin);
        waitingLabel.setAlignment(Align.center);
        gameOverWaitingMenu.add(waitingLabel).expand().pad(20);
        gameOverWaitingMenu.pack();
        gameOverWaitingMenu.setPosition(stage.getWidth() / 2 - gameOverWaitingMenu.getWidth() / 2, stage.getHeight() / 2 - gameOverWaitingMenu.getHeight() / 2);
        stage.addActor(gameOverWaitingMenu);
        gameOverWaitingMenu.setVisible(false); // Hide it by default

        // 6. Build the Game Over Menu for Local Games (exit only, no restart)
        gameOverLocalMenu = new Window("Game Over", skin);
        gameOverLocalMenu.pad(20);
        gameOverLocalMenu.padTop(50);
        TextButton exitLocalButton = new TextButton("Exit", skin);
        gameOverLocalMenu.add(exitLocalButton).expand().pad(10);
        gameOverLocalMenu.pack();
        gameOverLocalMenu.setPosition(stage.getWidth() / 2 - gameOverLocalMenu.getWidth() / 2, stage.getHeight() / 2 - gameOverLocalMenu.getHeight() / 2);
        stage.addActor(gameOverLocalMenu);
        gameOverLocalMenu.setVisible(false); // Hide it by default

        exitLocalButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Local game - just return to start screen
                game.setScreen(new StartScreen(game));
                dispose(); // Dispose this screen
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (gameClient != null && game.exitMatchCallback != null) {
                    game.exitMatchCallback.onExitMatch();
                } else {
                    game.setScreen(new StartScreen(game));
                    dispose(); // Dispose this screen
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
                    int numPlayers = blackjackLogic.getPlayersList().size();
                    game.setScreen(new GameScreen(game, numPlayers));
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

    public void rebuildLayout(List<Player> players) {
        // First, remove all existing player and dealer labels from the stage
        if (dealerScoreLabel != null) {
            dealerScoreLabel.remove(); // remove() detaches the actor from the stage
        }
        for (Label label : playerScoreLabels.values()) {
            label.remove();
        }
        for (Label label : playerBalanceLabels.values()) {
            label.remove();
        }

        // Second, clear the maps that track the labels
        playerScoreLabels.clear();
        playerBalanceLabels.clear();

        // Third, reset the currentPlayer reference in the UI
        this.currentPlayer = null;

        // Finally, call your original buildLayout method to recreate everything
        // with the new, filtered list of players.
        buildLayout(players);
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
        if (currentPlayer != null && playerScoreLabels.containsKey(currentPlayer)) {
            playerScoreLabels.get(currentPlayer).setColor(128 / 255f, 128 / 255f, 128 / 255f, 0.75f);
        }
        setCurrentPlayer(p);
    }

    public void setCurrentPlayer(Player p) {
        currentPlayer = p;
        if(currentPlayer != null && playerScoreLabels.containsKey(currentPlayer)) {
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
