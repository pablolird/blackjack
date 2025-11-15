package com.badlogic.blackjack.network;

import java.util.ArrayList;

/**
 * All classes that are sent over the network must be registered.
 * These are simple data transfer objects (DTOs).
 */
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
        // --- ADD THESE FIELDS ---
        public String roomName;
        public int maxPlayers;
        public int currentPlayers;
        public ArrayList<String> playerNames; // List of all connected player names
        // --- END OF ADDED FIELDS ---
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
        public boolean hostExited; // true if host exited, false if non-host exited
        public String exitedPlayerName;
    }
    
    /**
     * Sent from the Server to a Client to notify them that the lobby is full.
     */
    public static class LobbyFullResponse {
        public String message; // "Lobby is full"
    }

    /**
     * Sent from the Host to all Clients to tell them the game is starting.
     */
    public static class StartGame {
        // --- ADD THESE FIELDS ---
        public int maxPlayers;
        public ArrayList<String> playerNames; // Send the final player list to clients
        // --- END OF ADDED FIELDS ---
    }

    // --- (PlayerActionType enum and PlayerAction class remain the same) ---
    public enum PlayerActionType {
        HIT,
        STAND,
        ADD_TO_BET,
        LOCK_IN_BET
    }

    public static class PlayerAction {
        public PlayerActionType action;
        public int amount; // Used for ADD_TO_BET, otherwise 0
    }


    // --- NEW: DTOs FOR STATE SYNCHRONIZATION ---

    /**
     * Minimal card information to display on clients.
     * Must be registered in Kryo.
     */
    public static class CardInfo {
        public int id;
        public String rank;
        public String suit;
        public boolean isFaceUp;
    }

    /**
     * Minimal player information for a game state update.
     * Must be registered in Kryo.
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
     * This is now the "God packet" that defines the entire game board.
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
