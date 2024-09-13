package server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import common.protocol.Commands;
import server.network.PlayerHandler;

public class Lobby {
    private final Queue<PlayerHandler> waitingPlayers;
    private final Map<String, Long> disconnectedPlayers; // Track disconnected players and their disconnect time
    private final ReentrantLock lock;
    private final Condition newPlayerCondition;
    private final int requiredPlayers;
    private final GameMode gameMode;
    private Game game;
    private final ExecutorService gameExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private static final long RECONNECT_TIMEOUT = 30000; // 30 seconds timeout

    public Lobby(int requiredPlayers, GameMode gameMode) {
        this.waitingPlayers = new LinkedList<>();
        this.disconnectedPlayers = new HashMap<>();
        this.lock = new ReentrantLock();
        this.newPlayerCondition = lock.newCondition();
        this.requiredPlayers = requiredPlayers;
        this.gameMode = gameMode;
    }

    public List<String> getGamePlayers() {
        if (game != null && game.isGameRunning()) {
            return game.getPlayers();
        }
        return new ArrayList<>();
    }

    public List<String> getPlayers() {
        lock.lock();
        try {
            List<String> players = new ArrayList<>();
            for (PlayerHandler player : waitingPlayers) {
                players.add(player.getUsername());
            }
            players.addAll(disconnectedPlayers.keySet()); // Include disconnected players in the list
            return players;
        } finally {
            lock.unlock();
        }
    }

    public Game getGame() {
        return game;
    }

    public void enterLobby(PlayerHandler player) {
        lock.lock();
        try {
            if (!waitingPlayers.contains(player) && !disconnectedPlayers.containsKey(player.getUsername())) {
                waitingPlayers.add(player);
                for (PlayerHandler p : waitingPlayers) {
                    System.out.println("Player " + p.getUsername() + " is in the lobby.");
                }
                checkStartGame(); // Check if the game can be started
            } else {
                player.sendMessage(Commands.WAITING_QUEUE);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean containsPlayer(PlayerHandler player) {
        lock.lock();
        try {
            return waitingPlayers.contains(player) || disconnectedPlayers.containsKey(player.getUsername());
        } finally {
            lock.unlock();
        }
    }

    public void removePlayer(PlayerHandler player) {
        lock.lock();
        try {
            waitingPlayers.remove(player);
            disconnectedPlayers.remove(player.getUsername());
            if (waitingPlayers.isEmpty() && disconnectedPlayers.isEmpty()) {
                resetLobby();
            }
        } finally {
            lock.unlock();
        }
    }

    private void checkStartGame() {
        if (waitingPlayers.size() + disconnectedPlayers.size() >= requiredPlayers) {
            if (disconnectedPlayers.isEmpty()) {
                startGame();
            } else {
                scheduleTimeoutCheck();
            }
        } else {
            newPlayerCondition.signalAll();
        }
    }

    private void startGame() {
        lock.lock();
        try {
            if (waitingPlayers.size() >= requiredPlayers) {
                game = new Game(100, this);

                List<PlayerHandler> playersToStart = new ArrayList<>();
                for (int i = 0; i < requiredPlayers; i++) {
                    PlayerHandler player = waitingPlayers.poll();
                    if (player != null) {
                        playersToStart.add(player);
                    }
                }

                gameExecutor.execute(() -> {
                    for (PlayerHandler player : playersToStart) {
                        game.addPlayer(player);
                        player.setGame(game);
                    }
                    game.run();
                });

            } else {
                System.out.println("Not enough players to start the game.");
            }
        } finally {
            lock.unlock();
        }
    }

    private void scheduleTimeoutCheck() {
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            disconnectedPlayers.values().removeIf(disconnectTime -> now - disconnectTime > RECONNECT_TIMEOUT);
            if (disconnectedPlayers.isEmpty()) {
                startGame();
            } else {
                newPlayerCondition.await(RECONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
                checkStartGame();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public void reconnectPlayer(PlayerHandler player) {
        lock.lock();
        try {
            if (game != null && game.getPlayers().contains(player.getUsername())) {
                game.reconnectPlayer(player);
                player.setGame(game);
                gameExecutor.execute(() -> {
                    game.run();
                });
            }
        } finally {
            lock.unlock();
        }
    }

    public void reconnectLobbyPlayer(PlayerHandler player) {
        lock.lock();
        try {
            disconnectedPlayers.remove(player.getUsername()); // Remove from disconnected players if reconnecting
            if (!waitingPlayers.stream().anyMatch(p -> p.getUsername().equals(player.getUsername()))) {
                waitingPlayers.add(player);
            }
            player.sendMessage(Commands.RECONNECT_SUCCESS, "Reconnected to the lobby.");
            checkStartGame(); // Check if the game can start after the player has reconnected
        } finally {
            lock.unlock();
        }
    }

    public void markPlayerDisconnected(PlayerHandler player) {
        lock.lock();
        try {
            if (waitingPlayers.remove(player)) {
                disconnectedPlayers.put(player.getUsername(), System.currentTimeMillis());
                scheduleTimeoutCheck();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean hasSpace() {
        return waitingPlayers.size() + disconnectedPlayers.size() < requiredPlayers;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void resetLobby() {
        lock.lock();
        try {
            waitingPlayers.clear();
            disconnectedPlayers.clear();
            newPlayerCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}