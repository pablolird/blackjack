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
        public String roomName;
        public int maxPlayers;
        public int currentPlayers;
        public ArrayList<String> playerNames; // List of all connected player names
    }

    /**
     * Sent from the Host to all Clients to tell them the game is starting.
     */
    public static class StartGame {
        public int maxPlayers;
        public ArrayList<String> playerNames; // Send the final player list to clients
    }

    // --- NEW IN-GAME PACKETS ---

    /**
     * Enum for all possible actions a client can send to the server.
     */
    public enum PlayerActionType {
        HIT,
        STAND,
        ADD_TO_BET,
        LOCK_IN_BET
    }

    /**
     * Sent from a Client to the Server to execute an action in the game.
     */
    public static class PlayerAction {
        public PlayerActionType action;
        public int amount; // Used for ADD_TO_BET, otherwise 0
    }

    /**
     * Sent from the Server to all Clients to update the game state.
     */
    public static class GameStateUpdate {
        // We'll start simple and expand this later
        public String currentGameState; // The enum value (e.g. "PLAYER_TURN")
        public String currentPlayerName;
        public int currentPlayerIndex;
        // This packet will be expanded to contain cards, balances, etc.
    }
}
