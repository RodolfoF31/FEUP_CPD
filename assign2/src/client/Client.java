package client;

import common.protocol.Commands;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;

import client.ui.ClientUI;

class Response {
    private List<String> serverResponses = new ArrayList<>();

    public void addServerResponse(String response) {
        synchronized (serverResponses) {
            serverResponses.add(response);
            serverResponses.notifyAll();
        }
    }

    public String getNextServerResponse() {
        synchronized (serverResponses) {
            long startTime = System.currentTimeMillis();
            long maxWaitTime = 300000; // 5 minutes in milliseconds
            while (serverResponses.isEmpty() && System.currentTimeMillis() - startTime < maxWaitTime) {
                try {
                    serverResponses.wait(1000); // wait for 1 second before re-checking
                } catch (InterruptedException e) {
                    System.out.println("Interrupted while waiting for server response: " + e.getMessage());
                    Thread.currentThread().interrupt(); // re-interrupt the thread
                    return null;
                }
            }
            return serverResponses.isEmpty() ? null : serverResponses.remove(0);
        }
    }
}

public class Client {
    private ClientUI ui;
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Response response = new Response();
    private String sessionToken;
    private boolean isGameRunning = false;
    private final ReentrantLock isGameRunningLock = new ReentrantLock();
    private volatile boolean allowGuessSubmission = true;
    private Scanner scanner;
    private TokenDB tokenDB;
    private String username;

    public Client(String host, int port) {
        this.ui = new ClientUI();
        this.host = host;
        this.port = port;
        this.sessionToken = null;
        this.scanner = new Scanner(System.in);
        this.tokenDB = new TokenDB("client/playersTokens");
    }

    public void connectToServer() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to the game server at " + host + ":" + port);

            Thread.startVirtualThread(() -> {
                try {
                    String fromServer;
                    while ((fromServer = in.readLine()) != null) {
                        response.addServerResponse(fromServer);
                    }
                } catch (IOException e) {
                    System.out.println("Connection to server lost: " + e.getMessage());
                }
            });

        } catch (IOException e) {
            System.out.println("Could not connect to the server: " + e.getMessage());
            System.exit(1);
        }
    }

    public void start() throws IOException {
        connectToServer();
        ui.showGameName();
        mainMenu();
        ui.showGoodbyeMessage();
    }

    private void mainMenu() throws IOException {
        while (true) {
            if (isUserLoggedIn()) {
                int rank = handleAskRank();
                username = handleAskUsername();

                ui.showMessage("\nUser: " + username + " (Rank: " + rank + ")");
                String option = ui.showLoggedMenu();
                switch (option) {
                    case "1":
                        handlePlay(Commands.START_GAME_SIMPLE);
                        break;
                    case "2":
                        handlePlay(Commands.START_GAME_RANK);
                        break;
                    case "3":
                        ui.clearConsole();
                        handleLogout();
                        break;
                }
            } else {
                String option = ui.showMainMenu();
                switch (option) {
                    case "1":
                        ui.clearConsole();
                        handleLogin();
                        break;
                    case "2":
                        ui.clearConsole();
                        handleTokenLogin();
                        break;
                    case "3":
                        ui.clearConsole();
                        handleRegister();
                        break;
                    case "4":
                        handleQuit();
                        return;
                    default:
                        ui.showMessage("Invalid option. Please select a valid one.");
                        break;
                }
            }
        }
    }

    private String handleAskUsername() {
        sendCommand(Commands.ASK_USERNAME, sessionToken);
        String[] responseTemp = handleServerResponse();
        if (responseTemp == null) {
            return null;
        }
        String command = responseTemp[0];
        String message = responseTemp[1];
        if (command.equals(Commands.ASK_USERNAME)) {
            return message;
        }
        return null;
    }

    private int handleAskRank() {
        sendCommand(Commands.ASK_RANK, sessionToken);
        String[] responseTemp = handleServerResponse();
        if (responseTemp == null) {
            return -1;
        }
        String command = responseTemp[0];
        String message = responseTemp[1];
        if (command.equals(Commands.ASK_RANK)) {
            return Integer.parseInt(message);
        }
        return -1;
    }

    private String[] handleServerResponse() {
        String serverResponse = response.getNextServerResponse();
        if (serverResponse == null)
            return null;
        String[] parts = serverResponse.split("\\$", 3);
        String command = parts[0];
        String message = parts.length > 1 ? parts[1] : "";
        String other = parts.length > 2 ? parts[2] : "";
        if (command.equals(Commands.ERROR)) {
            ui.clearAndShowMessage(message);
            return null;
        } else if (command.equals(Commands.INVALID_SESSION)) {
            ui.clearAndShowMessage(message);
            sessionToken = null;
            return null;
        } else {
            return new String[] { command, message, other };
        }
    }

    private void handleTokenLogin() {
        username = ui.showTokenLoginPrompt();
        // Send the token login request to the server
        String token = tokenDB.loadToken(username); // Retrieve token from the file
        if (token == null) {
            ui.showMessage("No token found for user: " + username);
            return;
        }

        sendCommand(Commands.LOGIN_WITH_TOKEN, username, token);
        String[] serverResponse = handleServerResponse();

        if (serverResponse == null) {
            ui.showMessage("Login failed. Please try again.");
            return;
        }

        String command = serverResponse[0];
        String message = serverResponse[1];

        if (command.equals(Commands.LOGIN_SUCCESS)) {
            sessionToken = message.trim();
            ui.showMessage("Login Successful. Token: " + sessionToken);

            handleReconnect(username);
        }
    }

    private void handleLogin() throws IOException {
        String[] credentials = ui.showLoginPrompt();
        username = credentials[0];
        String password = credentials[1];
        sendCommand(Commands.LOGIN, username, password);

        String[] serverResponse = handleServerResponse();

        if (serverResponse == null) {
            ui.showMessage("Login failed. Please try again.");
            return;
        }

        String command = serverResponse[0];
        String message = serverResponse[1];

        if (command.equals(Commands.LOGIN_SUCCESS)) {
            sessionToken = message.trim();
            String currentState = tokenDB.loadState(username);
            if (currentState == null) {
                currentState = "none";
            }
            tokenDB.saveToken(username, sessionToken, currentState);
            ui.showMessage("Login Successful. Token: " + sessionToken);

            handleReconnect(username);
        }
    }

    private void handleRegister() {
        boolean validRegister = false;
        do {
            String[] credentials = ui.showRegistrationPrompt();
            username = credentials[0];
            String password = credentials[1];

            boolean isValidUsername = isValidUsername(username);
            boolean isValidPassword = isValidPassword(password);

            // Verify username and password
            if (!isValidUsername) {
                ui.showErrorMessageAndClear("Invalid username. Please enter a valid username.");
                ui.showMessage("A valid username must have at least 5 characters.");
            }
            if (!isValidPassword) {
                ui.showErrorMessageAndClear("Invalid password. Please enter a valid password.");
                ui.showMessage(
                        "A valid password must have at least 8 characters, one uppercase letter, one lowercase letter, and one digit.");
            }

            if (!isValidUsername || !isValidPassword) {
                String tryAgain = ui.tryAgainPrompt();
                if (!tryAgain.equalsIgnoreCase("Y")) {
                    return;
                }
                continue;
            }

            // Send the registration command to the server
            sendCommand(Commands.REGISTER, username, password);
            String[] serverResponse = handleServerResponse();
            if (serverResponse != null) {
                String command = serverResponse[0];
                String message = serverResponse[1];
                validRegister = command.equals(Commands.REGISTER_SUCCESS);
                if (!validRegister) {
                    String tryAgain = ui.tryAgainPrompt();
                    if (!tryAgain.equalsIgnoreCase("Y")) {
                        return;
                    }
                }
                ui.clearAndShowMessage(message);
            }

        } while (!validRegister);
    }

    private boolean isValidPassword(String password) {
        // Password must have at least 8 characters
        if (password.length() < 8) {
            return false;
        }

        // Password must contain at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }

        // Password must contain at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return false;
        }

        // Password must contain at least one digit
        if (!password.matches(".*\\d.*")) {
            return false;
        }

        // Password is valid
        return true;
    }

    private boolean isValidUsername(String username) {
        // Username must have at least 5 characters
        if (username.length() < 5) {
            return false;
        }

        // Username is valid
        return true;
    }

    public boolean isUserLoggedIn() {
        return sessionToken != null;
    }

    private void handlePlay(String modeCommand) {
        if (!isUserLoggedIn()) {
            ui.clearAndShowMessage("You must be logged in to play.");
            return;
        }

        sendCommand(modeCommand, sessionToken);
        String[] responseTemp = handleServerResponse();
        if (responseTemp == null) {
            ui.showMessage("Unable to join or reconnect to the game. Please try again.");
            return;
        }

        String command = responseTemp[0];
        String message = responseTemp[1];
        if (command.equals(Commands.WAITING_QUEUE)) {
            tokenDB.saveToken(username, sessionToken, "lobby");
            waitForGameToStart();
        } else {
            ui.showMessage(message);
        }
    }

    private void handleReconnect(String username) {
        String state = tokenDB.loadState(username);
        if (state == null || state.equals("none")) {
            ui.showMessage("No reconnection state found.");
            return;
        }

        if (state.equals("game")) {
            sendCommand(Commands.RECONNECT, sessionToken);
        } else if (state.equals("lobby")) {
            sendCommand(Commands.RECONNECT_LOBBY, sessionToken);
        }

        String[] responseTemp = handleServerResponse();
        if (responseTemp == null) {
            ui.showMessage("Reconnection failed.");
            return;
        }
        String command = responseTemp[0];
        if (command.equals(Commands.RECONNECT_SUCCESS)) {
            ui.showMessage("Reconnection successful.");
            if (state.equals("game")) {
                startGameplayLoop();
            } else {
                waitForGameToStart();
            }
        } else {
            ui.showMessage("Reconnection failed: " + responseTemp[1]);
        }
    }

    private boolean isGameRunning() {
        isGameRunningLock.lock();
        try {
            return isGameRunning;
        } finally {
            isGameRunningLock.unlock();
        }
    }

    private void setGameRunning(boolean running) {
        isGameRunningLock.lock();
        try {
            isGameRunning = running;
        } finally {
            isGameRunningLock.unlock();
        }
    }

    private void waitForGameToStart() {
        ui.showLobbyWaiting();

        Thread listenForGameStartThread = Thread.startVirtualThread(this::listenForGameStart);
        Thread inputThread = Thread.startVirtualThread(() -> listenForExitLobby(scanner));

        try {
            listenForGameStartThread.join();
            inputThread.interrupt();
            inputThread.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
        }
        if (isGameRunning()) {
            startGameplayLoop();
        }
    }

    private void listenForGameStart() {
        while (!Thread.currentThread().isInterrupted()) {
            String[] serverResponse = handleServerResponse();
            if (serverResponse == null) {
                ui.showMessage("Unable to join the game. Please try again.");
                break;
            }
            String command = serverResponse[0];
            String message = serverResponse[1];
            if (command.equals(Commands.GAME_STARTING)) {
                tokenDB.saveToken(username, sessionToken, "game");
                setGameRunning(true);
                break;
            } else if (command.equals(Commands.QUIT_LOBBY)) {
                tokenDB.saveToken(username, sessionToken, "none");
                ui.clearAndShowMessage(message);
                break;
            } else if (command.equals(Commands.INVALID_SESSION)) {
                handleLogout();
                ui.clearAndShowMessage(message);
                break;
            }
        }
    }

    private void listenForExitLobby(Scanner scanner) {
        while (!Thread.currentThread().isInterrupted() && !isGameRunning()) {
            try {
                if (System.in.available() > 0) {
                    if (scanner.hasNextLine()) {
                        String input = scanner.nextLine().trim();
                        if (input.equalsIgnoreCase("Q")) {
                            sendCommand(Commands.QUIT_LOBBY, sessionToken);
                            tokenDB.saveToken(username, sessionToken, "none");
                            break;
                        }
                    }
                }
            } catch (NoSuchElementException e) {
                System.out.println("User input interrupted.");
                break;
            } catch (IOException e) {
                System.out.println("Error reading user input: " + e.getMessage());
                break;
            }
        }
    }

    private void startGameplayLoop() {
        setGameRunning(true);
        allowGuessSubmission = true;

        Thread serverListenerThread = Thread.startVirtualThread(this::listenToServer);
        Thread userInputThread = Thread.startVirtualThread(() -> handleUserInput(scanner));

        try {
            serverListenerThread.join();
            userInputThread.interrupt();
            userInputThread.join();
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
        }
        System.out.println("Press ENTER to go to the menu!");
        scanner.nextLine();
    }

    private void listenToServer() {
        while (!Thread.currentThread().isInterrupted()) {
            String[] serverResponse = handleServerResponse();
            if (serverResponse != null) {
                processServerResponse(serverResponse);
            }
            if (!isGameRunning()) {
                break;
            }
        }
    }

    private void handleUserInput(Scanner scanner) {
        ui.clearConsole();
        ui.showGameStarted();
        ui.showMessage("Enter your guess or press 'Q' to exit the game: ");
        while (!Thread.currentThread().isInterrupted() && isGameRunning()) {
            try {
                if (System.in.available() > 0) {
                    if (scanner.hasNextLine()) {
                        String input = scanner.nextLine().trim();
                        System.out.println("User input: " + input);
                        if ("Q".equalsIgnoreCase(input)) {
                            sendCommand(Commands.QUIT_GAME, sessionToken);
                            tokenDB.saveToken(username, sessionToken, "none");
                            setGameRunning(false);
                            ui.showMessage("Exiting the game, please wait...");
                            break;
                        }
                        sendCommand(Commands.GUESS, sessionToken, input);
                        ui.showMessage("Enter your guess or press 'Q' to exit the game: ");
                        if (!allowGuessSubmission) {
                            System.out.println("Guess submission is disabled.");
                            continue;
                        }
                    }
                } else {
                    Thread.sleep(100); // Sleep for a short duration to avoid busy waiting
                }
            } catch (IOException e) {
                System.out.println("Error reading user input: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processServerResponse(String[] serverResponse) {
        String command = serverResponse[0];
        String message = serverResponse[1];

        switch (command) {
            case Commands.GAME_OVER:
                tokenDB.saveToken(username, sessionToken, "none");
                setGameRunning(false);
                String winner = serverResponse.length > 1 ? message : "unknown";
                String ratingLost = serverResponse.length > 2 ? serverResponse[2] : null;
                ui.clearAndShowMessage("Game Over! The winner is " + winner + ".");
                if (!ratingLost.isEmpty())
                    ui.showMessage("You lost " + ratingLost + " points.");
                allowGuessSubmission = false;
                break;
            case Commands.GUESS_CORRECT:
                tokenDB.saveToken(username, sessionToken, "none");
                setGameRunning(false);
                String ratingWon = serverResponse.length > 2 ? serverResponse[2] : null;
                ui.clearAndShowMessage("Correct guess! The number was " + message + ".");
                if (!ratingWon.isEmpty())
                    ui.showMessage("You won " + ratingWon + " points.");
                allowGuessSubmission = false;
                break;
            case Commands.GUESS_INCORRECT:
                ui.showMessage(message);
                break;
            case Commands.QUIT_GAME:
                tokenDB.saveToken(username, sessionToken, "none");
                setGameRunning(false);
                allowGuessSubmission = false;
                ui.showMessage(message);
                break;
            case Commands.PLAYER_RECONNECT:
                ui.showMessage("User: " + message + " has reconnected to the game.");
                break;
            case Commands.PLAYER_DISCONNECTED:
                ui.showMessage("User: " + message + " has lost connection.");
                break;
            case Commands.PLAYER_QUITED:
                ui.showMessage("User: " + message + " has quited the game.");
                break;
            default:
                ui.showMessage("Received unknown command: " + command);
                break;
        }
    }

    private void handleQuit() {
        closeEverything();
    }

    private void handleLogout() {
        if (isUserLoggedIn()) {
            sendCommand(Commands.LOGOUT, sessionToken);
            sessionToken = null;
            ui.showMessage("You have been logged out.");
        } else {
            sessionToken = null;
            ui.showMessage("You are not logged in.");
        }
    }

    private void sendCommand(String command, String... args) {
        if (out != null) {
            StringBuilder sb = new StringBuilder(command);
            for (String arg : args) {
                sb.append("$").append(arg); // Appends each argument as a single block
            }
            out.println(sb.toString());
        }
    }

    private void closeEverything() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            System.out.println("Connection to the server closed.");
        } catch (IOException e) {
            System.out.println("An error occurred while closing resources: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client("127.0.0.1", 9999);
        client.start();
        client.scanner.close();
    }
}
