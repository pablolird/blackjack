package com.badlogic.blackjack.game;

public enum GameState {
    STARTING,
    BETTING,
    DEALING_DEALER,
    DEALING_PLAYERS,
    PLAYER_TURN,
    ANIMATIONS_IN_PROGRESS,
    DEALER_TURN,
    RESOLVING_BETS,
    FINISHING_ROUND,
    GAME_OVER
}
