package com.badlogic.blackjack;

import com.esotericsoftware.kryo.Kryo;

public class Network
{
    public static final int PORT_TCP = 54555;
    public static final int PORT_UDP = 54777;

    public static void register(Kryo kryo)
    {
        kryo.register(Connect.class);
        kryo.register(LobbyUpdate.class);
        kryo.register(HostAdvertise.class); // <-- NEW: Register HostAdvertise
        kryo.register(java.lang.String.class); // Necessary for custom Kryo serialization
    }

    // Packet sent by client upon connection
    public static class Connect
    {
        public String playerName;

        public Connect(){
        }

        public Connect(String playerName)
        {
            this.playerName = playerName;
        }
    }

    // Packet sent by server to all clients
    public static class LobbyUpdate
    {
        public int currentPlayers;
        public int maxPlayers;

        public LobbyUpdate(){

        }

        public LobbyUpdate(int currentPlayers, int maxPlayers)
        {
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
        }
    }

    // NEW: Packet sent by server as response to a client's discovery request
    public static class HostAdvertise
    {
        public String lobbyName;
        public int currentPlayers;
        public int maxPlayers;

        public HostAdvertise(){
        }

        public HostAdvertise(String lobbyName, int currentPlayers, int maxPlayers)
        {
            this.lobbyName = lobbyName;
            this.currentPlayers = currentPlayers;
            this.maxPlayers = maxPlayers;
        }
    }
}
