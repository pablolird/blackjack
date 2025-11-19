package com.badlogic.blackjack.network;

import java.util.ArrayList;

// Registered packets
public class NetworkPacket {

    /**
     * Sent from a Client to the Server to announce its arrival and requested name.
     */
    public static class RegisterPlayer {
        public String name;
    }

    /**
     * Sent from the Server to all Clients (including the Host) to update the lobby state.
     */
    public static class LobbyUpdate {
        public String roomName;
        public int maxPlayers;
        public int currentPlayers;
        public ArrayList<String> playerNames;
    }

    /**
     * Sent from a Client to the Server to request exiting the lobby.
     */
    public static class ExitLobbyRequest {
        public String playerName;
    }

    /**
     * Sent from the Server to Clients to notify them about lobby exit.
     * If host exits, all clients receive this. If non-host exits, only that client receives this.
     */
    public static class ExitLobbyResponse {
        public boolean hostExited;
        public String exitedPlayerName;
    }

    /**
     * Sent from the Server to a Client to notify them that the lobby is full.
     */
    public static class LobbyFullResponse {
        public String message;
    }

    /**
     * Sent from the Host to all Clients to tell them the game is starting.
     */
    public static class StartGame {
        public int maxPlayers;
        public ArrayList<String> playerNames;
    }

    /**
     * Enumeration of possible player actions during gameplay.
     * Used in PlayerAction packets to specify what action the player wants to perform.
     */
    public enum PlayerActionType {
        HIT,
        STAND,
        ADD_TO_BET,
        LOCK_IN_BET
    }

    /**
     * Sent from a Client to the Server to perform a player action (hit, stand, bet).
     * The server validates that it's the correct player's turn before processing.
     */
    public static class PlayerAction {
        public PlayerActionType action;
        public int amount;
    }


    /**
     * Card information to display on clients.
     */
    public static class CardInfo {
        public int id;
        public String rank;
        public String suit;
        public boolean isFaceUp;
    }

    /**
     * Player information for a game state update.
     */
    public static class PlayerInfo {
        public String name;
        public int balance;
        public int currentBet;
        public ArrayList<CardInfo> cards;
        public boolean isActive;
    }

    /**
     * Sent from the Server to all Clients to update the game state.
     * This is the packet that defines board status.
     */
    public static class GameStateUpdate {
        public String currentGameState; // The enum value (e.g. "PLAYER_TURN")
        public String currentPlayerName;
        public int currentPlayerIndex;
        public ArrayList<PlayerInfo> players; // List of all player states
        public ArrayList<CardInfo> dealerCards; // Dealer's hand
    }

    /**
     * Sent from a Client to the Server to request exiting the match.
     */
    public static class ExitMatchRequest {
        public String playerName;
    }

    /**
     * Sent from the Server to Clients to notify them to return to start screen.
     * If host exits, all clients receive this.
     * If non-host exits, only that client receives this.
     */
    public static class ExitMatchResponse {
        public boolean hostExited; // true if host exited, false if non-host exited
        public String exitedPlayerName;
    }

    /**
     * Sent from the Host to the Server to request restarting the match.
     */
    public static class RestartMatchRequest {
        // Empty - host just sends this to restart
    }

    /**
     * Sent from the Server to all Clients to notify them to restart the match.
     * Contains the list of players who will participate in the new match.
     */
    public static class RestartMatchResponse {
        public ArrayList<String> playerNames; // List of players for the new match
    }
}
