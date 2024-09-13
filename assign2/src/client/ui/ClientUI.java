package client.ui;

import java.util.Scanner;

public class ClientUI {
    private final Scanner scanner;

    public ClientUI() {
        scanner = new Scanner(System.in);
    }

    public void clearConsole() {
        for (int i = 0; i < 100; i++) // Adjust the number of lines if you wish
            System.out.println();
    }

    public void showWelcomeMessage() {
        clearConsole();
        System.out.println("===================================");
        System.out.println("       Welcome to Guessing Game     ");
        System.out.println("===================================");
        System.out.println("Connect, play, and challenge others.\n");
    }

    public void showGoodbyeMessage() {
        System.out.println("Thank you for playing Guessing Game!");
        System.out.println("Goodbye and see you again soon!");
    }

    public void showGameName() {
        System.out.println("   _____                     _                                          ");
        System.out.println("  / ____|                   (_)                                         ");
        System.out.println(" | |  __ _   _  ___  ___ ___ _ _ __   __ _    __ _  __ _ _ __ ___   ___ ");
        System.out.println(" | | |_ | | | |/ _ \\/ __/ __| | '_ \\ / _` |  / _` |/ _` | '_ ` _ \\ / _ \\");
        System.out.println(" | |__| | |_| |  __/\\__ \\__ \\ | | | | (_| | | (_| | (_| | | | | | |  __/");
        System.out.println("  \\_____|\\__,_|\\___||___/___/_|_| |_|\\__, |  \\__, |\\__,_|_| |_| |_|\\___|");
        System.out.println("                                      __/ |   __/ |                     ");
        System.out.println("                                     |___/   |___/                      ");
        System.out.println("");
    }

    public void showGameStarted() {
        System.out.println("  ___                  ___ _            _          _ ");
        System.out.println(" / __|__ _ _ __  ___  / __| |_ __ _ _ _| |_ ___ __| |");
        System.out.println("| (_ / _` | '  \\/ -_) \\__ \\  _/ _` | '_|  _/ -_) _` |");
        System.out.println(" \\___\\__,_|_|_|_\\___| |___/\\__\\__,_|_|  \\__\\___\\__,_|");
        System.out.println("");
    }


    public String showMainMenu() {
        System.out.println("╭─────────────────────────────────────────────────────╮");
        System.out.println("│                      Main Menu                      │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│1. Login (Access Your Existing Account)              │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│2. Login With Token (Access Your Existing Account)   │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│3. Register (Create a New Account)                   │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│4. Quit (Close Game)                                 │");
        System.out.println("╰─────────────────────────────────────────────────────╯");
        System.out.print("Select one option => ");
        return scanner.nextLine();
    }

    public String showLoggedMenu() {
        System.out.println("╭─────────────────────────────────────────────────────╮");
        System.out.println("│                    Playing Menu                     │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│1. Play (Simple - Join a game with other)            │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│2. Play (Ranked lobby - Compete for the high score)  │");
        System.out.println("├─────────────────────────────────────────────────────┤");
        System.out.println("│3. Logout (Close Game)                               │");
        System.out.println("╰─────────────────────────────────────────────────────╯");
        System.out.print("Select one option => ");
        return scanner.nextLine();
    }


    public String[] showLoginPrompt() {
        System.out.print("Enter username => ");
        String username = scanner.nextLine();
        System.out.print("Enter password => ");
        String password = scanner.nextLine();
        return new String[] { username, password };
    }

    public String showTokenLoginPrompt() {
        System.out.print("Enter username => ");
        String username = scanner.nextLine();
        return username;
    }

    public String[] showRegistrationPrompt() {
        System.out.print("Enter desired username => ");
        String username = scanner.nextLine();
        System.out.print("Enter password => ");
        String password = scanner.nextLine();
        // You could add more validations or confirm password input here if needed.
        return new String[] { username, password };
    }

    public String tryAgainPrompt() {
        System.out.print("Would you like to try again? (Y/N) => ");
        String response = scanner.nextLine();
        return response;
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void clearAndShowMessage(String message) {
        clearConsole();
        System.out.println(message);
    }

    public void showLobbyWaiting() {
        System.out.println("\nWaiting for other players to join...");
        System.out.println("Press 'Q' to exit the lobby.");
    }

    public String getLobbyUserCommand() {
        System.out.print("Press 'Q' to leave lobby or wait: ");
        return scanner.nextLine();
    }

    public String getGuessFromUser() {
        System.out.print("Enter your guess or press 'Q' to exit the game: ");
        return scanner.nextLine();
    }

    public void showWaitingForPlayers() {
        System.out.println("Entering lobby. Waiting for other players...");
        // Add a simple text-based animation if desired
    }

    public void showGameStart() {
        System.out.println("Game is starting! Good luck!");
    }

    public void showGameModesMenu() {
        System.out.println("\nSelect Game Mode =>");
        System.out.println("1. Simple Mode");
        System.out.println("2. Ranked Mode (Coming Soon!)");
        System.out.print("Choose an option => ");
    }

    public void showGameplayInstructions() {
        System.out.println("\nGame Instructions =>");
        System.out.println("You will be guessing a number between 1 and X.");
        System.out.println("Enter your guess when prompted and wait for the result.");
        System.out.println("Type 'quit' to leave the game.");
    }

    public void showWinnerAnnouncement(String winner) {
        System.out.println("\nCongratulations " + winner + "! You have guessed the correct number!");
        System.out.println("Thank you for playing. The game will now end.");
    }

    public void showLoserAnnouncement(String winner) {
        System.out.println("\nGame Over! The winner was " + winner + ".");
        System.out.println("Better luck next time!");
    }

    public void showGameEnding() {
        System.out.println("\nThe game has ended. Returning to the main menu...");
    }

    public void showMatchmakingUpdate(String updateMessage) {
        System.out.println(updateMessage);
    }

    public void showErrorMessage(String errorMessage) {
        System.out.println("\nERROR => " + errorMessage);
        System.out.println("Please try again or contact support if the problem persists.");
    }

    public void showErrorMessageAndClear(String errorMessage) {
        clearConsole();
        showErrorMessage(errorMessage);
    }

    public void showSuccessMessage(String message) {
        System.out.println("\nSUCCESS => " + message);
    }

    public void showInputPrompt() {
        System.out.print("Enter your command => ");
    }

    // This method should be called periodically to simulate waiting animation
    public void showWaitingAnimation() {
        // Example of a simple waiting animation
        System.out.print(".");
        try {
            Thread.sleep(1000); // Wait for a second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.print(".");
    }

}
