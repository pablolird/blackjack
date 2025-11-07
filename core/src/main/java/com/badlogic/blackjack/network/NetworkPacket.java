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
}
