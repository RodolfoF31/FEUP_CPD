package server.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import common.protocol.Commands;
import server.model.guesses.Guess;
import server.model.guesses.GuessQueue;
import server.network.PlayerHandler;

public class Game implements Runnable {
    private final int maxNumber;
    private final int numberToGuess;
    private final Map<String, Integer> playerScores;
    private final List<PlayerHandler> players;
    private final GuessQueue guessQueue;
    private final Object playersLock = new Object();
    private Lobby lobby;
    private boolean isGameRunning;
    private String reconnected_player = null;
    private GameMode gameMode;

    public Game(int maxNumber, Lobby lobby) {
        this.maxNumber = maxNumber;
        this.lobby = lobby;
        this.numberToGuess = new Random().nextInt(maxNumber) + 1;
        this.playerScores = new HashMap<>();
        this.players = new ArrayList<>();
        this.guessQueue = new GuessQueue();
        this.isGameRunning = true;
        this.gameMode = lobby.getGameMode();
    }

    public List<String> getPlayers() {
        synchronized (playersLock) {
            List<String> playerNames = new ArrayList<>();
            for (PlayerHandler player : players) {
                playerNames.add(player.getUsername());
            }
            return playerNames;
        }
    }

    public int getMaxNumber() {
        return maxNumber;
    }

    public void playerLostConnection(PlayerHandler disconnected_player) {
        synchronized (playersLock) {
            for (PlayerHandler player : players) {
                if (!player.getUsername().equals(disconnected_player.getUsername())) {
                    player.sendMessage(Commands.PLAYER_DISCONNECTED, disconnected_player.getUsername());
                }
            }
        }
    }

    public void playerQuitGame(PlayerHandler player) {
        removePlayer(player);
        synchronized (playersLock) {
            for (PlayerHandler p : players) {
                p.sendMessage(Commands.PLAYER_QUITED, player.getUsername());
            }
        }
    }

    public void addPlayer(PlayerHandler playerHandler) {
        synchronized (playersLock) {
            players.add(playerHandler);
            playerScores.put(playerHandler.getUsername(), 0);
        }
    }

    public synchronized void removePlayer(PlayerHandler player) {
        String username = player.getUsername();
        synchronized (playersLock) {
            players.remove(player);
            playerScores.remove(username);
            if (players.isEmpty()) {
                isGameRunning = false;
            }
        }
    }

    public synchronized void reconnectPlayer(PlayerHandler player_to_reconnect) {
        synchronized (playersLock) {
            removePlayer(player_to_reconnect.getUsername());
            addPlayer(player_to_reconnect);
            reconnected_player = player_to_reconnect.getUsername();
        }
    }

    public synchronized void removePlayer(String username) {
        synchronized (playersLock) {
            for (PlayerHandler player : players) {
                if (player.getUsername().equals(username)) {
                    players.remove(player);
                    playerScores.remove(username);
                    if (players.isEmpty()) {
                        isGameRunning = false;
                    }
                    break;
                }
            }
        }
    }

    public synchronized String guess(String username, int guess) throws IOException {
        if (!isGameRunning) {
            return "Game is over.";
        }
        if (guess == numberToGuess) {
            playerScores.put(username, playerScores.getOrDefault(username, 0) + 1);
            isGameRunning = false;
            announceWinner(username);

            if (gameMode.equals(GameMode.RANKED)) {
                adjustRanks(username);
                return Commands.GUESS_CORRECT + "$" + numberToGuess + "$"
                        + getRankDifference(getPlayer(username), true);
            }

            return Commands.GUESS_CORRECT + "$" + numberToGuess;

        } else {
            return guess < numberToGuess ? Commands.GUESS_INCORRECT + "$" + "Too low!"
                    : Commands.GUESS_INCORRECT + "$" + "Too high!";
        }
    }

    private void adjustRanks(String winnerUsername) throws IOException {
        synchronized (playersLock) {
            for (PlayerHandler player : players) {
                boolean isWinner = player.getUsername().equals(winnerUsername);
                int averageOfOtherRanks = players.stream().filter(p -> !p.getUsername().equals(winnerUsername))
                        .mapToInt(p -> p.getRank()).sum() / (players.size() - 1);

                int new_rank = RankingSystem.calculateNewRank(player.getRank(), averageOfOtherRanks, isWinner);
                player.setRank(new_rank);
            }
        }
    }

    private PlayerHandler getPlayer(String username) {
        synchronized (playersLock) {
            for (PlayerHandler player : players) {
                if (player.getUsername().equals(username)) {
                    return player;
                }
            }
            return null;
        }
    }

    private int getRankDifference(PlayerHandler player, boolean isWinner) {
        synchronized (playersLock) {
            int averageOfOtherRanks = players.stream().filter(p -> !p.getUsername().equals(player.getUsername()))
                    .mapToInt(p -> p.getRank()).sum() / (players.size() - 1);
            int new_rank = RankingSystem.calculateNewRank(player.getRank(), averageOfOtherRanks, isWinner);
            return Math.abs(player.getRank() - new_rank);
        }
    }

    public boolean isGameRunning() {
        return isGameRunning;
    }

    @Override
    public void run() {
        try {
            synchronized (playersLock) {
                for (PlayerHandler player : players) {
                    // Notify all players that the game is starting or that a player has reconnected
                    if (reconnected_player == null)
                        player.sendMessage(Commands.GAME_STARTING);
                    else
                        player.sendMessage(Commands.PLAYER_RECONNECT, reconnected_player);
                }
            }
            while (isGameRunning) {
                Guess guess;
                synchronized (this) {
                    while (guessQueue.isEmpty() && isGameRunning) {
                        wait();
                    }
                    guess = guessQueue.getNextGuess();
                    System.out.println("Guess of the queue: " + guess.getGuess());
                }
                if (guess != null) {
                    // This is where you process the guess and send the response
                    String username = guess.getPlayerHandler().getUsername();
                    int guessNumber = guess.getGuess();
                    String response = this.guess(username, guessNumber); // Process the guess
                    guess.getPlayerHandler().sendMessage(response);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Game thread interrupted: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanupGame();
            lobby.resetLobby();
        }
    }

    public void submitGuess(PlayerHandler player, int guess) {
        synchronized (this) {
            guessQueue.addGuess(new Guess(player, guess));
            notifyAll();
        }
    }

    private void announceWinner(String winnerUsername) {
        synchronized (playersLock) {
            for (PlayerHandler player : players) {
                if (!player.getUsername().equals(winnerUsername)) {
                    if (GameMode.RANKED.equals(gameMode)) {
                        player.sendMessage(
                                Commands.GAME_OVER + "$" + winnerUsername + "$" + getRankDifference(player, false));
                    } else {
                        player.sendMessage(Commands.GAME_OVER + "$" + winnerUsername);
                    }
                }
            }
        }
    }

    private void cleanupGame() {
        synchronized (playersLock) {
            for (PlayerHandler player : players) {
                player.setGame(null);
            }
            players.clear();
        }
    }
}
