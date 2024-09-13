package server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import server.network.PlayerHandler;

public class LobbyManager {
    private static final int MAX_PLAYERS = 3;
    private static LobbyManager instance;
    private List<Lobby> lobbies;
    private RatedMatchMaking ratedMatchMaking;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private LobbyManager(RatedMatchMaking ratedMatchMaking) {
        lobbies = new ArrayList<>();
        this.ratedMatchMaking = ratedMatchMaking;
    }

    public static synchronized LobbyManager getInstance(RatedMatchMaking ratedMatchMaking) {
        if (instance == null) {
            instance = new LobbyManager(ratedMatchMaking);
        }
        return instance;
    }

    public synchronized Lobby getAvailableLobby(GameMode mode) {
        for (Lobby lobby : lobbies) {
            if (lobby.getGameMode() == mode && lobby.hasSpace()) {
                System.out.println("Lobby found with mode " + mode + ".");
                return lobby;
            }
        }
        Lobby newLobby = new Lobby(MAX_PLAYERS, mode);
        lobbies.add(newLobby);
        return newLobby;
    }

    public synchronized void addPlayerToMatchMaking(PlayerHandler player) {
        ratedMatchMaking.addPlayer(player);
    }

    public synchronized boolean removePlayerFromMatchMaking(PlayerHandler player) {
        return ratedMatchMaking.removePlayer(player.getUsername());
    }

    public synchronized void addLobby(Lobby lobby) {
        lobbies.add(lobby);
    }

    public Lobby waitForMatch(PlayerHandler player) {
        Lobby lobby = ratedMatchMaking.findPossibleMatch();
        if (lobby != null) {
            lobbies.add(lobby);
        }
        return lobby;
    }

    public Lobby findLobbyWithPlayer(PlayerHandler player) {
        for (Lobby lobby : lobbies) {
            System.out.println("Lobby Players: " + lobby.getPlayers());
            if (lobby.getPlayers().contains(player.getUsername())) {
                return lobby;
            }
        }
        return null;
    }

    public Lobby findGameWithPlayer(PlayerHandler player) {
        for (Lobby lobby : lobbies) {
            if (lobby.getGamePlayers().contains(player.getUsername())) {
                return lobby;
            }
        }
        return null;
    }

    public void scheduleRemovePlayer(PlayerHandler player) {
        scheduler.schedule(() -> {
            Lobby lobby = findLobbyWithPlayer(player);
            if (lobby != null) {
                lobby.removePlayer(player);
            }
        }, 30, TimeUnit.SECONDS);
    }
}