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
    }
}
