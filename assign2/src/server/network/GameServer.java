package server.network;

import server.model.Database;
import server.model.LobbyManager;
import server.model.RatedMatchMaking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import client.TokenDB;

public class GameServer {

    private static final int PORT = 9999;

    private ExecutorService threadPoolPlayers = Executors.newVirtualThreadPerTaskExecutor();
    private LobbyManager lobbyManager;
    private RatedMatchMaking ratedMatchMaking;
    private Database database;
    private ServerSocket serverSocket;
    private TokenDB tokenDB;

    public GameServer() throws IOException {
        this.database = new Database("server/data/database.json");
        this.serverSocket = new ServerSocket(PORT);
        this.ratedMatchMaking = new RatedMatchMaking(3, 100);
        this.lobbyManager = LobbyManager.getInstance(ratedMatchMaking);
        this.tokenDB = new TokenDB("client/playersTokens");
        this.tokenDB.resetAllStates(); // Reset all player states to 'none'
    }

    public void startServer() {
        System.out.println("Server is listening on port " + PORT);

        // Thread that checks for possible ranked matches
        threadPoolPlayers.execute(ratedMatchMaking);

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connected to client " + clientSocket.getRemoteSocketAddress());
                PlayerHandler playerHandler = new PlayerHandler(clientSocket, database, lobbyManager);
                threadPoolPlayers.execute(playerHandler);
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdownServer();
        }
    }

    private void shutdownServer() {
        threadPoolPlayers.shutdown(); // Properly shutdown the thread pool
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Could not close the server socket: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            server.startServer();
        } catch (IOException e) {
            System.out.println("Failed to start the server: " + e.getMessage());
        }
    }
}
