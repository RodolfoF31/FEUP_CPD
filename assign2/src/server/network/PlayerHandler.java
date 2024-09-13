package server.network;

import java.net.Socket;

import server.model.Database;
import server.model.Game;
import server.model.GameMode;
import server.model.Lobby;
import server.model.LobbyManager;

import java.io.*;
import common.protocol.Commands;

public class PlayerHandler implements Runnable {
    private Socket clientSocket;
    private Database database;
    private Game game;
    private LobbyManager lobbyManager;
    private Lobby currentSimpleLobby;
    private String username;
    private PrintWriter out;

    public PlayerHandler(Socket socket, Database database, LobbyManager lobbyManager) {
        this.clientSocket = socket;
        this.database = database;
        this.lobbyManager = lobbyManager;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void startGame() {
        sendMessage(Commands.GAME_STARTING, "Game is starting!");
    }

    public String getUsername() {
        return this.username;
    }

    public void setRank(int rank) throws IOException {
        database.updateSetRankPlayer(username, rank);
    }

    public int getRank() {
        return database.getRank(username);
    }

    @Override
    public void run() {
        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            String[] userData;
            while (true) {
                inputLine = in.readLine();
                if (inputLine == null) {
                    System.out.println("Client " + username + " lost connection.");
                    database.updateSessionActivity(database.getTokenByUsername(username));
                    if (currentSimpleLobby != null) {
                        currentSimpleLobby.markPlayerDisconnected(this); // Mark player as disconnected
                    }
                    break;
                }

                userData = inputLine.split("\\$");
                for (String data : userData) {
                    System.out.println("Received: " + data);
                }
                if (userData[0].equalsIgnoreCase(Commands.REGISTER) || userData[0].equalsIgnoreCase(Commands.LOGIN)
                        || userData[0].equalsIgnoreCase(Commands.LOGIN_WITH_TOKEN)
                        || userData[0].equalsIgnoreCase(Commands.LOGOUT)) {
                    handleCommand(userData);
                } else if (userData.length > 1 && database.isSessionActive(userData[1])) {
                    if (database.updateSessionActivity(userData[1])) {
                        handleCommand(userData);
                    } else {
                        sendMessage(Commands.ERROR, "Failed to update session activity.");
                    }
                } else {
                    if (currentSimpleLobby != null) {
                        currentSimpleLobby.markPlayerDisconnected(this); // Mark player as disconnected
                    }
                    sendMessage(Commands.INVALID_SESSION, "Invalid session. Please log in again.");
                }
            }
        } catch (IOException e) {
            System.out.println("PlayerHandler exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (currentSimpleLobby != null) {
                    currentSimpleLobby.markPlayerDisconnected(this); // Mark player as disconnected
                }
                clientSocket.close();
                System.out.println("Closed client socket.");
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                System.out.println("Could not close the client socket or output stream");
                e.printStackTrace();
            }
        }
    }

    private boolean removeFromLobby() {
        Lobby lobby = lobbyManager.findLobbyWithPlayer(this);
        if (lobby != null) {
            lobby.removePlayer(this);
            return true;
        }
        return false;
    }

    private void handleCommand(String[] userData) throws IOException {
        switch (userData[0].toUpperCase()) {
            case Commands.REGISTER:
                handleRegister(userData);
                break;
            case Commands.LOGIN:
                handleLogin(userData);
                break;
            case Commands.LOGIN_WITH_TOKEN:
                handleLoginWithToken(userData);
                break;
            case Commands.RECONNECT:
                handleReconnection(userData);
                break;
            case Commands.RECONNECT_LOBBY:
                restorePlayerStateLobby();
                break;
            case Commands.START_GAME_SIMPLE:
                handleStartGameSimple();
                break;
            case Commands.START_GAME_RANK:
                handleStartGameRank();
                break;
            case Commands.GUESS:
                handleGuess(userData);
                break;
            case Commands.ASK_RANK:
                handleAskRank(userData[1]);
                break;
            case Commands.ASK_USERNAME:
                handleAskUsername(userData[1]);
                break;
            case Commands.QUIT_GAME:
                handleQuitGame(userData[1]);
                break;
            case Commands.QUIT_LOBBY:
                handleQuitLobby(userData[1]);
                break;
            case Commands.LOGOUT:
                handleLogout(userData[1]);
                break;
            default:
                System.out.println("Unknown command: " + userData[0]);
                sendMessage("ERROR: Unknown command");
                break;
        }
    }

    private void handleAskUsername(String token) {
        String username = database.findUserByToken(token);
        sendMessage(Commands.ASK_USERNAME, username);
    }

    private void handleRegister(String[] userData) {
        if (userData.length < 3) {
            sendMessage(Commands.ERROR, "Missing credentials for registration.");
            return;
        }
        String username = userData[1];
        String password = userData[2];

        try {
            boolean success = database.register(username, password);
            if (success) {
                sendMessage(Commands.REGISTER_SUCCESS,
                        "Registration successful. You can now login with your credentials.");
            } else {
                sendMessage(Commands.ERROR, "Registration failed. Username may already exist.");
            }
        } catch (IOException e) {
            sendMessage(Commands.ERROR, "Problem during the registration process.");
            e.printStackTrace();
        }
    }

    private void handleAskRank(String token) {
        int rank = database.getRank(database.findUserByToken(token));
        sendMessage(Commands.ASK_RANK, String.valueOf(rank));
    }

    private void handleLoginWithToken(String[] userData) {
        if (userData.length < 3) {
            sendMessage(Commands.ERROR, "Missing credentials.");
            return;
        }
        this.username = userData[1];
        String token = userData[2];

        try {
            if (database.isSessionActive(token)) {
                boolean isLoggedIn = database.loginWithToken(username, token);
                if (!isLoggedIn) {
                    sendMessage(Commands.ERROR, "Login failed.");
                    return;
                }
                sendMessage(Commands.LOGIN_SUCCESS, token);
            } else {
                sendMessage(Commands.ERROR, "Authentication failed.");
            }
        } catch (IOException e) {
            sendMessage(Commands.ERROR, "Problem with the authentication process.");
            e.printStackTrace();
        }
    }

    private void handleLogin(String[] userData) {
        if (userData.length < 3) {
            sendMessage(Commands.ERROR, "Missing credentials.");
            return;
        }
        this.username = userData[1];
        String password = userData[2];

        try {
            if (database.authUserPass(username, password)) {
                boolean isLoggedIn = database.loginUser(username);
                if (!isLoggedIn) {
                    sendMessage(Commands.ERROR, "Login failed.");
                    return;
                }
                String token = database.getTokenByUsername(username);
                sendMessage(Commands.LOGIN_SUCCESS, token);
            } else {
                sendMessage(Commands.ERROR, "Authentication failed.");
            }
        } catch (IOException e) {
            sendMessage(Commands.ERROR, "Problem with the authentication process.");
            e.printStackTrace();
        }
    }

    private void handleReconnection(String[] userData) throws IOException {
        String token = userData[1];
        if (database.updateSessionActivity(token)) {
            this.username = database.findUserByToken(token);
            restorePlayerState();
        } else {
            sendMessage(Commands.RECONNECT_FAILED, "Session expired, please login again.");
        }
    }

    private void restorePlayerState() {
        if (this.game != null) {
            sendMessage(Commands.GAME_STATE, "Reconnected to the game.");
        } else {
            Lobby lobby = lobbyManager.findGameWithPlayer(this);

            if (lobby != null) {
                lobby.reconnectPlayer(this);
                sendMessage(Commands.RECONNECT_SUCCESS, "Reconnected to the lobby.");
            } else {
                sendMessage(Commands.RECONNECT_FAILED, "No active game or lobby found.");
            }
        }
    }

    private void restorePlayerStateLobby() {
        Lobby lobby = lobbyManager.findLobbyWithPlayer(this);

        if (lobby != null) {
            currentSimpleLobby = lobby;
            lobby.reconnectLobbyPlayer(this);
        } else {
            sendMessage(Commands.RECONNECT_FAILED, "No active lobby found.");
        }
    }

    private void handleStartGameSimple() {
        System.out.println("Player " + username + " is trying to start a game.");
        if (this.game != null || !database.isSessionActive(database.getTokenByUsername(username))) {
            sendMessage(Commands.ERROR, "You cannot join the game at this moment.");
            return;
        }

        if (this.username == null || this.username.isEmpty()) {
            sendMessage(Commands.ERROR, "You must be logged in to join a game.");
            return;
        }

        Lobby lobby = lobbyManager.getAvailableLobby(GameMode.SIMPLE);
        this.currentSimpleLobby = lobby;
        lobby.enterLobby(this);
        sendMessage(Commands.WAITING_QUEUE);
    }

    private void handleStartGameRank() {
        System.out.println("Player " + username + " is trying to start a game.");
        if (this.game != null || !database.isSessionActive(database.getTokenByUsername(username))) {
            sendMessage(Commands.ERROR, "You cannot join the game at this moment.");
            return;
        }

        if (this.username == null || this.username.isEmpty()) {
            sendMessage(Commands.ERROR, "You must be logged in to join a game.");
            return;
        }

        sendMessage(Commands.WAITING_QUEUE);
        lobbyManager.addPlayerToMatchMaking(this);
    }

    private void handleGuess(String[] userData) {
        if (this.game != null && userData.length > 2) {
            int guess = 0;
            try {
                guess = Integer.parseInt(userData[2]);
            } catch (NumberFormatException e) {
                sendMessage(Commands.ERROR, "Your guess must be a number.");
                return;
            }
            game.submitGuess(this, guess);
        } else {
            sendMessage(Commands.ERROR, "You are not in a game or your guess was not provided.");
        }
    }

    private void handleQuitGame(String token) {
        if (game != null) {
            game.playerQuitGame(this);
            this.game = null;
            sendMessage(Commands.QUIT_GAME, "Successfully left the game.");
        } else {
            sendMessage(Commands.ERROR, "No game found.");
        }
    }

    private void handleQuitLobby(String token) {
        lobbyManager.removePlayerFromMatchMaking(this);
        removeFromLobby();
        currentSimpleLobby = null;
        sendMessage(Commands.QUIT_LOBBY, "Successfully left the lobby.");
    }

    public void handleLogout(String token) throws IOException {
        System.out.println("Logging out player with token: " + token);
        if (game != null) {
            game.removePlayer(this);
        }
        lobbyManager.removePlayerFromMatchMaking(this);
        removeFromLobby();
        currentSimpleLobby = null;
        database.makeTokenInval(token);
    }

    public void sendMessage(String command, String... args) {
        if (out != null) {
            StringBuilder sb = new StringBuilder(command);
            for (String arg : args) {
                sb.append("$").append(arg);
            }
            String message = sb.toString();
            out.println(message);
            System.out.println("Server sent message: " + message);
        }
    }
}
