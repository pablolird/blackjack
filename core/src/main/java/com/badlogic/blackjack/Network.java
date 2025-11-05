package com.badlogic.blackjack;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;

public class Network
{
    private Server server;
    private Client client;
    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54777;

    public Network()
    {

    }

    public void startHostServer()
    {
        try
        {
            server = new Server();
            // REGISTRATIONS
            server.start();
            server.bind(TCP_PORT, UDP_PORT);
            System.out.println("Kryonet Server started on port " + TCP_PORT);
        } catch (IOException e)
        {
            System.err.println("Could not start server " + e.getMessage());
        }
    }

    public void stopServer()
    {
        if(server != null)
        {
            server.stop();
            server = null;
        }
    }

    public void startClient()
    {
        client = new Client();
        // REGISTRATION
        client.start();
    }

    public void stopClient()
    {
        if (client != null)
        {
            client.stop();
            client = null;
        }
    }

    public Client getClient()
    {
        return client;
    }

}
